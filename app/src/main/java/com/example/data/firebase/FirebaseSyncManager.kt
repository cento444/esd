package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FirebaseSyncStatus(
    val isConnected: Boolean = false,
    val isConfigured: Boolean = false,
    val projectId: String = "",
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val statusSummary: String = "",
    val errorMessage: String? = null
)

/**
 * Gestor de sincronización en tiempo real con Firebase Cloud Firestore.
 * Conecta la app Android con la base de datos central en la nube compartida entre
 * todos los socios de la C.B., la versión Web y el Panel Financiero.
 */
class FirebaseSyncManager private constructor(
    private val context: Context,
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error setting firestore settings: ${e.message}")
        }
        db
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val listeners = mutableListOf<ListenerRegistration>()
    private var isSyncingFromCloud = false

    private val _syncStatus = MutableStateFlow(
        FirebaseSyncStatus(
            isConfigured = false,
            projectId = "",
            statusSummary = "Inicializando..."
        )
    )
    val syncStatus: StateFlow<FirebaseSyncStatus> = _syncStatus.asStateFlow()

    fun resolveProjectId(): String {
        return try {
            firestore.app.options.projectId ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun isPlaceholderConfig(): Boolean {
        val pid = resolveProjectId().lowercase()
        val apiKey = try { firestore.app.options.apiKey ?: "" } catch (_: Exception) { "" }
        return pid.isBlank() || pid == "remixed-project-id" || pid == "1234567890" || apiKey == "remixed-api-key"
    }

    companion object {
        private const val TAG = "FirebaseSyncManager"

        // Nombres de colecciones estándar compartidas con la Web y Panel Financiero
        const val COL_ORCHARDS = "orchards"
        const val COL_WORK_PARTS = "work_parts"
        const val COL_CALENDAR_TASKS = "calendar_tasks"
        const val COL_SOCIAL_POSTS = "social_posts"
        const val COL_POST_COMMENTS = "post_comments"
        const val COL_SETTINGS = "shared_settings"
        const val DOC_TITULARES = "titulares_config"

        @Volatile
        private var INSTANCE: FirebaseSyncManager? = null

        fun getInstance(context: Context, database: AppDatabase): FirebaseSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSyncManager(context.applicationContext, database).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Inicializa la autenticación anónima y los observadores en tiempo real
     */
    fun startSync() {
        val pid = resolveProjectId()
        val placeholder = isPlaceholderConfig()

        if (placeholder) {
            _syncStatus.update {
                it.copy(
                    isConfigured = false,
                    isConnected = false,
                    projectId = pid.ifBlank { "remixed-project-id" },
                    statusSummary = "Modo Local (Firebase sin configurar)",
                    errorMessage = "El archivo google-services.json tiene la ID de plantilla ('${pid.ifBlank { "remixed-project-id" }}'). Cada móvil funciona en modo local. Para sincronizar automáticamente por internet, se requiere un proyecto Firebase real."
                )
            }
            Log.w(TAG, "Firebase en modo plantilla ('$pid'). No se iniciarán observadores de nube.")
            return
        }

        _syncStatus.update {
            it.copy(
                isConfigured = true,
                projectId = pid,
                isSyncing = true,
                statusSummary = "Conectando a Firebase ($pid)..."
            )
        }

        scope.launch {
            try {
                if (auth.currentUser == null) {
                    try {
                        auth.signInAnonymously().await()
                        Log.d(TAG, "Autenticación anónima iniciada con UID: ${auth.currentUser?.uid}")
                    } catch (authEx: Exception) {
                        Log.w(TAG, "Firebase Auth anónimo no habilitado o sin conexión: ${authEx.message}. Continuando directamente con Firestore.")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso de inicialización Auth: ${e.message}")
            }

            try {
                setupRealtimeListeners()
                uploadInitialDataIfCloudEmpty()
                _syncStatus.update {
                    it.copy(
                        isConnected = true,
                        isConfigured = true,
                        isSyncing = false,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        statusSummary = "Conectado a Firebase ($pid) en tiempo real",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configurando observadores Firestore: ${e.message}")
                _syncStatus.update {
                    it.copy(
                        isConnected = false,
                        isSyncing = false,
                        statusSummary = "Error al conectar con la nube",
                        errorMessage = e.localizedMessage ?: e.message
                    )
                }
            }
        }
    }

    /**
     * Fuerza una comprobación de sincronización o reintento de conexión
     */
    fun forceSync(onFinished: ((Boolean, String) -> Unit)? = null) {
        val pid = resolveProjectId()
        val placeholder = isPlaceholderConfig()

        if (placeholder) {
            val msg = "Modo Local activo: El proyecto está usando credenciales de plantilla ('${pid.ifBlank { "remixed-project-id" }}'). Para conectar múltiples móviles por internet se necesita añadir un google-services.json real de Firebase."
            _syncStatus.update {
                it.copy(
                    isConfigured = false,
                    isConnected = false,
                    isSyncing = false,
                    projectId = pid.ifBlank { "remixed-project-id" },
                    statusSummary = "Modo Local (Plantilla)",
                    errorMessage = msg
                )
            }
            scope.launch(Dispatchers.Main) {
                onFinished?.invoke(false, msg)
            }
            return
        }

        _syncStatus.update { it.copy(isSyncing = true, statusSummary = "Sincronizando con la nube...") }

        scope.launch {
            try {
                uploadInitialDataIfCloudEmpty()
                _syncStatus.update {
                    it.copy(
                        isConnected = true,
                        isConfigured = true,
                        isSyncing = false,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        statusSummary = "Sincronizado con éxito ($pid)",
                        errorMessage = null
                    )
                }
                withContext(Dispatchers.Main) {
                    onFinished?.invoke(true, "Sincronización completada con éxito ($pid)")
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: e.message ?: "Error desconocido"
                _syncStatus.update {
                    it.copy(
                        isConnected = false,
                        isSyncing = false,
                        statusSummary = "Error de sincronización",
                        errorMessage = err
                    )
                }
                withContext(Dispatchers.Main) {
                    onFinished?.invoke(false, "Error: $err")
                }
            }
        }
    }

    private fun setupRealtimeListeners() {
        clearListeners()

        // 1. Escuchar Huertos en tiempo real
        try {
            val orchardListener = firestore.collection(COL_ORCHARDS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error escuchando huertos en tiempo real: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            try {
                                isSyncingFromCloud = true
                                for (doc in snapshot.documents) {
                                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                    val entity = OrchardEntity(
                                        id = id,
                                        name = doc.getString("name") ?: "",
                                        ownerType = doc.getString("ownerType") ?: "propios",
                                        ownerName = doc.getString("ownerName") ?: "",
                                        variety = doc.getString("variety") ?: "Clemenules",
                                        rootstock = doc.getString("rootstock") ?: "",
                                        fruitType = doc.getString("fruitType") ?: "Cítricos",
                                        locationGps = doc.getString("locationGps") ?: "",
                                        municipality = doc.getString("municipality") ?: "",
                                        partida = doc.getString("partida") ?: "",
                                        polygon = doc.getString("polygon") ?: "",
                                        parcel = doc.getString("parcel") ?: "",
                                        hanegadas = doc.getDouble("hanegadas") ?: 0.0,
                                        plantingYear = (doc.getLong("plantingYear") ?: 0L).toInt(),
                                        pozo = doc.getString("pozo") ?: "",
                                        sector = doc.getString("sector") ?: "",
                                        hidrante = doc.getString("hidrante") ?: "",
                                        regadorName = doc.getString("regadorName") ?: "",
                                        regadorPhone = doc.getString("regadorPhone") ?: "",
                                        photoUri = doc.getString("photoUri"),
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt()
                                    )
                                    database.orchardDao().insertOrchard(entity)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error procesando sincronización de huertos: ${e.message}")
                            } finally {
                                isSyncingFromCloud = false
                            }
                        }
                    }
                }
            listeners.add(orchardListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listener de huertos: ${e.message}")
        }

        // 2. Escuchar Partes de Trabajo en tiempo real
        try {
            val workPartsListener = firestore.collection(COL_WORK_PARTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error escuchando partes de trabajo: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            try {
                                isSyncingFromCloud = true
                                for (doc in snapshot.documents) {
                                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                    val entity = WorkPartEntity(
                                        id = id,
                                        orchardId = doc.getLong("orchardId") ?: 0L,
                                        orchardName = doc.getString("orchardName") ?: "",
                                        type = doc.getString("type") ?: "tareas",
                                        taskName = doc.getString("taskName") ?: "",
                                        hours = doc.getDouble("hours") ?: 0.0,
                                        pricePerHour = doc.getDouble("pricePerHour") ?: 0.0,
                                        totalCost = doc.getDouble("totalCost") ?: 0.0,
                                        observations = doc.getString("observations") ?: "",
                                        materialsJson = doc.getString("materialsJson") ?: "",
                                        photoUri = doc.getString("photoUri"),
                                        dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis(),
                                        isMonthlyRepeat = doc.getBoolean("isMonthlyRepeat") ?: false,
                                        kilos = doc.getDouble("kilos") ?: 0.0,
                                        pricePerKg = doc.getDouble("pricePerKg") ?: 0.0,
                                        kilosDestrio = doc.getDouble("kilosDestrio") ?: 0.0,
                                        precioDestrio = doc.getDouble("precioDestrio") ?: 0.0,
                                        indemnizacionSeguro = doc.getDouble("indemnizacionSeguro") ?: 0.0,
                                        ownerCategory = doc.getString("ownerCategory") ?: "Mío"
                                    )
                                    database.workPartDao().insertWorkPart(entity)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error procesando partes de trabajo: ${e.message}")
                            } finally {
                                isSyncingFromCloud = false
                            }
                        }
                    }
                }
            listeners.add(workPartsListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listener de partes de trabajo: ${e.message}")
        }

        // 3. Escuchar Tareas del Calendario en tiempo real
        try {
            val calendarListener = firestore.collection(COL_CALENDAR_TASKS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error escuchando tareas de calendario: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            try {
                                isSyncingFromCloud = true
                                for (doc in snapshot.documents) {
                                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                    val entity = CalendarTaskEntity(
                                        id = id,
                                        orchardId = doc.getLong("orchardId"),
                                        orchardName = doc.getString("orchardName") ?: "Huerto",
                                        fruitType = doc.getString("fruitType") ?: "orange",
                                        dateKey = doc.getString("dateKey") ?: "",
                                        timeText = doc.getString("timeText") ?: "08:00",
                                        title = doc.getString("title") ?: "",
                                        description = doc.getString("description") ?: "",
                                        isDone = doc.getBoolean("isDone") ?: false,
                                        completedAt = doc.getLong("completedAt"),
                                        reminderEnabled = doc.getBoolean("reminderEnabled") ?: false,
                                        reminderDate = doc.getString("reminderDate") ?: "",
                                        reminderTime = doc.getString("reminderTime") ?: ""
                                    )
                                    database.calendarTaskDao().insertTask(entity)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error procesando tareas: ${e.message}")
                            } finally {
                                isSyncingFromCloud = false
                            }
                        }
                    }
                }
            listeners.add(calendarListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listener de tareas: ${e.message}")
        }

        // 4. Escuchar Publicaciones del Muro de Socios
        try {
            val socialListener = firestore.collection(COL_SOCIAL_POSTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error escuchando muro de socios: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            try {
                                isSyncingFromCloud = true
                                for (doc in snapshot.documents) {
                                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                    val entity = SocialPostEntity(
                                        id = id,
                                        orchardId = doc.getLong("orchardId"),
                                        authorName = doc.getString("authorName") ?: "Socio",
                                        authorAvatar = doc.getString("authorAvatar"),
                                        orchardName = doc.getString("orchardName") ?: "Finca",
                                        timestampText = doc.getString("timestampText") ?: "Hace un momento",
                                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                        content = doc.getString("content") ?: "",
                                        photoUri = doc.getString("photoUri"),
                                        isAlert = doc.getBoolean("isAlert") ?: false,
                                        alertTitle = doc.getString("alertTitle"),
                                        isAlertResolved = doc.getBoolean("isAlertResolved") ?: false,
                                        likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                                        commentsCount = (doc.getLong("commentsCount") ?: 0L).toInt(),
                                        isLiked = doc.getBoolean("isLiked") ?: false
                                    )
                                    database.socialPostDao().insertPost(entity)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error procesando posts de socios: ${e.message}")
                            } finally {
                                isSyncingFromCloud = false
                            }
                        }
                    }
                }
            listeners.add(socialListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listener de posts: ${e.message}")
        }

        // 5. Escuchar Configuración Compartida de Titulares (Propiedad, V&R, Otros)
        try {
            val settingsListener = firestore.collection(COL_SETTINGS).document(DOC_TITULARES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    scope.launch {
                        try {
                            val titularPropios = snapshot.getString("titularPropios") ?: ""
                            val titularVr = snapshot.getString("titularVr") ?: ""
                            val titularOtros = snapshot.getString("titularOtros") ?: ""

                            val currentProfile = database.userProfileDao().getProfileDirect()
                            if (currentProfile != null) {
                                val updated = currentProfile.copy(
                                    titularPropios = titularPropios.ifBlank { currentProfile.titularPropios },
                                    titularVr = titularVr.ifBlank { currentProfile.titularVr },
                                    titularOtros = titularOtros.ifBlank { currentProfile.titularOtros }
                                )
                                database.userProfileDao().updateProfile(updated)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sincronizando configuración de titulares: ${e.message}")
                        }
                    }
                }
            listeners.add(settingsListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listener de configuración compartida: ${e.message}")
        }
    }

    /**
     * Si es la primera vez que se conecta a Firestore y la nube está vacía,
     * sube los datos locales existentes para que todos los socios los compartan.
     */
    private suspend fun uploadInitialDataIfCloudEmpty() {
        try {
            val snapshot = firestore.collection(COL_ORCHARDS).limit(1).get().await()
            if (snapshot.isEmpty) {
                Log.d(TAG, "Firestore está vacío. Subiendo datos locales iniciales a la nube...")
                // Subir huertos
                val orchards = database.orchardDao().getAllOrchards()
                orchards.firstOrNull()?.forEach { orchard ->
                    pushOrchard(orchard)
                }

                // Subir partes de trabajo
                val workParts = database.workPartDao().getAllWorkParts()
                workParts.firstOrNull()?.forEach { part ->
                    pushWorkPart(part)
                }

                // Subir tareas
                val tasks = database.calendarTaskDao().getAllTasks()
                tasks.firstOrNull()?.forEach { task ->
                    pushCalendarTask(task)
                }

                // Subir titulares
                val profile = database.userProfileDao().getProfileDirect()
                if (profile != null) {
                    pushSharedTitulares(profile.titularPropios, profile.titularVr, profile.titularOtros)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en carga inicial a Firestore: ${e.message}")
        }
    }

    // --- MÉTODOS DE SUBIDA (PUSH) ---

    fun pushOrchard(orchard: OrchardEntity) {
        if (isSyncingFromCloud || isPlaceholderConfig()) return
        scope.launch {
            try {
                val data = hashMapOf(
                    "id" to orchard.id,
                    "name" to orchard.name,
                    "ownerType" to orchard.ownerType,
                    "ownerName" to orchard.ownerName,
                    "variety" to orchard.variety,
                    "rootstock" to orchard.rootstock,
                    "fruitType" to orchard.fruitType,
                    "locationGps" to orchard.locationGps,
                    "municipality" to orchard.municipality,
                    "partida" to orchard.partida,
                    "polygon" to orchard.polygon,
                    "parcel" to orchard.parcel,
                    "hanegadas" to orchard.hanegadas,
                    "plantingYear" to orchard.plantingYear,
                    "pozo" to orchard.pozo,
                    "sector" to orchard.sector,
                    "hidrante" to orchard.hidrante,
                    "regadorName" to orchard.regadorName,
                    "regadorPhone" to orchard.regadorPhone,
                    "photoUri" to orchard.photoUri,
                    "createdAt" to orchard.createdAt,
                    "sortOrder" to orchard.sortOrder,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(COL_ORCHARDS)
                    .document(orchard.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error subiendo huerto a Firestore: ${e.message}")
            }
        }
    }

    fun deleteOrchard(orchardId: Long) {
        if (isPlaceholderConfig()) return
        scope.launch {
            try {
                firestore.collection(COL_ORCHARDS).document(orchardId.toString()).delete().await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error borrando huerto en Firestore: ${e.message}")
            }
        }
    }

    fun pushWorkPart(part: WorkPartEntity) {
        if (isSyncingFromCloud || isPlaceholderConfig()) return
        scope.launch {
            try {
                val data = hashMapOf(
                    "id" to part.id,
                    "orchardId" to part.orchardId,
                    "orchardName" to part.orchardName,
                    "type" to part.type,
                    "taskName" to part.taskName,
                    "hours" to part.hours,
                    "pricePerHour" to part.pricePerHour,
                    "totalCost" to part.totalCost,
                    "observations" to part.observations,
                    "materialsJson" to part.materialsJson,
                    "photoUri" to part.photoUri,
                    "dateTimestamp" to part.dateTimestamp,
                    "isMonthlyRepeat" to part.isMonthlyRepeat,
                    "kilos" to part.kilos,
                    "pricePerKg" to part.pricePerKg,
                    "kilosDestrio" to part.kilosDestrio,
                    "precioDestrio" to part.precioDestrio,
                    "indemnizacionSeguro" to part.indemnizacionSeguro,
                    "ownerCategory" to part.ownerCategory,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(COL_WORK_PARTS)
                    .document(part.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error subiendo parte de trabajo a Firestore: ${e.message}")
            }
        }
    }

    fun deleteWorkPart(partId: Long) {
        if (isPlaceholderConfig()) return
        scope.launch {
            try {
                firestore.collection(COL_WORK_PARTS).document(partId.toString()).delete().await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error borrando parte de trabajo en Firestore: ${e.message}")
            }
        }
    }

    fun pushCalendarTask(task: CalendarTaskEntity) {
        if (isSyncingFromCloud || isPlaceholderConfig()) return
        scope.launch {
            try {
                val data = hashMapOf(
                    "id" to task.id,
                    "orchardId" to task.orchardId,
                    "orchardName" to task.orchardName,
                    "fruitType" to task.fruitType,
                    "dateKey" to task.dateKey,
                    "timeText" to task.timeText,
                    "title" to task.title,
                    "description" to task.description,
                    "isDone" to task.isDone,
                    "completedAt" to task.completedAt,
                    "reminderEnabled" to task.reminderEnabled,
                    "reminderDate" to task.reminderDate,
                    "reminderTime" to task.reminderTime,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(COL_CALENDAR_TASKS)
                    .document(task.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error subiendo tarea a Firestore: ${e.message}")
            }
        }
    }

    fun deleteCalendarTask(taskId: Long) {
        if (isPlaceholderConfig()) return
        scope.launch {
            try {
                firestore.collection(COL_CALENDAR_TASKS).document(taskId.toString()).delete().await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error borrando tarea en Firestore: ${e.message}")
            }
        }
    }

    fun pushSocialPost(post: SocialPostEntity) {
        if (isSyncingFromCloud || isPlaceholderConfig()) return
        scope.launch {
            try {
                val data = hashMapOf(
                    "id" to post.id,
                    "orchardId" to post.orchardId,
                    "authorName" to post.authorName,
                    "authorAvatar" to post.authorAvatar,
                    "orchardName" to post.orchardName,
                    "timestampText" to post.timestampText,
                    "timestamp" to post.timestamp,
                    "content" to post.content,
                    "photoUri" to post.photoUri,
                    "isAlert" to post.isAlert,
                    "alertTitle" to post.alertTitle,
                    "isAlertResolved" to post.isAlertResolved,
                    "likesCount" to post.likesCount,
                    "commentsCount" to post.commentsCount,
                    "isLiked" to post.isLiked,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(COL_SOCIAL_POSTS)
                    .document(post.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error subiendo post a Firestore: ${e.message}")
            }
        }
    }

    fun deleteSocialPost(postId: Long) {
        if (isPlaceholderConfig()) return
        scope.launch {
            try {
                firestore.collection(COL_SOCIAL_POSTS).document(postId.toString()).delete().await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error borrando post en Firestore: ${e.message}")
            }
        }
    }

    fun pushSharedTitulares(titularPropios: String, titularVr: String, titularOtros: String) {
        if (isPlaceholderConfig()) return
        scope.launch {
            try {
                val data = hashMapOf(
                    "titularPropios" to titularPropios,
                    "titularVr" to titularVr,
                    "titularOtros" to titularOtros,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(COL_SETTINGS)
                    .document(DOC_TITULARES)
                    .set(data, SetOptions.merge())
                    .await()
                _syncStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
            } catch (e: Exception) {
                Log.e(TAG, "Error subiendo configuración compartida a Firestore: ${e.message}")
            }
        }
    }

    private fun clearListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
