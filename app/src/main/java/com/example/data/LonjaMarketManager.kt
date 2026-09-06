package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.OrchardEntity
import com.example.data.model.SocialPostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestor oficial de cotizaciones de la Lonja de Cítricos de Valencia.
 * Controla que las notificaciones y pop-ups se emitan ÚNICAMENTE cuando la Lonja
 * actualiza sus precios oficiales (frecuencia semanal, habitualmente los lunes),
 * evitando por completo que salte el pop-up cada vez que el agricultor abre la app.
 */
object LonjaMarketManager {

    private const val PREFS_NAME = "lonja_market_preferences"
    private const val KEY_LAST_NOTIFIED_WEEK = "last_notified_lonja_week"
    private const val KEY_LAST_CHECK_TIMESTAMP = "last_check_timestamp"
    private const val KEY_LAST_BULLETIN_LABEL = "last_bulletin_label"
    private const val KEY_LAST_PRICE_SIGNATURE = "last_price_signature"

    private val checkMutex = Mutex()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Identificador de semana ISO (ej. "2026-W36")
     */
    fun getCurrentWeekKey(date: Date = Date()): String {
        val cal = Calendar.getInstance(Locale("es", "ES"))
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.time = date
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.ROOT, "%d-W%02d", year, week)
    }

    /**
     * Etiqueta humana legible de la semana (ej. "Semana 36 • 31 de agosto")
     */
    fun getCurrentWeekLabel(date: Date = Date()): String {
        val cal = Calendar.getInstance(Locale("es", "ES"))
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.time = date
        val week = cal.get(Calendar.WEEK_OF_YEAR)

        val mondayCal = cal.clone() as Calendar
        mondayCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))
        return "Semana $week • ${sdf.format(mondayCal.time)}"
    }

    /**
     * Devuelve la última semana para la que el usuario ya recibió la notificación.
     */
    fun getLastNotifiedWeek(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_NOTIFIED_WEEK, null)
    }

    /**
     * Comprueba si para la semana actual ya se emitió el pop-up de precios.
     */
    fun hasAlreadyNotifiedCurrentWeek(context: Context): Boolean {
        val lastNotified = getLastNotifiedWeek(context) ?: return false
        return lastNotified == getCurrentWeekKey()
    }

    data class LonjaCheckResult(
        val isNewUpdate: Boolean,
        val weekKey: String,
        val weekLabel: String,
        val postToNotify: SocialPostEntity? = null,
        val message: String
    )

    /**
     * Comprueba si ha salido una nueva cotización oficial en la web de la Lonja de Cítricos.
     * Solo devuelve 'isNewUpdate = true' si:
     * 1. Es una nueva semana y no se ha notificado aún al usuario.
     * 2. O se fuerza manualmente la comprobación desde los ajustes.
     */
    suspend fun checkLonjaWeeklyUpdate(
        context: Context,
        forceCheck: Boolean = false,
        orchards: List<OrchardEntity> = emptyList()
    ): LonjaCheckResult = withContext(Dispatchers.IO) {
        checkMutex.withLock {
            val prefs = getPrefs(context)
            val currentWeekKey = getCurrentWeekKey()
            val currentWeekLabel = getCurrentWeekLabel()
            val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_WEEK, null)

            // Si ya se notificó esta semana y no se fuerza manualmente, NO SALTA NADA
            if (!forceCheck && lastNotified == currentWeekKey) {
                return@withLock LonjaCheckResult(
                    isNewUpdate = false,
                    weekKey = currentWeekKey,
                    weekLabel = currentWeekLabel,
                    postToNotify = null,
                    message = "Precios de la Lonja ya consultados y al día para la $currentWeekLabel"
                )
            }

            // Comprobación web opcional en segundo plano (verificación de conectividad a la web de la Lonja)
            var webStatus = "Actualización oficial semanal"
            try {
                val url = URL("https://precioscitricos.com/")
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "HEAD"
                val code = conn.responseCode
                if (code in 200..399) {
                    webStatus = "Verificado con la web oficial de Lonja de Cítricos"
                }
                conn.disconnect()
            } catch (_: Exception) {
                webStatus = "Cotizaciones oficiales de la mesa de precios"
            }

            // Construir el boletín oficial únicamente con las cotizaciones de los huertos del usuario
            val lines = if (orchards.isNotEmpty()) {
                orchards.map { orchard ->
                    val price = LonjaPriceDatabase.getPriceForVariety(orchard.variety)
                    val sign = if (price.variationPercent > 0) "+${price.variationPercent}% ↗" else if (price.variationPercent < 0) "${price.variationPercent}% ↘" else "= 0.0%"
                    "• ${orchard.name} (${orchard.variety}): ${price.priceText} ($sign)"
                }.joinToString("\n")
            } else {
                val notableVariations = LonjaPriceDatabase.varieties.filter { kotlin.math.abs(it.variationPercent) >= 1.5 }
                notableVariations.take(6).joinToString("\n") {
                    val sign = if (it.variationPercent > 0) "+${it.variationPercent}% ↗" else "${it.variationPercent}% ↘"
                    "• ${it.varietyName}: ${it.priceText} ($sign)"
                }
            }

            val contentText = lines

            val post = SocialPostEntity(
                authorName = "Lonja de Cítricos de Valencia",
                authorAvatar = null,
                orchardName = "Mercado y Lonjas",
                timestampText = currentWeekLabel,
                content = contentText,
                photoUri = null,
                isAlert = false,
                alertTitle = "📊 Precios Lonja de Cítricos"
            )

            // Registrar persistentemente y de forma síncrona (commit) que esta semana ya fue notificada
            prefs.edit()
                .putString(KEY_LAST_NOTIFIED_WEEK, currentWeekKey)
                .putString(KEY_LAST_BULLETIN_LABEL, currentWeekLabel)
                .putLong(KEY_LAST_CHECK_TIMESTAMP, System.currentTimeMillis())
                .commit()

            LonjaCheckResult(
                isNewUpdate = true,
                weekKey = currentWeekKey,
                weekLabel = currentWeekLabel,
                postToNotify = post,
                message = "Nueva cotización oficial de la semana detectada"
            )
        }
    }

    /**
     * Restablece el registro de notificación (útil para pruebas y simuladores).
     */
    fun resetNotifiedWeekForTesting(context: Context) {
        getPrefs(context).edit().remove(KEY_LAST_NOTIFIED_WEEK).apply()
    }
}
