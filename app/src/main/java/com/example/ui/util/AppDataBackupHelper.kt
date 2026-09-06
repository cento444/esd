package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para exportar e importar todos los datos de la aplicación (huertos, partes de trabajo,
 * calendario, publicaciones del muro y titulares) en un archivo JSON estructurado.
 * Permite transferir todos los datos entre dos móviles inmediatamente vía WhatsApp, Telegram,
 * Drive, Email o Bluetooth sin necesidad de tener configurado Firebase en la nube.
 */
object AppDataBackupHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val filenameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    data class ExportResult(
        val file: File,
        val orchardCount: Int,
        val workPartCount: Int,
        val taskCount: Int,
        val postCount: Int
    )

    data class ImportResult(
        val success: Boolean,
        val orchardCount: Int = 0,
        val workPartCount: Int = 0,
        val taskCount: Int = 0,
        val postCount: Int = 0,
        val errorMessage: String? = null
    )

    /**
     * Exporta toda la base de datos local a un archivo JSON y abre el diálogo nativo
     * para compartirlo inmediatamente por WhatsApp, Email, Drive o guardarlo.
     */
    suspend fun exportAndShareData(
        context: Context,
        database: AppDatabase
    ): ExportResult? = withContext(Dispatchers.IO) {
        try {
            val orchards = database.orchardDao().getAllOrchards().firstOrNull() ?: emptyList()
            val workParts = database.workPartDao().getAllWorkParts().firstOrNull() ?: emptyList()
            val tasks = database.calendarTaskDao().getAllTasks().firstOrNull() ?: emptyList()
            val posts = database.socialPostDao().getAllPosts().firstOrNull() ?: emptyList()
            val profile = database.userProfileDao().getProfileDirect()

            val rootJson = JSONObject()
            rootJson.put("app", "AgroWork")
            rootJson.put("version", 1)
            rootJson.put("exportedAt", System.currentTimeMillis())
            rootJson.put("exportedAtText", dateFormat.format(Date()))

            // Perfil y titulares
            if (profile != null) {
                val profileJson = JSONObject().apply {
                    put("name", profile.name)
                    put("role", profile.role)
                    put("titularPropios", profile.titularPropios)
                    put("titularVr", profile.titularVr)
                    put("titularOtros", profile.titularOtros)
                }
                rootJson.put("profile", profileJson)
            }

            // Huertos
            val orchardsArray = JSONArray()
            for (o in orchards) {
                val oJson = JSONObject().apply {
                    put("id", o.id)
                    put("name", o.name)
                    put("ownerType", o.ownerType)
                    put("ownerName", o.ownerName)
                    put("variety", o.variety)
                    put("rootstock", o.rootstock)
                    put("fruitType", o.fruitType)
                    put("locationGps", o.locationGps)
                    put("municipality", o.municipality)
                    put("partida", o.partida)
                    put("polygon", o.polygon)
                    put("parcel", o.parcel)
                    put("hanegadas", o.hanegadas)
                    put("plantingYear", o.plantingYear)
                    put("pozo", o.pozo)
                    put("sector", o.sector)
                    put("hidrante", o.hidrante)
                    put("regadorName", o.regadorName)
                    put("regadorPhone", o.regadorPhone)
                    put("createdAt", o.createdAt)
                    put("sortOrder", o.sortOrder)
                }
                orchardsArray.put(oJson)
            }
            rootJson.put("orchards", orchardsArray)

            // Partes de trabajo
            val workPartsArray = JSONArray()
            for (p in workParts) {
                val pJson = JSONObject().apply {
                    put("id", p.id)
                    put("orchardId", p.orchardId)
                    put("orchardName", p.orchardName)
                    put("type", p.type)
                    put("taskName", p.taskName)
                    put("hours", p.hours)
                    put("pricePerHour", p.pricePerHour)
                    put("totalCost", p.totalCost)
                    put("observations", p.observations)
                    put("materialsJson", p.materialsJson)
                    put("dateTimestamp", p.dateTimestamp)
                    put("isMonthlyRepeat", p.isMonthlyRepeat)
                    put("kilos", p.kilos)
                    put("pricePerKg", p.pricePerKg)
                    put("kilosDestrio", p.kilosDestrio)
                    put("precioDestrio", p.precioDestrio)
                    put("indemnizacionSeguro", p.indemnizacionSeguro)
                    put("ownerCategory", p.ownerCategory)
                }
                workPartsArray.put(pJson)
            }
            rootJson.put("workParts", workPartsArray)

            // Tareas de calendario
            val tasksArray = JSONArray()
            for (t in tasks) {
                val tJson = JSONObject().apply {
                    put("id", t.id)
                    put("orchardId", t.orchardId ?: 0L)
                    put("orchardName", t.orchardName)
                    put("fruitType", t.fruitType)
                    put("dateKey", t.dateKey)
                    put("timeText", t.timeText)
                    put("title", t.title)
                    put("description", t.description)
                    put("isDone", t.isDone)
                    put("completedAt", t.completedAt ?: 0L)
                    put("reminderEnabled", t.reminderEnabled)
                    put("reminderDate", t.reminderDate)
                    put("reminderTime", t.reminderTime)
                }
                tasksArray.put(tJson)
            }
            rootJson.put("calendarTasks", tasksArray)

            // Publicaciones sociales
            val postsArray = JSONArray()
            for (post in posts) {
                val postJson = JSONObject().apply {
                    put("id", post.id)
                    put("orchardId", post.orchardId ?: 0L)
                    put("authorName", post.authorName)
                    put("orchardName", post.orchardName)
                    put("timestampText", post.timestampText)
                    put("timestamp", post.timestamp)
                    put("content", post.content)
                    put("isAlert", post.isAlert)
                    put("alertTitle", post.alertTitle ?: "")
                    put("isAlertResolved", post.isAlertResolved)
                    put("likesCount", post.likesCount)
                    put("commentsCount", post.commentsCount)
                }
                postsArray.put(postJson)
            }
            rootJson.put("socialPosts", postsArray)

            // Guardar en archivo temporal para compartir
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "copia_agro_work_${filenameDateFormat.format(Date())}.json"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                out.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            }

            // Lanzar Intent de compartir
            withContext(Dispatchers.Main) {
                shareJsonFile(context, file, fileName, orchards.size, workParts.size)
            }

            ExportResult(
                file = file,
                orchardCount = orchards.size,
                workPartCount = workParts.size,
                taskCount = tasks.size,
                postCount = posts.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al exportar datos: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }

    private fun shareJsonFile(
        context: Context,
        file: File,
        fileName: String,
        orchardCount: Int,
        workPartCount: Int
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Copia de Datos Agro Work ($fileName)")
            putExtra(
                Intent.EXTRA_TEXT,
                "Te envío la copia de datos de Agro Work ($orchardCount parcelas y $workPartCount partes). Ábrelo o impórtalo desde la app en el otro móvil."
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Enviar copia de datos al otro móvil").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
    }

    /**
     * Importa un archivo JSON seleccionado por el usuario en el segundo móvil y restaura
     * todos los huertos, partes de trabajo, tareas y titulares.
     */
    suspend fun importDataFromUri(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        replaceExisting: Boolean = true
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext ImportResult(success = false, errorMessage = "No se pudo leer el archivo seleccionado.")

            val rootJson = JSONObject(jsonString)
            if (!rootJson.has("orchards") && !rootJson.has("workParts")) {
                return@withContext ImportResult(
                    success = false,
                    errorMessage = "El archivo seleccionado no es una copia válida de Agro Work."
                )
            }

            // Si se elige reemplazar, limpiar datos antiguos
            if (replaceExisting) {
                database.orchardDao().deleteAllOrchards()
                database.workPartDao().deleteAllWorkParts()
                database.calendarTaskDao().deleteAllCalendarTasks()
            }

            // 1. Restaurar perfil y titulares
            if (rootJson.has("profile")) {
                val profileJson = rootJson.getJSONObject("profile")
                val currentProfile = database.userProfileDao().getProfileDirect() ?: UserProfileEntity()
                val updatedProfile = currentProfile.copy(
                    name = profileJson.optString("name", currentProfile.name),
                    role = profileJson.optString("role", currentProfile.role),
                    titularPropios = profileJson.optString("titularPropios", currentProfile.titularPropios),
                    titularVr = profileJson.optString("titularVr", currentProfile.titularVr),
                    titularOtros = profileJson.optString("titularOtros", currentProfile.titularOtros)
                )
                database.userProfileDao().updateProfile(updatedProfile)
            }

            // 2. Restaurar Huertos
            var importedOrchards = 0
            if (rootJson.has("orchards")) {
                val orchardsArray = rootJson.getJSONArray("orchards")
                for (i in 0 until orchardsArray.length()) {
                    val oJson = orchardsArray.getJSONObject(i)
                    val orchard = OrchardEntity(
                        id = oJson.optLong("id", 0L),
                        name = oJson.optString("name", "Huerto"),
                        ownerType = oJson.optString("ownerType", "propios"),
                        ownerName = oJson.optString("ownerName", ""),
                        variety = oJson.optString("variety", "Clemenules"),
                        rootstock = oJson.optString("rootstock", ""),
                        fruitType = oJson.optString("fruitType", "Cítricos"),
                        locationGps = oJson.optString("locationGps", ""),
                        municipality = oJson.optString("municipality", ""),
                        partida = oJson.optString("partida", ""),
                        polygon = oJson.optString("polygon", ""),
                        parcel = oJson.optString("parcel", ""),
                        hanegadas = oJson.optDouble("hanegadas", 0.0),
                        plantingYear = oJson.optInt("plantingYear", 0),
                        pozo = oJson.optString("pozo", ""),
                        sector = oJson.optString("sector", ""),
                        hidrante = oJson.optString("hidrante", ""),
                        regadorName = oJson.optString("regadorName", ""),
                        regadorPhone = oJson.optString("regadorPhone", ""),
                        createdAt = oJson.optLong("createdAt", System.currentTimeMillis()),
                        sortOrder = oJson.optInt("sortOrder", i)
                    )
                    database.orchardDao().insertOrchard(orchard)
                    importedOrchards++
                }
            }

            // 3. Restaurar Partes de trabajo
            var importedParts = 0
            if (rootJson.has("workParts")) {
                val workPartsArray = rootJson.getJSONArray("workParts")
                for (i in 0 until workPartsArray.length()) {
                    val pJson = workPartsArray.getJSONObject(i)
                    val part = WorkPartEntity(
                        id = pJson.optLong("id", 0L),
                        orchardId = pJson.optLong("orchardId", 0L),
                        orchardName = pJson.optString("orchardName", ""),
                        type = pJson.optString("type", "tareas"),
                        taskName = pJson.optString("taskName", ""),
                        hours = pJson.optDouble("hours", 0.0),
                        pricePerHour = pJson.optDouble("pricePerHour", 0.0),
                        totalCost = pJson.optDouble("totalCost", 0.0),
                        observations = pJson.optString("observations", ""),
                        materialsJson = pJson.optString("materialsJson", ""),
                        dateTimestamp = pJson.optLong("dateTimestamp", System.currentTimeMillis()),
                        isMonthlyRepeat = pJson.optBoolean("isMonthlyRepeat", false),
                        kilos = pJson.optDouble("kilos", 0.0),
                        pricePerKg = pJson.optDouble("pricePerKg", 0.0),
                        kilosDestrio = pJson.optDouble("kilosDestrio", 0.0),
                        precioDestrio = pJson.optDouble("precioDestrio", 0.0),
                        indemnizacionSeguro = pJson.optDouble("indemnizacionSeguro", 0.0),
                        ownerCategory = pJson.optString("ownerCategory", "Mío")
                    )
                    database.workPartDao().insertWorkPart(part)
                    importedParts++
                }
            }

            // 4. Restaurar Tareas de calendario
            var importedTasks = 0
            if (rootJson.has("calendarTasks")) {
                val tasksArray = rootJson.getJSONArray("calendarTasks")
                for (i in 0 until tasksArray.length()) {
                    val tJson = tasksArray.getJSONObject(i)
                    val task = CalendarTaskEntity(
                        id = tJson.optLong("id", 0L),
                        orchardId = if (tJson.has("orchardId") && tJson.getLong("orchardId") > 0) tJson.getLong("orchardId") else null,
                        orchardName = tJson.optString("orchardName", "Huerto"),
                        fruitType = tJson.optString("fruitType", "orange"),
                        dateKey = tJson.optString("dateKey", ""),
                        timeText = tJson.optString("timeText", "08:00"),
                        title = tJson.optString("title", ""),
                        description = tJson.optString("description", ""),
                        isDone = tJson.optBoolean("isDone", false),
                        completedAt = if (tJson.optLong("completedAt", 0L) > 0L) tJson.getLong("completedAt") else null,
                        reminderEnabled = tJson.optBoolean("reminderEnabled", false),
                        reminderDate = tJson.optString("reminderDate", ""),
                        reminderTime = tJson.optString("reminderTime", "")
                    )
                    database.calendarTaskDao().insertTask(task)
                    importedTasks++
                }
            }

            // 5. Restaurar Publicaciones sociales
            var importedPosts = 0
            if (rootJson.has("socialPosts")) {
                val postsArray = rootJson.getJSONArray("socialPosts")
                for (i in 0 until postsArray.length()) {
                    val postJson = postsArray.getJSONObject(i)
                    val post = SocialPostEntity(
                        id = postJson.optLong("id", 0L),
                        orchardId = if (postJson.has("orchardId") && postJson.getLong("orchardId") > 0) postJson.getLong("orchardId") else null,
                        authorName = postJson.optString("authorName", "Socio"),
                        orchardName = postJson.optString("orchardName", ""),
                        timestampText = postJson.optString("timestampText", "Hace un momento"),
                        timestamp = postJson.optLong("timestamp", System.currentTimeMillis()),
                        content = postJson.optString("content", ""),
                        isAlert = postJson.optBoolean("isAlert", false),
                        alertTitle = postJson.optString("alertTitle", null),
                        isAlertResolved = postJson.optBoolean("isAlertResolved", false),
                        likesCount = postJson.optInt("likesCount", 0),
                        commentsCount = postJson.optInt("commentsCount", 0)
                    )
                    database.socialPostDao().insertPost(post)
                    importedPosts++
                }
            }

            ImportResult(
                success = true,
                orchardCount = importedOrchards,
                workPartCount = importedParts,
                taskCount = importedTasks,
                postCount = importedPosts
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                errorMessage = "Error al procesar el archivo: ${e.localizedMessage ?: e.message}"
            )
        }
    }
}
