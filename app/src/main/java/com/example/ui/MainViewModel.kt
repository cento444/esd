package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LonjaPriceDatabase
import com.example.data.VarietyPrice
import android.net.Uri
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.firebase.FirebaseSyncStatus
import com.example.ui.util.AppDataBackupHelper
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.network.OrchardWeather
import com.example.network.WeatherService
import com.example.ui.util.AppNotificationHelper
import com.example.ui.util.InAppNotificationData
import com.example.ui.util.TaskAlarmScheduler
import com.example.ui.util.formatDateToDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object Dashboard : Screen()
    data class OrchardDetail(val orchardId: Long) : Screen()
    data class AddOrchard(val editOrchardId: Long? = null) : Screen()
    data class WorkParts(
        val initialTab: String = "tareas",
        val selectedOrchardId: Long? = null,
        val preselectedTaskType: String? = null,
        val editWorkPartId: Long? = null,
        val preselectedProductName: String? = null,
        val preselectedProductDose: String? = null,
        val preselectedProductSafetyDays: Int? = null,
        val preselectedActiveSubstance: String? = null
    ) : Screen()
    object Finances : Screen()
    object Social : Screen()
    data class NewPost(val preselectedOrchardName: String? = null) : Screen()
    object CalendarView : Screen()
    object Settings : Screen()
    object NotificationSettings : Screen()
    object DoseCalculator : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase = AppDatabase.getDatabase(application)
    private val syncManager: FirebaseSyncManager = FirebaseSyncManager.getInstance(application, db)
    private val repository: AppRepository = AppRepository(db, syncManager)

    val firebaseSyncStatus: StateFlow<FirebaseSyncStatus> = syncManager.syncStatus

    init {
        syncManager.startSync()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.checkAndPopulateIfEmpty(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                repository.cleanOrphanRecords()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                repository.updateVariosOwnerCategories()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                syncExistingTasksToSocialFeed()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                TaskAlarmScheduler.rescheduleAllActiveAlarms(application)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                startTaskAlarmActiveMonitor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                checkAndPublishLonjaMarketAlert(forceManualCheck = false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Navigation Stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenHistory = mutableListOf<Screen>()

    // Live In-App pop-up notification state
    private val _inAppNotification = MutableStateFlow<InAppNotificationData?>(null)
    val inAppNotification: StateFlow<InAppNotificationData?> = _inAppNotification.asStateFlow()

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    /**
     * Fuerza una sincronización manual o comprobación de conexión con Firebase
     */
    fun forceCloudSync(onResult: ((Boolean, String) -> Unit)? = null) {
        syncManager.forceSync(onResult)
    }

    /**
     * Exporta toda la base de datos a un archivo JSON y lo comparte (por WhatsApp, Drive, etc.)
     * para transferirlo inmediatamente a otro móvil.
     */
    fun exportFullDatabaseJson(
        context: Context,
        onResult: (AppDataBackupHelper.ExportResult?) -> Unit
    ) {
        viewModelScope.launch {
            val result = AppDataBackupHelper.exportAndShareData(context, db)
            onResult(result)
        }
    }

    /**
     * Importa y restaura una copia de datos JSON seleccionada en este móvil,
     * sincronizando todos los huertos, partes de trabajo y tareas.
     */
    fun importFullDatabaseJson(
        context: Context,
        uri: Uri,
        replaceExisting: Boolean = true,
        onResult: (AppDataBackupHelper.ImportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = AppDataBackupHelper.importDataFromUri(
                context = context,
                uri = uri,
                database = db,
                replaceExisting = replaceExisting
            )
            if (result.success) {
                repository.cleanOrphanRecords()
                repository.updateVariosOwnerCategories()
                TaskAlarmScheduler.rescheduleAllActiveAlarms(context)
            }
            onResult(result)
        }
    }

    /**
     * Inserta la publicación en la base de datos y lanza notificación pop-up en el móvil si:
     * 1. Las notificaciones de publicaciones de socios están activas en ajustes (`notificationSettings.newSocialPosts == true`).
     * 2. El autor de la modificación es OTRA persona distinta al usuario actual ("siempre y cuando no lo modifique yo").
     *
     * Si la modificación la realiza el propio usuario actual ("yo"), no se emite pop-up para evitar auto-notificaciones.
     */
    private suspend fun insertAndNotifyPost(post: SocialPostEntity) {
        val postId = repository.insertPost(post)
        val savedPost = if (post.id > 0) post else post.copy(id = postId)

        val me = userProfile.value.name.ifEmpty { "Carlos Vicente" }
        val author = savedPost.authorName.trim()
        val isFromOther = !author.equals(me.trim(), ignoreCase = true)
        
        val isWeatherAlert = savedPost.isAlert && (
            savedPost.content.contains("Helada", ignoreCase = true) ||
            savedPost.content.contains("Lluvia", ignoreCase = true) ||
            savedPost.content.contains("Viento", ignoreCase = true) ||
            savedPost.content.contains("Calor", ignoreCase = true) ||
            savedPost.content.contains("Granizo", ignoreCase = true) ||
            savedPost.authorName.contains("Alerta", ignoreCase = true)
        )

        val notificationsActive = if (isWeatherAlert) {
            val ns = notificationSettings.value
            val content = savedPost.content
            val is7Days = content.contains("7 DÍAS", ignoreCase = true) || content.contains("7 días", ignoreCase = true)
            val is2Days = content.contains("48 HORAS", ignoreCase = true) || content.contains("48h", ignoreCase = true) || content.contains("2 Días", ignoreCase = true) || content.contains("2 días", ignoreCase = true)
            val isRealtime = content.contains("TIEMPO REAL", ignoreCase = true) || content.contains("Tiempo Real", ignoreCase = true)

            when {
                content.contains("Helada", ignoreCase = true) -> {
                    ns.frostAlert && when {
                        is7Days -> ns.frostNotify7Days
                        is2Days -> ns.frostNotify2Days
                        isRealtime -> ns.frostNotifyRealtime
                        else -> true
                    }
                }
                content.contains("Viento", ignoreCase = true) -> {
                    ns.windAlert && when {
                        is7Days -> ns.windNotify7Days
                        is2Days -> ns.windNotify2Days
                        isRealtime -> ns.windNotifyRealtime
                        else -> true
                    }
                }
                content.contains("Calor", ignoreCase = true) -> {
                    ns.heatAlert && when {
                        is7Days -> ns.heatNotify7Days
                        is2Days -> ns.heatNotify2Days
                        isRealtime -> ns.heatNotifyRealtime
                        else -> true
                    }
                }
                content.contains("Lluvia", ignoreCase = true) -> {
                    ns.rainAlert && when {
                        is7Days -> ns.rainNotify7Days
                        is2Days -> ns.rainNotify2Days
                        isRealtime -> ns.rainNotifyRealtime
                        else -> true
                    }
                }
                content.contains("Granizo", ignoreCase = true) -> {
                    ns.hailAlert && when {
                        is7Days -> ns.hailNotify7Days
                        is2Days -> ns.hailNotify2Days
                        isRealtime -> ns.hailNotifyRealtime
                        else -> true
                    }
                }
                else -> true
            }
        } else {
            notificationSettings.value.newSocialPosts
        }

        if (isFromOther && notificationsActive) {
            // 1. Pop-up en la barra del sistema móvil (Heads-Up Banner con vibración y prioridad alta)
            AppNotificationHelper.notifySocialActivity(
                context = getApplication(),
                post = savedPost,
                currentUserName = me,
                notificationsEnabled = true
            )

            // 2. Pop-up visual flotante dentro de la aplicación móvil
            val summary = AppNotificationHelper.getActionSummary(savedPost)
            val title = AppNotificationHelper.formatNotificationTitle(savedPost)
            val cleanContent = AppNotificationHelper.simplifyPostContent(savedPost)
            _inAppNotification.value = InAppNotificationData(
                id = savedPost.id,
                title = title,
                message = summary,
                authorName = author,
                orchardName = savedPost.orchardName,
                fullContent = cleanContent,
                iconType = AppNotificationHelper.getActionIconType(savedPost),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun navigateTo(screen: Screen) {
        screenHistory.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            _currentScreen.value = screenHistory.removeAt(screenHistory.size - 1)
        } else {
            _currentScreen.value = Screen.Dashboard
        }
    }

    // Bottom Bar Active Tab
    val activeBottomTab = _currentScreen.map { screen ->
        when (screen) {
            is Screen.Dashboard, is Screen.OrchardDetail, is Screen.AddOrchard -> "inicio"
            is Screen.CalendarView -> "tareas"
            is Screen.WorkParts -> "partes"
            is Screen.Finances -> "finanzas"
            is Screen.Social, is Screen.NewPost, is Screen.Settings, is Screen.NotificationSettings, is Screen.DoseCalculator -> "socios"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "inicio")

    // Orchards
    val allOrchards: StateFlow<List<OrchardEntity>> = repository.allOrchards
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Filter for Dashboard: "propios" (Propiedad / Propios), "vr" (V&R C.B.), "otros" (Otros)
    private val _dashboardFilter = MutableStateFlow("propios")
    val dashboardFilter: StateFlow<String> = _dashboardFilter.asStateFlow()

    fun setDashboardFilter(filter: String) {
        _dashboardFilter.value = filter
    }

    enum class OrchardCardViewMode {
        CUADROS,
        BARRAS
    }

    private val _orchardCardViewMode = MutableStateFlow(
        try {
            val p = application.getSharedPreferences("vyr_dashboard_prefs", Context.MODE_PRIVATE)
            if (p.getString("card_view_mode", "cuadros") == "barras") OrchardCardViewMode.BARRAS else OrchardCardViewMode.CUADROS
        } catch (e: Exception) {
            OrchardCardViewMode.CUADROS
        }
    )
    val orchardCardViewMode: StateFlow<OrchardCardViewMode> = _orchardCardViewMode.asStateFlow()

    fun setOrchardCardViewMode(mode: OrchardCardViewMode) {
        _orchardCardViewMode.value = mode
        try {
            val p = getApplication<Application>().getSharedPreferences("vyr_dashboard_prefs", Context.MODE_PRIVATE)
            p.edit().putString("card_view_mode", if (mode == OrchardCardViewMode.BARRAS) "barras" else "cuadros").apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val filteredOrchards: StateFlow<List<OrchardEntity>> = combine(allOrchards, _dashboardFilter) { list, filter ->
        when (filter) {
            "vr", "v_y_r_cb" -> list.filter { it.ownerType == "v_y_r_cb" || it.ownerType == "vr" }
            "otros" -> list.filter { it.ownerType == "otros" }
            "propios", "propiedad" -> list.filter { it.ownerType == "propios" || it.ownerType == "propiedad" }
            else -> list.filter { it.ownerType == "propios" || it.ownerType == "propiedad" }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Weather Cache by GPS coords
    private val _weatherMap = MutableStateFlow<Map<String, OrchardWeather>>(emptyMap())
    val weatherMap: StateFlow<Map<String, OrchardWeather>> = _weatherMap.asStateFlow()
    private val reportedRainAlertsToday = mutableSetOf<String>()
    private val reportedFrostAlertsToday = mutableSetOf<String>()

    fun loadWeatherForOrchards(orchards: List<OrchardEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentMap = _weatherMap.value.toMutableMap()
            // Provide immediate fallback for any missing coords so UI has weather immediately
            orchards.forEach { orchard ->
                if (!currentMap.containsKey(orchard.locationGps)) {
                    val lat = orchard.locationGps.split(",").firstOrNull()?.trim()?.toDoubleOrNull() ?: 39.15
                    currentMap[orchard.locationGps] = WeatherService.getDefaultFallbackWeather(lat)
                }
            }
            _weatherMap.value = currentMap

            // Fetch real weather in parallel asynchronously
            val results = orchards.map { orchard ->
                async {
                    val realWeather = WeatherService.fetchWeatherForLocation(orchard.locationGps)
                    orchard to realWeather
                }
            }.awaitAll()

            val updatedMap = _weatherMap.value.toMutableMap()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            results.forEach { (orchard, weather) ->
                updatedMap[orchard.locationGps] = weather

                // Detect rain/storm danger in the forecast and broadcast to Socios
                val rainHazard = weather.dailyForecast.take(4).firstOrNull { forecast ->
                    forecast.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99) ||
                    forecast.iconType in listOf("rainy", "showers", "storm", "drizzle")
                }

                if (rainHazard != null) {
                    val alertKey = "${orchard.id}_${todayStr}_${rainHazard.dayOfWeek}"
                    if (!reportedRainAlertsToday.contains(alertKey)) {
                        reportedRainAlertsToday.add(alertKey)
                        val alertPost = SocialPostEntity(
                            authorName = "Alerta Meteorológica",
                            authorAvatar = null,
                            orchardName = orchard.name,
                            timestampText = "Alerta del día • ${orchard.name}",
                            content = "• Huerto: ${orchard.name}\n• Previsión: ${rainHazard.conditionDesc} el ${rainHazard.dayOfWeek} (${rainHazard.minTemp}°C / ${rainHazard.maxTemp}°C)",
                            photoUri = null,
                            isAlert = true,
                            alertTitle = "🌧️ ${orchard.name} • Peligro de lluvia"
                        )
                        insertAndNotifyPost(alertPost)
                    }
                }

                // -------------------------------------------------------------
                // SISTEMA DE 3 ALERTAS DE HELADA (7 Días, 2 Días y Tiempo Real)
                // -------------------------------------------------------------
                val frostSettings = notificationSettings.value
                val frostEnabled = frostSettings.frostAlert
                val frostThreshold = frostSettings.frostThresholdTemp.toIntOrNull() ?: 2

                if (frostEnabled) {
                    // ETAPA 3: Alerta en Tiempo Real (Momento exacto en que llega o baja del parámetro fijado)
                    if (weather.currentTemp <= frostThreshold) {
                        val alertKey = "${orchard.id}_${todayStr}_frost_realtime_${weather.currentTemp}"
                        if (!reportedFrostAlertsToday.contains(alertKey)) {
                            reportedFrostAlertsToday.add(alertKey)
                            val alertPost = SocialPostEntity(
                                authorName = "Alerta de Helada (Tiempo Real)",
                                authorAvatar = null,
                                orchardName = orchard.name,
                                timestampText = "Alerta Crítica • Momento exacto",
                                content = "• Huerto: ${orchard.name}\n• Sensor actual: ${weather.currentTemp} °C (umbral: $frostThreshold °C alcanzado)",
                                photoUri = null,
                                isAlert = true,
                                alertTitle = "❄️ ${orchard.name} • Helada ahora"
                            )
                            insertAndNotifyPost(alertPost)
                        }
                    }

                    // ETAPA 2: Aviso a 2 Días / 48 Horas (Alta Fiabilidad)
                    val nearHazard = weather.dailyForecast.take(3).firstOrNull { it.minTemp <= frostThreshold }
                    if (nearHazard != null) {
                        val alertKey = "${orchard.id}_${todayStr}_frost_2days_${nearHazard.dateStr}"
                        if (!reportedFrostAlertsToday.contains(alertKey)) {
                            reportedFrostAlertsToday.add(alertKey)
                            val alertPost = SocialPostEntity(
                                authorName = "Alerta de Helada (48 Horas)",
                                authorAvatar = null,
                                orchardName = orchard.name,
                                timestampText = "Previsión 48h • ${nearHazard.dayOfWeek}",
                                content = "• Huerto: ${orchard.name}\n• Previsión: Mínima de ${nearHazard.minTemp} °C para el ${nearHazard.dayOfWeek} (umbral: $frostThreshold °C)",
                                photoUri = null,
                                isAlert = true,
                                alertTitle = "❄️ ${orchard.name} • Helada 48h"
                            )
                            insertAndNotifyPost(alertPost)
                        }
                    }

                    // ETAPA 1: Aviso Preventivo a 7 Días (Medio Plazo)
                    val midTermHazard = weather.dailyForecast.drop(3).take(4).firstOrNull { it.minTemp <= frostThreshold }
                    if (midTermHazard != null) {
                        val alertKey = "${orchard.id}_${todayStr}_frost_7days_${midTermHazard.dateStr}"
                        if (!reportedFrostAlertsToday.contains(alertKey)) {
                            reportedFrostAlertsToday.add(alertKey)
                            val alertPost = SocialPostEntity(
                                authorName = "Alerta de Helada (7 Días)",
                                authorAvatar = null,
                                orchardName = orchard.name,
                                timestampText = "Previsión 7 días • ${midTermHazard.dayOfWeek}",
                                content = "• Huerto: ${orchard.name}\n• Previsión: Mínima de ${midTermHazard.minTemp} °C para el ${midTermHazard.dayOfWeek} (umbral: $frostThreshold °C)",
                                photoUri = null,
                                isAlert = true,
                                alertTitle = "❄️ ${orchard.name} • Helada 7 días"
                            )
                            insertAndNotifyPost(alertPost)
                        }
                    }
                }
            }
            _weatherMap.value = updatedMap
        }
    }

    private val lonjaCheckMutex = Mutex()

    fun checkAndPublishLonjaMarketAlert(forceManualCheck: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!forceManualCheck && !lonjaCheckMutex.tryLock()) {
                // Ya hay una comprobación en curso, evitar ejecuciones simultáneas
                return@launch
            }
            try {
                // Eliminar posibles publicaciones duplicadas de ejecuciones anteriores
                repository.cleanupDuplicateLonjaPosts()
                // Limpiar texto obsoleto o preámbulos extensos de posts previos de la Lonja si existieran
                val existingPosts = repository.allPosts.first()
                existingPosts.filter { it.authorName.contains("Lonja", ignoreCase = true) || it.orchardName == "Mercado y Lonjas" }.forEach { post ->
                    val bulletLines = post.content.lines().map { it.trim() }.filter { it.startsWith("•") }
                    val cleaned = if (bulletLines.isNotEmpty()) {
                        bulletLines.joinToString("\n") { line ->
                            line.replace("(= (", "(=").replace("%))", "%)")
                        }
                    } else {
                        post.content
                            .replace(Regex("📊 NUEVA COTIZACIÓN[^\n]*\n?"), "")
                            .replace(Regex("📅 Semana[^\n]*\n?"), "")
                            .replace("Cotizaciones oficiales para tus huertos:\n", "")
                            .replace("Cotizaciones oficiales para tus huertos:", "")
                            .replace("Precios de referencia oficiales destacados:\n", "")
                            .replace("Precios de referencia oficiales destacados:", "")
                            .replace("\n\nConsulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
                            .replace("Consulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
                            .trim()
                    }
                    if (cleaned != post.content) {
                        repository.insertPost(post.copy(content = cleaned, alertTitle = "📊 Precios Lonja de Cítricos"))
                    }
                }

                val currentWeekLabel = com.example.data.LonjaMarketManager.getCurrentWeekLabel()
                // Si ya existe la publicación de esta semana en la base de datos y no es forzado, no lanzar notificación
                val alreadyPublishedThisWeek = existingPosts.any { post ->
                    (post.authorName.contains("Lonja", ignoreCase = true) || post.orchardName == "Mercado y Lonjas") &&
                    (post.timestampText == currentWeekLabel || post.timestampText.contains(currentWeekLabel.take(10)))
                }
                if (!forceManualCheck && alreadyPublishedThisWeek) {
                    return@launch
                }

                val context = getApplication<Application>()
                val orchards = repository.allOrchards.first()
                val result = com.example.data.LonjaMarketManager.checkLonjaWeeklyUpdate(
                    context = context,
                    forceCheck = forceManualCheck,
                    orchards = orchards
                )
                if (result.isNewUpdate && result.postToNotify != null) {
                    insertAndNotifyPost(result.postToNotify)
                }

                // Verificación automática de resumen semanal y mensual de costes
                try {
                    checkCostSummaries(forceWeekly = false, forceMonthly = false)
                } catch (_: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (lonjaCheckMutex.isLocked) {
                    try { lonjaCheckMutex.unlock() } catch (_: Exception) {}
                }
            }
        }
    }

    fun testLonjaWeeklyNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val orchards = repository.allOrchards.first()
            val result = com.example.data.LonjaMarketManager.checkLonjaWeeklyUpdate(
                context = context,
                forceCheck = true,
                orchards = orchards
            )
            if (result.postToNotify != null) {
                insertAndNotifyPost(result.postToNotify)
            }
        }
    }

    fun testWeeklyCostSummaryNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            checkCostSummaries(forceWeekly = true, forceMonthly = false)
        }
    }

    fun testMonthlyCostSummaryNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            checkCostSummaries(forceWeekly = false, forceMonthly = true)
        }
    }

    fun testWeeklyCostSummaryVyrNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            checkCostSummaries(forceWeeklyVyr = true)
        }
    }

    fun testMonthlyCostSummaryVyrNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            checkCostSummaries(forceMonthlyVyr = true)
        }
    }

    fun testWeeklyCostSummaryOtrosNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            checkCostSummaries(forceWeeklyOtros = true)
        }
    }

    fun testMonthlyCostSummaryOtrosNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            checkCostSummaries(forceMonthlyOtros = true)
        }
    }

    suspend fun checkCostSummaries(
        forceWeekly: Boolean = false,
        forceMonthly: Boolean = false,
        forceWeeklyVyr: Boolean = false,
        forceMonthlyVyr: Boolean = false,
        forceWeeklyOtros: Boolean = false,
        forceMonthlyOtros: Boolean = false
    ) {
        val settings = repository.notificationSettings.first() ?: NotificationSettingsEntity()
        val userProfile = repository.userProfile.first() ?: UserProfileEntity()
        val titularVr = userProfile.titularVr.ifBlank { "V&R C.B." }
        val titularOtros = userProfile.titularOtros.ifBlank { "2ª C.B." }
        val parts = repository.allWorkParts.first()
        val orchards = repository.allOrchards.first()
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("vyr_cost_notifications", Context.MODE_PRIVATE)

        val cal = Calendar.getInstance()
        val currentWeek = "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
        val currentMonth = "${cal.get(Calendar.YEAR)}-M${cal.get(Calendar.MONTH)}"
        val isMonday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
        val isFirstDaysOfMonth = cal.get(Calendar.DAY_OF_MONTH) <= 3

        // 1. Resumen Semanal General
        if (settings.weeklyCostSummary && (forceWeekly || (isMonday && prefs.getString("last_weekly_cost_week", "") != currentWeek))) {
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val recentParts = parts.filter { it.dateTimestamp >= sevenDaysAgo }
            val partsToUse = if (recentParts.isNotEmpty()) recentParts else parts.take(5)

            val totalLabor = partsToUse.sumOf { it.totalCost }
            val totalMaterials = partsToUse.sumOf { part ->
                MaterialsJsonHelper.fromJson(part.materialsJson).sumOf { it.totalCost }
            }
            val totalCost = totalLabor + totalMaterials
            val totalHours = partsToUse.sumOf { it.hours }
            val topOrchard = partsToUse.groupBy { it.orchardName }
                .maxByOrNull { entry ->
                    entry.value.sumOf { p -> p.totalCost + MaterialsJsonHelper.fromJson(p.materialsJson).sumOf { m -> m.totalCost } }
                }?.key ?: "Huertos Propios"

            val formattedTotal = String.format(Locale.GERMANY, "%.2f", totalCost)
            val formattedHours = String.format(Locale.GERMANY, "%.1f", totalHours)
            val formattedLabor = String.format(Locale.GERMANY, "%.2f", totalLabor)
            val formattedMaterials = String.format(Locale.GERMANY, "%.2f", totalMaterials)

            val content = buildString {
                append("• Huerto con mayor actividad: $topOrchard\n")
                append("• Inversión total 7 días: $formattedTotal €\n")
                append("• Mano de obra: $formattedLabor € ($formattedHours h)\n")
                append("• Materiales e insumos: $formattedMaterials €")
            }

            val post = SocialPostEntity(
                authorName = "Control de Costes",
                orchardName = topOrchard,
                timestampText = "Resumen Semanal • Balance General",
                content = content,
                isAlert = true,
                alertTitle = "📊 Resumen Semanal de Costes"
            )

            insertAndNotifyPost(post)
            prefs.edit().putString("last_weekly_cost_week", currentWeek).apply()
        }

        // 2. Resumen Mensual General
        if (settings.monthlyCostSummary && (forceMonthly || (isFirstDaysOfMonth && prefs.getString("last_monthly_cost_month", "") != currentMonth))) {
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            val recentParts = parts.filter { it.dateTimestamp >= thirtyDaysAgo }
            val partsToUse = if (recentParts.isNotEmpty()) recentParts else parts

            val laborCost = partsToUse.sumOf { it.totalCost }
            val materialsCost = partsToUse.sumOf { part ->
                MaterialsJsonHelper.fromJson(part.materialsJson).sumOf { it.totalCost }
            }
            val totalCost = laborCost + materialsCost
            val totalHours = partsToUse.sumOf { it.hours }

            val formattedTotal = String.format(Locale.GERMANY, "%.2f", totalCost)
            val formattedLabor = String.format(Locale.GERMANY, "%.2f", laborCost)
            val formattedMaterials = String.format(Locale.GERMANY, "%.2f", materialsCost)

            val content = buildString {
                append("• Gasto consolidado mensual: $formattedTotal €\n")
                append("• Mano de obra: $formattedLabor € (${String.format(Locale.GERMANY, "%.1f", totalHours)} h)\n")
                append("• Materiales e insumos: $formattedMaterials €\n")
                append("• Total partes registrados: ${partsToUse.size}")
            }

            val post = SocialPostEntity(
                authorName = "Control de Costes",
                orchardName = "General",
                timestampText = "Balance Mensual • Cierre General",
                content = content,
                isAlert = true,
                alertTitle = "📈 Balance Mensual de Costes"
            )

            insertAndNotifyPost(post)
            prefs.edit().putString("last_monthly_cost_month", currentMonth).apply()
        }

        // 3. Resumen Semanal EXCLUSIVO 1ª CB (V&R C.B.)
        if (settings.weeklyCostSummaryVyr && (forceWeeklyVyr || (isMonday && prefs.getString("last_weekly_cost_week_vyr", "") != currentWeek))) {
            val cbOrchards = orchards.filter { orchard ->
                val ownerType = orchard.ownerType.lowercase().trim()
                val name = orchard.name.lowercase().trim()
                val ownerName = orchard.ownerName.lowercase().trim()
                ownerType in listOf("v_y_r_cb", "vr", "v&r", "cb") ||
                        name.contains("v&r") || name.contains("cb") || name.contains("san jaime") ||
                        ownerName.contains("v&r") || ownerName.contains("cb") ||
                        (titularVr.isNotBlank() && (ownerType.contains(titularVr.lowercase()) || name.contains(titularVr.lowercase()) || ownerName.contains(titularVr.lowercase())))
            }
            val cbOrchardIds = cbOrchards.map { it.id }.toSet()
            val cbOrchardNames = cbOrchards.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()

            val cbParts = parts.filter { part ->
                val category = part.ownerCategory.trim().lowercase(Locale.getDefault())
                val oName = part.orchardName.trim().lowercase(Locale.getDefault())
                val obs = part.observations.lowercase(Locale.getDefault())

                category in listOf("v&r", "v&r c.b.", "vr", "v_y_r_cb", "cb") ||
                        category.contains("v&r") ||
                        (titularVr.isNotBlank() && category.contains(titularVr.lowercase(Locale.getDefault()))) ||
                        cbOrchardIds.contains(part.orchardId) ||
                        cbOrchardNames.contains(oName) ||
                        oName.contains("v&r") || oName.contains("cb") || oName.contains("san jaime") ||
                        obs.contains("v&r") || obs.contains("c.b.") || obs.contains("cb ")
            }

            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val recentCbParts = cbParts.filter { it.dateTimestamp >= sevenDaysAgo }
            val partsToUse = if (recentCbParts.isNotEmpty()) recentCbParts else if (cbParts.isNotEmpty()) cbParts.take(5) else parts.take(3)

            val totalLabor = partsToUse.sumOf { it.totalCost }
            val totalMaterials = partsToUse.sumOf { part ->
                MaterialsJsonHelper.fromJson(part.materialsJson).sumOf { it.totalCost }
            }
            val totalCost = totalLabor + totalMaterials
            val totalHours = partsToUse.sumOf { it.hours }
            val topOrchard = partsToUse.groupBy { it.orchardName }
                .maxByOrNull { entry ->
                    entry.value.sumOf { p -> p.totalCost + MaterialsJsonHelper.fromJson(p.materialsJson).sumOf { m -> m.totalCost } }
                }?.key ?: (cbOrchards.firstOrNull()?.name ?: "$titularVr (San Jaime)")

            val formattedTotal = String.format(Locale.GERMANY, "%.2f", totalCost)
            val formattedHours = String.format(Locale.GERMANY, "%.1f", totalHours)
            val formattedLabor = String.format(Locale.GERMANY, "%.2f", totalLabor)
            val formattedMaterials = String.format(Locale.GERMANY, "%.2f", totalMaterials)

            val content = buildString {
                append("• Huerto con mayor actividad: $topOrchard\n")
                append("• Inversión total 7 días: $formattedTotal €\n")
                append("• Mano de obra: $formattedLabor € ($formattedHours h)\n")
                append("• Materiales e insumos: $formattedMaterials €")
            }

            val post = SocialPostEntity(
                authorName = "Control de Costes $titularVr",
                orchardName = topOrchard,
                timestampText = "Resumen Semanal • Balance",
                content = content,
                isAlert = true,
                alertTitle = "📊 Resumen Semanal de Costes $titularVr"
            )

            insertAndNotifyPost(post)
            prefs.edit().putString("last_weekly_cost_week_vyr", currentWeek).apply()
        }

        // 4. Resumen Mensual EXCLUSIVO 1ª CB (V&R C.B.)
        if (settings.monthlyCostSummaryVyr && (forceMonthlyVyr || (isFirstDaysOfMonth && prefs.getString("last_monthly_cost_month_vyr", "") != currentMonth))) {
            val cbOrchards = orchards.filter { orchard ->
                val ownerType = orchard.ownerType.lowercase().trim()
                val name = orchard.name.lowercase().trim()
                val ownerName = orchard.ownerName.lowercase().trim()
                ownerType in listOf("v_y_r_cb", "vr", "v&r", "cb") ||
                        name.contains("v&r") || name.contains("cb") || name.contains("san jaime") ||
                        ownerName.contains("v&r") || ownerName.contains("cb") ||
                        (titularVr.isNotBlank() && (ownerType.contains(titularVr.lowercase()) || name.contains(titularVr.lowercase()) || ownerName.contains(titularVr.lowercase())))
            }
            val cbOrchardIds = cbOrchards.map { it.id }.toSet()
            val cbOrchardNames = cbOrchards.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()

            val cbParts = parts.filter { part ->
                val category = part.ownerCategory.trim().lowercase(Locale.getDefault())
                val oName = part.orchardName.trim().lowercase(Locale.getDefault())
                val obs = part.observations.lowercase(Locale.getDefault())

                category in listOf("v&r", "v&r c.b.", "vr", "v_y_r_cb", "cb") ||
                        category.contains("v&r") ||
                        (titularVr.isNotBlank() && category.contains(titularVr.lowercase(Locale.getDefault()))) ||
                        cbOrchardIds.contains(part.orchardId) ||
                        cbOrchardNames.contains(oName) ||
                        oName.contains("v&r") || oName.contains("cb") || oName.contains("san jaime") ||
                        obs.contains("v&r") || obs.contains("c.b.") || obs.contains("cb ")
            }

            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            val recentCbParts = cbParts.filter { it.dateTimestamp >= thirtyDaysAgo }
            val partsToUse = if (recentCbParts.isNotEmpty()) recentCbParts else if (cbParts.isNotEmpty()) cbParts else parts

            val laborCost = partsToUse.sumOf { it.totalCost }
            val materialsCost = partsToUse.sumOf { part ->
                MaterialsJsonHelper.fromJson(part.materialsJson).sumOf { it.totalCost }
            }
            val totalCost = laborCost + materialsCost
            val totalHours = partsToUse.sumOf { it.hours }

            val formattedTotal = String.format(Locale.GERMANY, "%.2f", totalCost)
            val formattedLabor = String.format(Locale.GERMANY, "%.2f", laborCost)
            val formattedMaterials = String.format(Locale.GERMANY, "%.2f", materialsCost)

            val content = buildString {
                append("• Gasto consolidado mensual: $formattedTotal €\n")
                append("• Mano de obra: $formattedLabor € (${String.format(Locale.GERMANY, "%.1f", totalHours)} h)\n")
                append("• Materiales e insumos: $formattedMaterials €\n")
                append("• Total partes registrados: ${partsToUse.size}")
            }

            val post = SocialPostEntity(
                authorName = "Control de Costes $titularVr",
                orchardName = "General",
                timestampText = "Balance Mensual • Cierre",
                content = content,
                isAlert = true,
                alertTitle = "📈 Balance Mensual de Costes $titularVr"
            )

            insertAndNotifyPost(post)
            prefs.edit().putString("last_monthly_cost_month_vyr", currentMonth).apply()
        }

        // 5. Resumen Semanal EXCLUSIVO 2ª CB / Otros
        if (settings.weeklyCostSummaryOtros && (forceWeeklyOtros || (isMonday && prefs.getString("last_weekly_cost_week_otros", "") != currentWeek))) {
            val otrosOrchards = orchards.filter { orchard ->
                val ownerType = orchard.ownerType.lowercase().trim()
                val name = orchard.name.lowercase().trim()
                val ownerName = orchard.ownerName.lowercase().trim()
                ownerType == "otros" ||
                        (titularOtros.isNotBlank() && (ownerType.contains(titularOtros.lowercase()) || name.contains(titularOtros.lowercase()) || ownerName.contains(titularOtros.lowercase())))
            }
            val otrosOrchardIds = otrosOrchards.map { it.id }.toSet()
            val otrosOrchardNames = otrosOrchards.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()

            val otrosParts = parts.filter { part ->
                val category = part.ownerCategory.trim().lowercase(Locale.getDefault())
                val oName = part.orchardName.trim().lowercase(Locale.getDefault())
                val obs = part.observations.lowercase(Locale.getDefault())

                category == "otros" ||
                        (titularOtros.isNotBlank() && category.contains(titularOtros.lowercase(Locale.getDefault()))) ||
                        otrosOrchardIds.contains(part.orchardId) ||
                        otrosOrchardNames.contains(oName) ||
                        (titularOtros.isNotBlank() && (oName.contains(titularOtros.lowercase(Locale.getDefault())) || obs.contains(titularOtros.lowercase(Locale.getDefault()))))
            }

            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val recentOtrosParts = otrosParts.filter { it.dateTimestamp >= sevenDaysAgo }
            val partsToUse = if (recentOtrosParts.isNotEmpty()) recentOtrosParts else if (otrosParts.isNotEmpty()) otrosParts.take(5) else parts.take(3)

            val totalLabor = partsToUse.sumOf { it.totalCost }
            val totalMaterials = partsToUse.sumOf { part ->
                MaterialsJsonHelper.fromJson(part.materialsJson).sumOf { it.totalCost }
            }
            val totalCost = totalLabor + totalMaterials
            val totalHours = partsToUse.sumOf { it.hours }
            val topOrchard = partsToUse.groupBy { it.orchardName }
                .maxByOrNull { entry ->
                    entry.value.sumOf { p -> p.totalCost + MaterialsJsonHelper.fromJson(p.materialsJson).sumOf { m -> m.totalCost } }
                }?.key ?: (otrosOrchards.firstOrNull()?.name ?: "$titularOtros (Parcelas)")

            val formattedTotal = String.format(Locale.GERMANY, "%.2f", totalCost)
            val formattedHours = String.format(Locale.GERMANY, "%.1f", totalHours)
            val formattedLabor = String.format(Locale.GERMANY, "%.2f", totalLabor)
            val formattedMaterials = String.format(Locale.GERMANY, "%.2f", totalMaterials)

            val content = buildString {
                append("• Huerto con mayor actividad: $topOrchard\n")
                append("• Inversión total 7 días: $formattedTotal €\n")
                append("• Mano de obra: $formattedLabor € ($formattedHours h)\n")
                append("• Materiales e insumos: $formattedMaterials €")
            }

            val post = SocialPostEntity(
                authorName = "Control de Costes $titularOtros",
                orchardName = topOrchard,
                timestampText = "Resumen Semanal • Balance",
                content = content,
                isAlert = true,
                alertTitle = "📊 Resumen Semanal de Costes $titularOtros"
            )

            insertAndNotifyPost(post)
            prefs.edit().putString("last_weekly_cost_week_otros", currentWeek).apply()
        }

        // 6. Resumen Mensual EXCLUSIVO 2ª CB / Otros
        if (settings.monthlyCostSummaryOtros && (forceMonthlyOtros || (isFirstDaysOfMonth && prefs.getString("last_monthly_cost_month_otros", "") != currentMonth))) {
            val otrosOrchards = orchards.filter { orchard ->
                val ownerType = orchard.ownerType.lowercase().trim()
                val name = orchard.name.lowercase().trim()
                val ownerName = orchard.ownerName.lowercase().trim()
                ownerType == "otros" ||
                        (titularOtros.isNotBlank() && (ownerType.contains(titularOtros.lowercase()) || name.contains(titularOtros.lowercase()) || ownerName.contains(titularOtros.lowercase())))
            }
            val otrosOrchardIds = otrosOrchards.map { it.id }.toSet()
            val otrosOrchardNames = otrosOrchards.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()

            val otrosParts = parts.filter { part ->
                val category = part.ownerCategory.trim().lowercase(Locale.getDefault())
                val oName = part.orchardName.trim().lowercase(Locale.getDefault())
                val obs = part.observations.lowercase(Locale.getDefault())

                category == "otros" ||
                        (titularOtros.isNotBlank() && category.contains(titularOtros.lowercase(Locale.getDefault()))) ||
                        otrosOrchardIds.contains(part.orchardId) ||
                        otrosOrchardNames.contains(oName) ||
                        (titularOtros.isNotBlank() && (oName.contains(titularOtros.lowercase(Locale.getDefault())) || obs.contains(titularOtros.lowercase(Locale.getDefault()))))
            }

            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            val recentOtrosParts = otrosParts.filter { it.dateTimestamp >= thirtyDaysAgo }
            val partsToUse = if (recentOtrosParts.isNotEmpty()) recentOtrosParts else if (otrosParts.isNotEmpty()) otrosParts else parts

            val laborCost = partsToUse.sumOf { it.totalCost }
            val materialsCost = partsToUse.sumOf { part ->
                MaterialsJsonHelper.fromJson(part.materialsJson).sumOf { it.totalCost }
            }
            val totalCost = laborCost + materialsCost
            val totalHours = partsToUse.sumOf { it.hours }

            val formattedTotal = String.format(Locale.GERMANY, "%.2f", totalCost)
            val formattedLabor = String.format(Locale.GERMANY, "%.2f", laborCost)
            val formattedMaterials = String.format(Locale.GERMANY, "%.2f", materialsCost)

            val content = buildString {
                append("• Gasto consolidado mensual: $formattedTotal €\n")
                append("• Mano de obra: $formattedLabor € (${String.format(Locale.GERMANY, "%.1f", totalHours)} h)\n")
                append("• Materiales e insumos: $formattedMaterials €\n")
                append("• Total partes registrados: ${partsToUse.size}")
            }

            val post = SocialPostEntity(
                authorName = "Control de Costes $titularOtros",
                orchardName = "General",
                timestampText = "Balance Mensual • Cierre",
                content = content,
                isAlert = true,
                alertTitle = "📈 Balance Mensual de Costes $titularOtros"
            )

            insertAndNotifyPost(post)
            prefs.edit().putString("last_monthly_cost_month_otros", currentMonth).apply()
        }
    }

    // Lonja Prices
    fun getPriceForVariety(variety: String): VarietyPrice {
        return LonjaPriceDatabase.getPriceForVariety(variety)
    }

    // Work Parts
    val allWorkParts: StateFlow<List<WorkPartEntity>> = repository.allWorkParts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveWorkPart(
        id: Long = 0L,
        orchardId: Long,
        orchardName: String,
        type: String,
        taskName: String,
        hours: Double = 0.0,
        pricePerHour: Double = 0.0,
        totalCost: Double = 0.0,
        observations: String = "",
        materialsJson: String = "",
        photoUri: String? = null,
        dateTimestamp: Long = System.currentTimeMillis(),
        isMonthlyRepeat: Boolean = false,
        kilos: Double = 0.0,
        pricePerKg: Double = 0.0,
        kilosDestrio: Double = 0.0,
        precioDestrio: Double = 0.0,
        indemnizacionSeguro: Double = 0.0,
        ownerCategory: String = "",
        publishToSocial: Boolean = true,
        authorName: String? = null
    ) {
        viewModelScope.launch {
            val calculatedCost = if (type == "tareas" && totalCost <= 0.0) {
                hours * pricePerHour
            } else totalCost

            val finalOwnerCategory = when {
                type.equals("varios", ignoreCase = true) -> "Varios"
                ownerCategory.isNotBlank() -> ownerCategory
                else -> "Mío"
            }

            val entity = WorkPartEntity(
                id = id,
                orchardId = orchardId,
                orchardName = orchardName,
                type = type,
                taskName = taskName,
                hours = hours,
                pricePerHour = pricePerHour,
                totalCost = calculatedCost,
                observations = observations,
                materialsJson = materialsJson,
                photoUri = photoUri,
                dateTimestamp = dateTimestamp,
                isMonthlyRepeat = isMonthlyRepeat,
                kilos = kilos,
                pricePerKg = pricePerKg,
                kilosDestrio = kilosDestrio,
                precioDestrio = precioDestrio,
                indemnizacionSeguro = indemnizacionSeguro,
                ownerCategory = finalOwnerCategory
            )
            repository.insertWorkPart(entity)

            // Automatically publish to Social Area so every work part is reflected in Socios screen
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val harvestDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateTimestamp))

            val postContent = when (type.lowercase()) {
                "produccion" -> {
                    val kilosFormatted = if (kilos % 1.0 == 0.0) kilos.toInt().toString() else "%.1f".format(kilos)
                    val priceFormatted = "%.2f".format(pricePerKg)
                    val totalFormatted = "%.2f".format(calculatedCost)
                    buildString {
                        append("🍊 Parte de Producción / Cosecha en $orchardName:\n")
                        append("• Variedad: $taskName\n")
                        append("• Fecha de cosecha: $harvestDateFormatted\n")
                        if (kilos > 0) append("• Kilos comerciales: $kilosFormatted kg a $priceFormatted €/kg\n")
                        if (kilosDestrio > 0) {
                            val destrioKgStr = if (kilosDestrio % 1.0 == 0.0) kilosDestrio.toInt().toString() else "%.1f".format(kilosDestrio)
                            if (precioDestrio > 0) {
                                append("• Destrío / Industria: $destrioKgStr kg a %.2f €/kg (+%.2f €)\n".format(precioDestrio, kilosDestrio * precioDestrio))
                            } else {
                                append("• Destrío / Merma (descarte): $destrioKgStr kg (0,00 €)\n")
                            }
                        }
                        if (indemnizacionSeguro > 0) {
                            append("• Indemnización Agroseguro / Siniestro: +%.2f €\n".format(indemnizacionSeguro))
                        }
                        if (calculatedCost > 0) append("• Total Liquidación / Ingreso: +$totalFormatted €\n")
                        if (observations.isNotBlank()) append("• Observaciones: ${observations.trim()}")
                    }.trim()
                }
                "goteo" -> {
                    buildString {
                        append("💧 Gasto de Riego / Goteo en $orchardName:\n")
                        append("• Importe: %.2f €\n".format(calculatedCost))
                        if (observations.isNotBlank()) append("• Observaciones: ${observations.trim()}")
                    }.trim()
                }
                "varios" -> {
                    buildString {
                        append("📋 Gasto General (Varios):\n")
                        append("• Concepto: $taskName\n")
                        if (calculatedCost > 0) append("• Importe: %.2f €\n".format(calculatedCost))
                        if (observations.isNotBlank()) append("• Observaciones: ${observations.trim()}")
                    }.trim()
                }
                else -> {
                    buildString {
                        append("🚜 Parte de Trabajo en $orchardName: $taskName\n")
                        if (hours > 0) append("• Horas trabajadas: ${hours}h\n")
                        if (calculatedCost > 0) append("• Coste: %.2f €\n".format(calculatedCost))
                        if (observations.isNotBlank()) append("• Observaciones: ${observations.trim()}")
                    }.trim()
                }
            }

            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = orchardName,
                timestampText = "Hace unos segundos • $orchardName",
                content = postContent,
                photoUri = photoUri,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    // Add Orchard
    fun addOrchard(
        name: String,
        ownerType: String,
        ownerName: String,
        variety: String,
        locationGps: String,
        municipality: String,
        partida: String,
        polygon: String,
        parcel: String,
        hanegadas: Double,
        plantingYear: Int,
        pozo: String,
        sector: String,
        hidrante: String,
        regadorName: String,
        regadorPhone: String,
        photoUri: String? = null,
        authorName: String? = null,
        rootstock: String = ""
    ) {
        viewModelScope.launch {
            val profile = userProfile.value
            val currentUserName = profile.name.ifEmpty { "Usuario" }
            val resolvedOwnerName = when {
                ownerName.isNotBlank() -> ownerName
                ownerType == "v_y_r_cb" || ownerType == "vr" -> profile.titularVr.ifEmpty { "V&R C.B." }
                ownerType == "propios" || ownerType == "propiedad" -> profile.titularPropios.ifEmpty { currentUserName }
                else -> profile.titularOtros.ifEmpty { "Otros" }
            }

            val entity = OrchardEntity(
                name = name,
                ownerType = ownerType,
                ownerName = resolvedOwnerName,
                variety = variety,
                rootstock = rootstock.trim(),
                fruitType = if (variety.contains("Aguacate", ignoreCase = true) || variety.contains("Hass", ignoreCase = true)) "Aguacate" else "Cítricos",
                locationGps = locationGps.trim(),
                municipality = municipality.trim(),
                partida = partida.trim(),
                polygon = polygon.trim(),
                parcel = parcel.trim(),
                hanegadas = hanegadas,
                plantingYear = plantingYear,
                pozo = pozo.trim(),
                sector = sector.trim(),
                hidrante = hidrante.trim(),
                regadorName = regadorName.trim(),
                regadorPhone = regadorPhone.trim(),
                photoUri = photoUri
            )
            repository.insertOrchard(entity)

            // Broadcast to Socios feed
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = name,
                timestampText = "Hace unos segundos • $name",
                content = "🌱 Nueva parcela registrada: Se ha dado de alta el huerto '$name' ($variety, ${hanegadas} hg en ${municipality.ifBlank { "término" }}).",
                photoUri = photoUri,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)

            _dashboardFilter.value = if (ownerType == "v_y_r_cb" || ownerType == "vr") "vr" else if (ownerType == "otros") "otros" else "propios"
            navigateTo(Screen.Dashboard)
        }
    }

    fun updateOrchard(
        id: Long,
        name: String,
        ownerType: String,
        ownerName: String,
        variety: String,
        locationGps: String,
        municipality: String,
        partida: String,
        polygon: String,
        parcel: String,
        hanegadas: Double,
        plantingYear: Int,
        pozo: String,
        sector: String,
        hidrante: String,
        regadorName: String,
        regadorPhone: String,
        photoUri: String? = null,
        authorName: String? = null,
        rootstock: String = ""
    ) {
        viewModelScope.launch {
            val profile = userProfile.value
            val currentUserName = profile.name.ifEmpty { "Usuario" }
            val resolvedOwnerName = when {
                ownerName.isNotBlank() -> ownerName
                ownerType == "v_y_r_cb" || ownerType == "vr" -> profile.titularVr.ifEmpty { "V&R C.B." }
                ownerType == "propios" || ownerType == "propiedad" -> profile.titularPropios.ifEmpty { currentUserName }
                else -> profile.titularOtros.ifEmpty { "Otros" }
            }
            val existing = repository.getOrchardById(id)
            val updated = OrchardEntity(
                id = id,
                name = name,
                ownerType = ownerType,
                ownerName = resolvedOwnerName,
                variety = variety,
                rootstock = rootstock.trim(),
                fruitType = if (variety.contains("Aguacate", ignoreCase = true) || variety.contains("Hass", ignoreCase = true)) "Aguacate" else "Cítricos",
                locationGps = locationGps.trim(),
                municipality = municipality.trim(),
                partida = partida.trim(),
                polygon = polygon.trim(),
                parcel = parcel.trim(),
                hanegadas = hanegadas,
                plantingYear = plantingYear,
                pozo = pozo.trim(),
                sector = sector.trim(),
                hidrante = hidrante.trim(),
                regadorName = regadorName.trim(),
                regadorPhone = regadorPhone.trim(),
                photoUri = photoUri ?: existing?.photoUri
            )
            repository.updateOrchard(updated)

            // Broadcast update to Socios feed
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = name,
                timestampText = "Hace unos segundos • $name",
                content = "✏️ Huerto actualizado: Se han modificado los datos de '$name' ($variety, ${hanegadas} hg en ${municipality.ifBlank { "término" }}).",
                photoUri = updated.photoUri,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)

            navigateTo(Screen.OrchardDetail(id))
        }
    }

    fun updateOrchardPhoto(id: Long, photoUri: String?, authorName: String? = null) {
        viewModelScope.launch {
            val orchard = repository.getOrchardById(id)
            if (orchard != null) {
                repository.updateOrchard(orchard.copy(photoUri = photoUri))
                val currentUser = userProfile.value
                val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
                val post = SocialPostEntity(
                    authorName = author,
                    authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                    orchardName = orchard.name,
                    timestampText = "Hace unos segundos • ${orchard.name}",
                    content = "📸 Nueva foto de parcela: Se ha actualizado la foto del huerto '${orchard.name}'.",
                    photoUri = photoUri,
                    isAlert = false,
                    likesCount = 0,
                    commentsCount = 0
                )
                insertAndNotifyPost(post)
            }
        }
    }

    fun reorderOrchards(newOrder: List<OrchardEntity>) {
        viewModelScope.launch {
            repository.updateOrchardsOrder(newOrder)
        }
    }

    fun deleteOrchard(id: Long, authorName: String? = null) {
        viewModelScope.launch {
            val orchard = repository.getOrchardById(id)
            repository.deleteOrchardById(id)
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = orchard?.name ?: "Huerto",
                timestampText = "Hace unos segundos • ${orchard?.name ?: "Huerto"}",
                content = "🗑️ Huerto eliminado: Se ha dado de baja el huerto '${orchard?.name ?: "Huerto"}' del registro de fincas.",
                photoUri = null,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
            navigateTo(Screen.Dashboard)
        }
    }

    fun deleteWorkPart(part: WorkPartEntity, authorName: String? = null) {
        viewModelScope.launch {
            repository.deleteWorkPartById(part.id)
            // Create social post alerting that an activity/work part was removed and finances were deducted
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Usuario" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = part.orchardName,
                timestampText = "Hace unos segundos • ${part.orchardName}",
                content = "🗑️ Actividad eliminada: Se ha eliminado el parte de trabajo '${part.taskName}'" +
                        (if (part.totalCost > 0) " (Coste: %.2f €)".format(part.totalCost) else "") +
                        " en ${part.orchardName}. Los gastos han sido descontados automáticamente del balance de Finanzas.",
                photoUri = null,
                isAlert = true,
                alertTitle = "Actividad Eliminada",
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    fun deleteCalendarTask(task: CalendarTaskEntity, authorName: String? = null) {
        viewModelScope.launch {
            TaskAlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
            repository.deleteCalendarTaskById(task.id)
            // Create social post alerting that a pending task was cancelled/deleted
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Usuario" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = task.orchardName,
                timestampText = "Hace unos segundos • ${task.orchardName}",
                content = "🗑️ Tarea cancelada / eliminada: Se ha cancelado la tarea '${task.title}' programada para ${formatDateToDisplay(task.dateKey)} (${task.timeText}) en ${task.orchardName}.",
                photoUri = null,
                isAlert = true,
                alertTitle = "Tarea Eliminada",
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    // Social Posts
    val allPosts: StateFlow<List<SocialPostEntity>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleLikePost(postId: Long) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    fun getCommentsForPost(postId: Long): Flow<List<PostCommentEntity>> {
        return repository.getCommentsForPost(postId)
    }

    fun addComment(postId: Long, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val currentUser = userProfile.value
            val author = currentUser.name.ifEmpty { "Carlos Vicente" }
            val nowStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
            val comment = PostCommentEntity(
                postId = postId,
                authorName = author,
                authorAvatar = currentUser.photoUri,
                content = content.trim(),
                timestampText = "Hoy, $nowStr",
                timestamp = System.currentTimeMillis()
            )
            repository.insertComment(comment)
        }
    }

    fun deleteComment(commentId: Long, postId: Long) {
        viewModelScope.launch {
            repository.deleteComment(commentId, postId)
        }
    }

    fun createSocialPost(
        orchardName: String,
        content: String,
        photoUri: String? = null,
        isAlert: Boolean = false,
        alertTitle: String? = null,
        orchardId: Long? = null,
        authorName: String? = null
    ) {
        viewModelScope.launch {
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Usuario" }
            val post = SocialPostEntity(
                orchardId = orchardId,
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = orchardName,
                timestampText = "Hace unos segundos • $orchardName",
                content = content,
                photoUri = photoUri,
                isAlert = isAlert,
                alertTitle = alertTitle,
                isAlertResolved = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
            navigateTo(Screen.Social)
        }
    }

    fun resolveAlert(postId: Long) {
        viewModelScope.launch {
            repository.setAlertResolved(postId, true)
        }
    }

    fun unresolveAlert(postId: Long) {
        viewModelScope.launch {
            repository.setAlertResolved(postId, false)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.deleteAllAlerts()
        }
    }

    fun deleteSocialPost(postId: Long) {
        viewModelScope.launch {
            repository.deletePostById(postId)
        }
    }

    fun clearAllSocialPosts() {
        viewModelScope.launch {
            db.socialPostDao().deleteAllPosts()
        }
    }

    // Calendar & Tasks
    val allCalendarTasks: StateFlow<List<CalendarTaskEntity>> = repository.allCalendarTasks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedCalendarDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedCalendarDate: StateFlow<String> = _selectedCalendarDate.asStateFlow()

    fun setSelectedCalendarDate(dateKey: String) {
        _selectedCalendarDate.value = dateKey
    }

    fun addCalendarTask(
        orchardId: Long?,
        orchardName: String,
        title: String,
        time: String,
        description: String,
        fruitType: String,
        dateKey: String = _selectedCalendarDate.value,
        reminderEnabled: Boolean = false,
        reminderDate: String = "",
        reminderTime: String = "",
        authorName: String? = null
    ) {
        viewModelScope.launch {
            val matchingOrchard = if (orchardId != null) {
                repository.getOrchardById(orchardId)
            } else {
                allOrchards.value.find { it.name.equals(orchardName, ignoreCase = true) }
            }

            val resolvedFruitType = if (matchingOrchard != null) {
                if (matchingOrchard.fruitType.contains("Aguacate", ignoreCase = true) ||
                    matchingOrchard.variety.contains("Aguacate", ignoreCase = true) ||
                    matchingOrchard.variety.contains("Hass", ignoreCase = true) ||
                    matchingOrchard.variety.contains("Bacon", ignoreCase = true) ||
                    matchingOrchard.variety.contains("Fuerte", ignoreCase = true) ||
                    matchingOrchard.variety.contains("Lamb", ignoreCase = true)
                ) "avocado" else "orange"
            } else fruitType

            val task = CalendarTaskEntity(
                orchardId = matchingOrchard?.id ?: orchardId,
                orchardName = matchingOrchard?.name ?: orchardName,
                fruitType = resolvedFruitType,
                dateKey = dateKey,
                timeText = time.ifBlank { "08:00" },
                title = title,
                description = description,
                isDone = false,
                reminderEnabled = reminderEnabled,
                reminderDate = reminderDate,
                reminderTime = reminderTime
            )
            val taskId = repository.insertCalendarTask(task)
            val savedTask = task.copy(id = taskId)

            if (reminderEnabled) {
                TaskAlarmScheduler.scheduleTaskAlarm(getApplication(), savedTask)
            }

            // Publish update to Socios feed
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val reminderInfo = if (reminderEnabled && reminderDate.isNotBlank()) {
                "\n🔔 Notificación programada: ${formatDateToDisplay(reminderDate)} a las ${reminderTime.ifBlank { time }}"
            } else ""
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = task.orchardName,
                timestampText = "Hace unos segundos • ${task.orchardName}",
                content = "📅 Nueva tarea programada: '${task.title}' para el día ${formatDateToDisplay(dateKey)} (${task.timeText}) en ${task.orchardName}." +
                        (if (description.isNotBlank()) "\nObservaciones: $description" else "") + reminderInfo,
                photoUri = null,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    fun completeCalendarTask(task: CalendarTaskEntity, authorName: String? = null) {
        viewModelScope.launch {
            TaskAlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
            repository.setCalendarTaskDone(task.id, true)
            // Publish update to Socios feed
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = task.orchardName,
                timestampText = "Hace unos segundos • ${task.orchardName}",
                content = "Tarea finalizada: '${task.title}' en ${task.orchardName}.",
                photoUri = null,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    fun toggleCalendarTaskDone(task: CalendarTaskEntity, isDone: Boolean, authorName: String? = null) {
        viewModelScope.launch {
            if (isDone) {
                TaskAlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
            } else if (task.reminderEnabled) {
                TaskAlarmScheduler.scheduleTaskAlarm(getApplication(), task.copy(isDone = false))
            }
            repository.setCalendarTaskDone(task.id, isDone)
            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val content = if (isDone) {
                "Tarea finalizada: '${task.title}' en ${task.orchardName}."
            } else {
                "🔄 Tarea reabierta: '${task.title}' en ${task.orchardName} ha vuelto al estado pendiente."
            }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = task.orchardName,
                timestampText = "Hace unos segundos • ${task.orchardName}",
                content = content,
                photoUri = null,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    fun deleteCalendarTaskById(taskId: Long, authorName: String? = null) {
        viewModelScope.launch {
            TaskAlarmScheduler.cancelTaskAlarm(getApplication(), taskId)
            val task = repository.getCalendarTaskById(taskId)
            repository.deleteCalendarTaskById(taskId)
            if (task != null) {
                val currentUser = userProfile.value
                val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
                val post = SocialPostEntity(
                    authorName = author,
                    authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                    orchardName = task.orchardName,
                    timestampText = "Hace unos segundos • ${task.orchardName}",
                    content = "🗑️ Tarea cancelada: Se ha eliminado la tarea '${task.title}' programada para ${formatDateToDisplay(task.dateKey)} (${task.timeText}) en ${task.orchardName}.",
                    photoUri = null,
                    isAlert = false,
                    likesCount = 0,
                    commentsCount = 0
                )
                insertAndNotifyPost(post)
            }
        }
    }

    /**
     * Modifica o activa/desactiva la alarma de una tarea existente (desde el calendario o el huerto).
     */
    fun updateCalendarTaskReminder(
        taskId: Long,
        reminderEnabled: Boolean,
        reminderDate: String,
        reminderTime: String
    ) {
        viewModelScope.launch {
            repository.updateTaskReminder(taskId, reminderEnabled, reminderDate, reminderTime)
            val updatedTask = repository.getCalendarTaskById(taskId)
            if (updatedTask != null) {
                if (reminderEnabled && !updatedTask.isDone) {
                    TaskAlarmScheduler.scheduleTaskAlarm(getApplication(), updatedTask)
                } else {
                    TaskAlarmScheduler.cancelTaskAlarm(getApplication(), taskId)
                }
            }
        }
    }

    /**
     * Lanza inmediatamente un pop-up de prueba de la alarma en el dispositivo móvil
     * para que el agricultor verifique que el banner emergente funciona a la perfección.
     */
    fun testTaskAlarm(task: CalendarTaskEntity) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            TaskAlarmScheduler.triggerImmediateTaskAlarm(app, task)
            _inAppNotification.value = InAppNotificationData(
                title = "⏰ Alarma de Tarea: ${task.title}",
                message = "📍 ${task.orchardName} • ${task.timeText}",
                authorName = "Recordatorio de Labor",
                orchardName = task.orchardName,
                fullContent = "Labor programada: ${task.title}\nHuerto: ${task.orchardName}\nFecha: ${formatDateToDisplay(task.dateKey)} (${task.timeText})\n${task.description}",
                iconType = "new_task"
            )
        }
    }

    /**
     * Monitor activo en segundo plano que comprueba periódicamente las tareas pendientes
     * con alarma programada para hoy para garantizar que el pop-up salte de inmediato.
     */
    private fun startTaskAlarmActiveMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val pendingTasks = repository.getPendingTasksWithReminders()
                    val now = System.currentTimeMillis()
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))

                    for (task in pendingTasks) {
                        val effDate = task.reminderDate.ifBlank { task.dateKey }.trim()
                        val effTime = task.reminderTime.ifBlank { task.timeText.ifBlank { "08:00" } }.trim()
                        if (effDate == todayKey) {
                            val taskTime = try {
                                sdf.parse("$effDate $effTime")?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                            // Si la alarma cae dentro del último minuto o en los próximos 15 segundos
                            if (taskTime > 0L && taskTime in (now - 60_000L)..(now + 15_000L)) {
                                TaskAlarmScheduler.triggerImmediateTaskAlarm(getApplication(), task)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Evitar interrupción del monitor
                }
                delay(30_000L) // Verificar cada 30 segundos
            }
        }
    }

    // Custom Reminders
    val allReminders: StateFlow<List<CustomReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createReminder(dateText: String, timeText: String, note: String) {
        viewModelScope.launch {
            val reminder = CustomReminderEntity(
                dateText = dateText,
                timeText = timeText,
                note = note
            )
            repository.insertReminder(reminder)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
        }
    }

    // Notification Settings
    val notificationSettings: StateFlow<NotificationSettingsEntity> = repository.notificationSettings
        .map { it ?: NotificationSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NotificationSettingsEntity())

    fun updateNotificationSettings(settings: NotificationSettingsEntity) {
        viewModelScope.launch {
            repository.updateNotificationSettings(settings)
        }
    }

    // User Profile
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .map { it ?: UserProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfileEntity())

    private val appPrefs by lazy {
        getApplication<Application>().getSharedPreferences("vyr_app_prefs", Context.MODE_PRIVATE)
    }

    private val _isInitialProfileSetupRequired = MutableStateFlow(
        !getApplication<Application>().getSharedPreferences("vyr_app_prefs", Context.MODE_PRIVATE)
            .getBoolean("initial_profile_setup_completed", false)
    )
    val isInitialProfileSetupRequired: StateFlow<Boolean> = _isInitialProfileSetupRequired.asStateFlow()

    fun completeInitialProfile(name: String, photoUri: String? = null) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val current = userProfile.value
            val finalName = if (cleanName.isNotEmpty()) cleanName else current.name
            repository.updateUserProfile(
                current.copy(
                    name = finalName,
                    titularPropios = finalName,
                    photoUri = photoUri
                )
            )
            if (cleanName.isNotEmpty()) {
                repository.updatePropiosOwnerName(cleanName)
            }
            appPrefs.edit().putBoolean("initial_profile_setup_completed", true).commit()
            _isInitialProfileSetupRequired.value = false
        }
    }

    fun resetInitialProfileSetupForTesting() {
        appPrefs.edit().putBoolean("initial_profile_setup_completed", false).commit()
        _isInitialProfileSetupRequired.value = true
    }

    fun purgeMockData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.cleanupMockData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearAllData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun updateUserProfile(name: String, photoUri: String? = null, updatePhoto: Boolean = false) {
        viewModelScope.launch {
            val current = userProfile.value
            val cleanName = name.trim()
            val finalName = if (cleanName.isNotEmpty()) cleanName else current.name
            val finalPhoto = if (updatePhoto) photoUri else (photoUri ?: current.photoUri)
            repository.updateUserProfile(
                current.copy(
                    name = finalName,
                    titularPropios = finalName,
                    photoUri = finalPhoto
                )
            )
            if (cleanName.isNotEmpty()) {
                repository.updatePropiosOwnerName(cleanName)
            }
        }
    }

    fun updateTitulares(
        titularPropios: String,
        titularVr: String,
        titularOtros: String
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            val cleanPropios = titularPropios.trim().ifEmpty { current.titularPropios }
            val cleanVr = titularVr.trim().ifEmpty { current.titularVr }
            val cleanOtros = titularOtros.trim().ifEmpty { current.titularOtros }

            val oldPropios = current.titularPropios
            val oldVr = current.titularVr
            val oldOtros = current.titularOtros

            repository.updateUserProfile(
                current.copy(
                    name = cleanPropios,
                    titularPropios = cleanPropios,
                    titularVr = cleanVr,
                    titularOtros = cleanOtros
                )
            )

            // Propagate titular name updates to all orchards and work parts
            if (cleanPropios != oldPropios) {
                repository.updatePropiosOwnerName(cleanPropios)
                repository.updateWorkPartOwnerCategory(oldPropios, cleanPropios)
                if (oldPropios != "Mío") repository.updateWorkPartOwnerCategory("Mío", cleanPropios)
                if (oldPropios != "Propiedad") repository.updateWorkPartOwnerCategory("Propiedad", cleanPropios)
            }
            if (cleanVr != oldVr) {
                repository.updateVrOwnerName(cleanVr)
                repository.updateWorkPartOwnerCategory(oldVr, cleanVr)
                if (oldVr != "V&R") repository.updateWorkPartOwnerCategory("V&R", cleanVr)
                if (oldVr != "V&R C.B.") repository.updateWorkPartOwnerCategory("V&R C.B.", cleanVr)
            }
            if (cleanOtros != oldOtros) {
                repository.updateOtrosOwnerName(cleanOtros)
                repository.updateWorkPartOwnerCategory(oldOtros, cleanOtros)
                if (oldOtros != "Otros") repository.updateWorkPartOwnerCategory("Otros", cleanOtros)
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateUserProfile(current.copy(isDarkMode = enabled))
        }
    }

    // Irrigation Schedules for an Orchard
    fun getSchedulesForOrchard(orchardId: Long): Flow<List<IrrigationScheduleEntity>> {
        return repository.getSchedulesForOrchard(orchardId)
    }

    fun saveIrrigationSchedules(orchardId: Long, schedules: List<IrrigationScheduleEntity>, authorName: String? = null) {
        viewModelScope.launch {
            repository.saveSchedules(orchardId, schedules)
            val orchard = repository.getOrchardById(orchardId)
            val orchardName = orchard?.name ?: "Huerto"
            val activeSchedules = schedules.filter { it.isEnabled && (it.startTime.isNotBlank() || it.endTime.isNotBlank()) }
            val scheduleSummary = if (activeSchedules.isNotEmpty()) {
                activeSchedules.joinToString("\n") { "• ${it.dayOfWeek}: ${it.startTime} - ${it.endTime}" }
            } else {
                "• Riego desactivado temporalmente."
            }

            val currentUser = userProfile.value
            val author = authorName?.takeIf { it.isNotBlank() } ?: currentUser.name.ifEmpty { "Carlos Vicente" }
            val post = SocialPostEntity(
                authorName = author,
                authorAvatar = if (author == currentUser.name) currentUser.photoUri else null,
                orchardName = orchardName,
                timestampText = "Hace unos segundos • $orchardName",
                content = "💧 Cambio de horario de riego en '$orchardName':\n$scheduleSummary",
                photoUri = null,
                isAlert = false,
                likesCount = 0,
                commentsCount = 0
            )
            insertAndNotifyPost(post)
        }
    }

    /**
     * Permite simular una actividad realizada por otro socio ("otra persona") en el sistema:
     * - "new_task": Ramón Ripoll programa una nueva tarea
     * - "task_completed": María Valero marca una actividad como completada
     * - "work_part": Paco López registra un parte de campo
     * - "orchard_modified": Vicente Martí modifica un huerto
     * - "new_orchard": Ramón Ripoll da de alta una nueva parcela
     *
     * Al ser una persona distinta a "yo", si las notificaciones de socios están activas,
     * salta inmediatamente el pop-up en el móvil.
     */
    fun simulatePartnerActivity(type: String, partnerName: String = "Ramón Ripoll") {
        viewModelScope.launch {
            val orchards = repository.allOrchards.first()
            val orchard1 = orchards.firstOrNull()
            val orchardName1 = orchard1?.name ?: "Huerto San Jaime"
            val orchardId1 = orchard1?.id

            when (type) {
                "new_task" -> {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val newTask = CalendarTaskEntity(
                        orchardId = orchardId1,
                        orchardName = orchardName1,
                        fruitType = "orange",
                        dateKey = todayStr,
                        timeText = "09:30",
                        title = "Revisión de goteros y desbroce",
                        description = "Revisión programada por $partnerName para verificar presión en sectores.",
                        isDone = false
                    )
                    repository.insertCalendarTask(newTask)
                    val post = SocialPostEntity(
                        authorName = partnerName,
                        authorAvatar = null,
                        orchardName = orchardName1,
                        timestampText = "Hace unos segundos • $orchardName1",
                        content = "📅 Nueva tarea programada: 'Revisión de goteros y desbroce' para el día $todayStr (09:30) en $orchardName1.\nObservaciones: Programada por el socio $partnerName.",
                        photoUri = null,
                        isAlert = false
                    )
                    insertAndNotifyPost(post)
                }
                "task_completed" -> {
                    val partner = if (partnerName == "Ramón Ripoll") "María Valero" else partnerName
                    val pendingTasks = repository.allCalendarTasks.first().filter { !it.isDone }
                    val targetTask = pendingTasks.firstOrNull()
                    if (targetTask != null) {
                        repository.setCalendarTaskDone(targetTask.id, true)
                    }
                    val tName = targetTask?.title ?: "Tratamiento fitosanitario preventivo"
                    val oName = targetTask?.orchardName ?: orchardName1
                    val post = SocialPostEntity(
                        authorName = partner,
                        authorAvatar = null,
                        orchardName = oName,
                        timestampText = "Hace unos segundos • $oName",
                        content = "Tarea finalizada: '$tName' en $oName.",
                        photoUri = null,
                        isAlert = false
                    )
                    insertAndNotifyPost(post)
                }
                "work_part" -> {
                    val partner = if (partnerName == "Ramón Ripoll") "Paco López" else partnerName
                    val part = WorkPartEntity(
                        orchardId = orchardId1 ?: 1L,
                        orchardName = orchardName1,
                        type = "tareas",
                        taskName = "Poda de formación y aclareo",
                        hours = 4.0,
                        pricePerHour = 15.0,
                        totalCost = 60.0,
                        observations = "Labor realizada por $partner. Retiradas ramas secas y chupones.",
                        ownerCategory = "V&R"
                    )
                    repository.insertWorkPart(part)
                    val post = SocialPostEntity(
                        authorName = partner,
                        authorAvatar = null,
                        orchardName = orchardName1,
                        timestampText = "Hace unos segundos • $orchardName1",
                        content = "🚜 Parte de Trabajo en $orchardName1: Poda de formación y aclareo\n• Horas trabajadas: 4.0h\n• Coste: 60.00 €\n• Registrado por: $partner\n• Observaciones: Retiradas ramas secas y chupones.",
                        photoUri = null,
                        isAlert = false
                    )
                    insertAndNotifyPost(post)
                }
                "orchard_modified" -> {
                    val partner = if (partnerName == "Ramón Ripoll") "Vicente Martí" else partnerName
                    val post = SocialPostEntity(
                        authorName = partner,
                        authorAvatar = null,
                        orchardName = orchardName1,
                        timestampText = "Hace unos segundos • $orchardName1",
                        content = "✏️ Huerto actualizado: Se han modificado los datos de '$orchardName1' por $partner (Actualizado hidrante, sector de abonado y teléfono de contacto).",
                        photoUri = null,
                        isAlert = false
                    )
                    insertAndNotifyPost(post)
                }
                "new_orchard" -> {
                    val partner = partnerName
                    val newName = "Huerto La Garrofera"
                    val post = SocialPostEntity(
                        authorName = partner,
                        authorAvatar = null,
                        orchardName = newName,
                        timestampText = "Hace unos segundos • $newName",
                        content = "🌱 Nueva parcela registrada: Se ha dado de alta el huerto '$newName' (Aguacate Hass, 12.5 hg en Carlet) por el socio $partner.",
                        photoUri = null,
                        isAlert = false
                    )
                    insertAndNotifyPost(post)
                }
                else -> {
                    val post = SocialPostEntity(
                        authorName = partnerName,
                        authorAvatar = null,
                        orchardName = orchardName1,
                        timestampText = "Hace unos segundos • $orchardName1",
                        content = "📢 Nueva actualización de $partnerName en $orchardName1.",
                        photoUri = null,
                        isAlert = false
                    )
                    insertAndNotifyPost(post)
                }
            }
        }
    }

    /**
     * Simula una de las 3 etapas del sistema de alertas de heladas:
     * - Stage 1: Aviso preventivo a 7 días (Medio Plazo)
     * - Stage 2: Aviso a 48 horas / 2 días (Alta Fiabilidad)
     * - Stage 3: Alerta Crítica en Tiempo Real (Momento en que llega al parámetro fijado)
     *
     * La alerta se publica en el Muro de Socios y lanza de inmediato la notificación pop-up en el móvil.
     */
    fun simulateFrostAlert(stage: Int, targetOrchardName: String? = null) {
        viewModelScope.launch {
            val orchards = repository.allOrchards.first()
            val orchard = orchards.firstOrNull { targetOrchardName == null || it.name.contains(targetOrchardName, ignoreCase = true) }
                ?: orchards.firstOrNull()
            val oName = orchard?.name ?: (targetOrchardName ?: "Hort de Baix")
            val threshold = notificationSettings.value.frostThresholdTemp

            val post = when (stage) {
                1 -> { // Etapa 1: Previsión a 7 Días
                    SocialPostEntity(
                        authorName = "Alerta de Helada (7 Días)",
                        authorAvatar = null,
                        orchardName = oName,
                        timestampText = "Previsión a 7 días • Modelos Meteorológicos",
                        content = "• Huerto: $oName\n• Previsión: Mínima de 1 °C en 7 días (umbral fijado: $threshold °C)",
                        photoUri = null,
                        isAlert = true,
                        alertTitle = "❄️ $oName • Helada 7 días"
                    )
                }
                2 -> { // Etapa 2: Previsión a 2 Días (48 horas - Alta Fiabilidad)
                    SocialPostEntity(
                        authorName = "Alerta de Helada (48 Horas)",
                        authorAvatar = null,
                        orchardName = oName,
                        timestampText = "Previsión a 48h • Alta Fiabilidad",
                        content = "• Huerto: $oName\n• Previsión: Mínima de 0 °C para pasado mañana (umbral fijado: $threshold °C)",
                        photoUri = null,
                        isAlert = true,
                        alertTitle = "❄️ $oName • Helada 48h"
                    )
                }
                else -> { // Etapa 3: En Tiempo Real (Llegada al parámetro fijado)
                    SocialPostEntity(
                        authorName = "Alerta de Helada (Tiempo Real)",
                        authorAvatar = null,
                        orchardName = oName,
                        timestampText = "Alerta Crítica • Momento exacto",
                        content = "• Huerto: $oName\n• Sensor actual: 1.8 °C (umbral fijado: $threshold °C alcanzado)",
                        photoUri = null,
                        isAlert = true,
                        alertTitle = "❄️ $oName • Helada ahora"
                    )
                }
            }
            insertAndNotifyPost(post)
        }
    }

    /**
     * Simula cualquiera de las alertas climáticas en sus 3 etapas:
     * - type: "frost", "wind", "heat", "rain", "hail"
     * - stage: 1 (7 días), 2 (2 días / 48h), 3 (parámetro alcanzado en tiempo real)
     */
    fun simulateClimateAlert(type: String, stage: Int, targetOrchardName: String? = null) {
        viewModelScope.launch {
            val orchards = repository.allOrchards.first()
            val orchard = orchards.firstOrNull { targetOrchardName == null || it.name.contains(targetOrchardName, ignoreCase = true) }
                ?: orchards.firstOrNull()
            val oName = orchard?.name ?: (targetOrchardName ?: "Hort de Baix")
            val ns = notificationSettings.value

            val post = when (type) {
                "wind" -> {
                    val threshold = ns.windThresholdKmh
                    when (stage) {
                        1 -> SocialPostEntity(
                            authorName = "Alerta de Viento (7 Días)",
                            orchardName = oName,
                            timestampText = "Previsión a 7 días • Viento",
                            content = "• Huerto: $oName\n• Previsión: Rachas superiores a $threshold km/h dentro de 7 días",
                            isAlert = true,
                            alertTitle = "💨 $oName • Viento 7 días"
                        )
                        2 -> SocialPostEntity(
                            authorName = "Alerta de Viento (48 Horas)",
                            orchardName = oName,
                            timestampText = "Previsión a 48h • Viento Fuerte",
                            content = "• Huerto: $oName\n• Previsión: Temporal con rachas superiores a $threshold km/h en 2 días",
                            isAlert = true,
                            alertTitle = "💨 $oName • Viento 48h"
                        )
                        else -> SocialPostEntity(
                            authorName = "Alerta de Viento (Tiempo Real)",
                            orchardName = oName,
                            timestampText = "Alerta Crítica • Momento exacto",
                            content = "• Huerto: $oName\n• Registro actual: Rachas de $threshold km/h (umbral alcanzado)",
                            isAlert = true,
                            alertTitle = "💨 $oName • Viento fuerte"
                        )
                    }
                }
                "heat" -> {
                    val threshold = ns.heatThresholdTemp
                    when (stage) {
                        1 -> SocialPostEntity(
                            authorName = "Alerta Ola de Calor (7 Días)",
                            orchardName = oName,
                            timestampText = "Previsión a 7 días • Ola de Calor",
                            content = "• Huerto: $oName\n• Previsión: Temperaturas que podrían superar los $threshold °C en 7 días",
                            isAlert = true,
                            alertTitle = "☀️ $oName • Calor 7 días"
                        )
                        2 -> SocialPostEntity(
                            authorName = "Alerta Ola de Calor (48 Horas)",
                            orchardName = oName,
                            timestampText = "Previsión a 48h • Altas Temperaturas",
                            content = "• Huerto: $oName\n• Previsión: Máximas extremas superando los $threshold °C en 2 días",
                            isAlert = true,
                            alertTitle = "☀️ $oName • Calor 48h"
                        )
                        else -> SocialPostEntity(
                            authorName = "Alerta Ola de Calor (Tiempo Real)",
                            orchardName = oName,
                            timestampText = "Alerta Crítica • Momento exacto",
                            content = "• Huerto: $oName\n• Temperatura actual: $threshold °C (umbral máximo fijado alcanzado)",
                            isAlert = true,
                            alertTitle = "☀️ $oName • Ola de calor"
                        )
                    }
                }
                "rain" -> {
                    val threshold = ns.rainProbabilityThreshold
                    when (stage) {
                        1 -> SocialPostEntity(
                            authorName = "Alerta de Lluvia (7 Días)",
                            orchardName = oName,
                            timestampText = "Previsión a 7 días • Precipitaciones",
                            content = "• Huerto: $oName\n• Previsión: Probabilidad de lluvia del $threshold% o superior en 7 días",
                            isAlert = true,
                            alertTitle = "🌧️ $oName • Lluvia 7 días"
                        )
                        2 -> SocialPostEntity(
                            authorName = "Alerta de Lluvia (48 Horas)",
                            orchardName = oName,
                            timestampText = "Previsión a 48h • Lluvia Inminente",
                            content = "• Huerto: $oName\n• Previsión: Lluvias con probabilidad del $threshold% en 2 días",
                            isAlert = true,
                            alertTitle = "🌧️ $oName • Lluvia 48h"
                        )
                        else -> SocialPostEntity(
                            authorName = "Alerta de Lluvia (Tiempo Real)",
                            orchardName = oName,
                            timestampText = "Alerta Crítica • Momento exacto",
                            content = "• Huerto: $oName\n• Registro actual: Precipitaciones activas ($threshold% alcanzado)",
                            isAlert = true,
                            alertTitle = "🌧️ $oName • Lluvia ahora"
                        )
                    }
                }
                "hail" -> {
                    val threshold = ns.hailProbabilityThreshold
                    when (stage) {
                        1 -> SocialPostEntity(
                            authorName = "Alerta de Granizo (7 Días)",
                            orchardName = oName,
                            timestampText = "Previsión a 7 días • Tormentas",
                            content = "• Huerto: $oName\n• Previsión: Riesgo de tormentas con granizo en 7 días ($threshold%)",
                            isAlert = true,
                            alertTitle = "⛈️ $oName • Granizo 7 días"
                        )
                        2 -> SocialPostEntity(
                            authorName = "Alerta de Granizo (48 Horas)",
                            orchardName = oName,
                            timestampText = "Previsión a 48h • Alta Probabilidad",
                            content = "• Huerto: $oName\n• Previsión: Alto riesgo de pedrisco o granizo en 2 días ($threshold%)",
                            isAlert = true,
                            alertTitle = "⛈️ $oName • Granizo 48h"
                        )
                        else -> SocialPostEntity(
                            authorName = "Alerta de Granizo (Tiempo Real)",
                            orchardName = oName,
                            timestampText = "Alerta Crítica • Momento exacto",
                            content = "• Huerto: $oName\n• Registro actual: Célula con caída de granizo sobre parcela ($threshold%)",
                            isAlert = true,
                            alertTitle = "⛈️ $oName • Granizo ahora"
                        )
                    }
                }
                else -> {
                    return@launch simulateFrostAlert(stage, targetOrchardName)
                }
            }
            insertAndNotifyPost(post)
        }
    }

    private suspend fun syncExistingTasksToSocialFeed() {
        try {
            val tasks = withTimeoutOrNull(3000L) { repository.allCalendarTasks.first() } ?: emptyList()
            val existingPosts = withTimeoutOrNull(3000L) { repository.allPosts.first() } ?: emptyList()
            tasks.forEach { task ->
                val alreadyPosted = existingPosts.any { post ->
                    post.content.contains(task.title, ignoreCase = true) &&
                    (post.orchardName.equals(task.orchardName, ignoreCase = true) || post.content.contains(task.orchardName, ignoreCase = true))
                }
                if (!alreadyPosted) {
                    val author = userProfile.value.name.ifEmpty { "Carlos Vicente" }
                    val post = SocialPostEntity(
                        authorName = author,
                        authorAvatar = userProfile.value.photoUri,
                        orchardName = task.orchardName,
                        timestampText = "${formatDateToDisplay(task.dateKey)} • ${task.timeText}",
                        content = if (task.isDone) {
                            "Tarea finalizada: '${task.title}' en ${task.orchardName}."
                        } else {
                            "📅 Nueva tarea programada: '${task.title}' para el día ${formatDateToDisplay(task.dateKey)} (${task.timeText}) en ${task.orchardName}." +
                                    (if (task.description.isNotBlank()) "\nObservaciones: ${task.description}" else "")
                        },
                        photoUri = null,
                        isAlert = false,
                        likesCount = 0,
                        commentsCount = 0
                    )
                    repository.insertPost(post)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
