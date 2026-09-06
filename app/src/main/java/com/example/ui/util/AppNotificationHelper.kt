package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.SocialPostEntity

object AppNotificationHelper {

    const val CHANNEL_ID_SOCIOS = "vr_agro_socios_popup_channel"
    private const val CHANNEL_NAME_SOCIOS = "Actividad y Novedades de Socios"

    const val CHANNEL_ID_TASK_ALARMS = "vr_agro_task_alarms_popup_channel"
    private const val CHANNEL_NAME_TASK_ALARMS = "Alarmas y Avisos de Tareas"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Ensure previous lower-priority channel is cleaned up if it existed
            try {
                notificationManager?.deleteNotificationChannel("vr_agro_socios_channel")
            } catch (e: Exception) {}

            val channelSocios = NotificationChannel(
                CHANNEL_ID_SOCIOS,
                CHANNEL_NAME_SOCIOS,
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH is required for Heads-Up mobile pop-up banner
            ).apply {
                description = "Avisos pop-up de nuevas tareas, actividades terminadas, partes y huertos registrados por otros socios"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val channelAlarms = NotificationChannel(
                CHANNEL_ID_TASK_ALARMS,
                CHANNEL_NAME_TASK_ALARMS,
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH for Heads-Up banner pop-up
            ).apply {
                description = "Avisos emergentes pop-up y alarmas de tareas programadas pendientes en huertos y calendario"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 200, 450, 200, 450)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager?.createNotificationChannel(channelSocios)
            notificationManager?.createNotificationChannel(channelAlarms)
        }
    }

    /**
     * Devuelve un resumen legible y representativo del tipo de acción para el pop-up
     */
    fun getActionSummary(post: SocialPostEntity): String {
        val orchard = post.orchardName.trim()
        val hasOrchard = orchard.isNotBlank() && orchard != "General"
        val oTag = if (hasOrchard) "$orchard: " else ""

        return when {
            // Costes
            (post.content.contains("Resumen Semanal", ignoreCase = true) || post.authorName.contains("Resumen Semanal", ignoreCase = true) || (post.alertTitle?.contains("Resumen Semanal", ignoreCase = true) == true)) && (post.content.contains("V&R", ignoreCase = true) || post.content.contains("VYR", ignoreCase = true) || post.authorName.contains("V&R", ignoreCase = true) || (post.alertTitle?.contains("V&R", ignoreCase = true) == true) || (post.alertTitle?.contains("VYR", ignoreCase = true) == true)) -> "📊 Balance semanal de gastos y labores consolidado V&R CB"
            post.content.contains("Resumen Semanal", ignoreCase = true) || post.authorName.contains("Resumen Semanal", ignoreCase = true) || (post.alertTitle?.contains("Resumen Semanal", ignoreCase = true) == true) -> {
                val title = post.alertTitle
                if (title != null && title.startsWith("📊 Resumen Semanal de Costes")) "📊 Balance semanal de gastos y labores consolidado ${title.removePrefix("📊 Resumen Semanal de Costes").trim()}"
                else "📊 Balance semanal de gastos y labores consolidado"
            }
            (post.content.contains("Balance Mensual", ignoreCase = true) || post.content.contains("Resumen Mensual", ignoreCase = true) || post.authorName.contains("Resumen Mensual", ignoreCase = true) || (post.alertTitle?.contains("Balance Mensual", ignoreCase = true) == true)) && (post.content.contains("V&R", ignoreCase = true) || post.content.contains("VYR", ignoreCase = true) || post.authorName.contains("V&R", ignoreCase = true) || (post.alertTitle?.contains("V&R", ignoreCase = true) == true) || (post.alertTitle?.contains("VYR", ignoreCase = true) == true)) -> "📈 Balance mensual de costes y rendimiento consolidado V&R CB"
            post.content.contains("Balance Mensual", ignoreCase = true) || post.content.contains("Resumen Mensual", ignoreCase = true) || post.authorName.contains("Resumen Mensual", ignoreCase = true) || (post.alertTitle?.contains("Balance Mensual", ignoreCase = true) == true) -> {
                val title = post.alertTitle
                if (title != null && title.startsWith("📈 Balance Mensual de Costes")) "📈 Balance mensual de costes y rendimiento consolidado ${title.removePrefix("📈 Balance Mensual de Costes").trim()}"
                else "📈 Balance mensual de costes y rendimiento consolidado"
            }

            // Heladas
            post.content.contains("ALERTA EN TIEMPO REAL", ignoreCase = true) && (post.content.contains("Helada", ignoreCase = true) || post.authorName.contains("Helada", ignoreCase = true)) -> "❄️⚠️ ¡Alerta Inmediata! ${oTag}Temperatura límite alcanzada"
            (post.content.contains("AVISO A 48 HORAS", ignoreCase = true) || post.content.contains("2 Días", ignoreCase = true) || post.authorName.contains("48 Horas", ignoreCase = true) || post.authorName.contains("2 Días", ignoreCase = true)) && (post.content.contains("Helada", ignoreCase = true) || post.authorName.contains("Helada", ignoreCase = true)) -> "❄️🚨 Previsión 48h en ${oTag}Alta fiabilidad de helada"
            (post.content.contains("AVISO PREVENTIVO A 7 DÍAS", ignoreCase = true) || post.authorName.contains("7 Días", ignoreCase = true)) && (post.content.contains("Helada", ignoreCase = true) || post.authorName.contains("Helada", ignoreCase = true)) -> "❄️📅 Previsión 7 días en ${oTag}Riesgo de helada detectado"

            // Viento Fuerte
            post.content.contains("ALERTA EN TIEMPO REAL", ignoreCase = true) && (post.content.contains("Viento", ignoreCase = true) || post.authorName.contains("Viento", ignoreCase = true)) -> "💨⚠️ ¡Alerta Inmediata! ${oTag}Rachas en umbral fijado"
            (post.content.contains("48 HORAS", ignoreCase = true) || post.content.contains("2 Días", ignoreCase = true) || post.authorName.contains("48 Horas", ignoreCase = true) || post.authorName.contains("2 Días", ignoreCase = true)) && (post.content.contains("Viento", ignoreCase = true) || post.authorName.contains("Viento", ignoreCase = true)) -> "💨🚨 Previsión 48h en ${oTag}Viento fuerte inminente"
            (post.content.contains("7 DÍAS", ignoreCase = true) || post.authorName.contains("7 Días", ignoreCase = true)) && (post.content.contains("Viento", ignoreCase = true) || post.authorName.contains("Viento", ignoreCase = true)) -> "💨📅 Previsión 7 días en ${oTag}Riesgo de viento fuerte"

            // Ola de Calor
            post.content.contains("ALERTA EN TIEMPO REAL", ignoreCase = true) && (post.content.contains("Calor", ignoreCase = true) || post.authorName.contains("Calor", ignoreCase = true)) -> "☀️⚠️ ¡Alerta Inmediata! ${oTag}Temperatura máxima alcanzada"
            (post.content.contains("48 HORAS", ignoreCase = true) || post.content.contains("2 Días", ignoreCase = true) || post.authorName.contains("48 Horas", ignoreCase = true) || post.authorName.contains("2 Días", ignoreCase = true)) && (post.content.contains("Calor", ignoreCase = true) || post.authorName.contains("Calor", ignoreCase = true)) -> "☀️🚨 Previsión 48h en ${oTag}Ola de calor inminente"
            (post.content.contains("7 DÍAS", ignoreCase = true) || post.authorName.contains("7 Días", ignoreCase = true)) && (post.content.contains("Calor", ignoreCase = true) || post.authorName.contains("Calor", ignoreCase = true)) -> "☀️📅 Previsión 7 días en ${oTag}Riesgo de ola de calor"

            // Lluvia
            post.content.contains("ALERTA EN TIEMPO REAL", ignoreCase = true) && (post.content.contains("Lluvia", ignoreCase = true) || post.authorName.contains("Lluvia", ignoreCase = true)) -> "🌧️⚠️ ¡Alerta Inmediata! ${oTag}Umbral de lluvia alcanzado"
            (post.content.contains("48 HORAS", ignoreCase = true) || post.content.contains("2 Días", ignoreCase = true) || post.authorName.contains("48 Horas", ignoreCase = true) || post.authorName.contains("2 Días", ignoreCase = true)) && (post.content.contains("Lluvia", ignoreCase = true) || post.authorName.contains("Lluvia", ignoreCase = true)) -> "🌧️🚨 Previsión 48h en ${oTag}Lluvias inminentes"
            (post.content.contains("7 DÍAS", ignoreCase = true) || post.authorName.contains("7 Días", ignoreCase = true)) && (post.content.contains("Lluvia", ignoreCase = true) || post.authorName.contains("Lluvia", ignoreCase = true)) -> "🌧️📅 Previsión 7 días en ${oTag}Probabilidad de lluvias"
            post.content.contains("Peligro de lluvia", ignoreCase = true) || post.content.contains("Lluvia", ignoreCase = true) || post.authorName.contains("Lluvia", ignoreCase = true) -> "🌧️ ${oTag}Alerta por previsión de lluvias"

            // Granizo
            post.content.contains("ALERTA EN TIEMPO REAL", ignoreCase = true) && (post.content.contains("Granizo", ignoreCase = true) || post.authorName.contains("Granizo", ignoreCase = true)) -> "⛈️⚠️ ¡Alerta Inmediata! ${oTag}Tormenta de granizo en curso"
            (post.content.contains("48 HORAS", ignoreCase = true) || post.content.contains("2 Días", ignoreCase = true) || post.authorName.contains("48 Horas", ignoreCase = true) || post.authorName.contains("2 Días", ignoreCase = true)) && (post.content.contains("Granizo", ignoreCase = true) || post.authorName.contains("Granizo", ignoreCase = true)) -> "⛈️🚨 Previsión 48h en ${oTag}Riesgo inminente de granizo"
            (post.content.contains("7 DÍAS", ignoreCase = true) || post.authorName.contains("7 Días", ignoreCase = true)) && (post.content.contains("Granizo", ignoreCase = true) || post.authorName.contains("Granizo", ignoreCase = true)) -> "⛈️📅 Previsión 7 días en ${oTag}Probabilidad de granizo"

            post.content.contains("Nueva tarea", ignoreCase = true) -> "📅 Hay una nueva tarea en la pantalla de socios"
            post.content.contains("Tarea completada", ignoreCase = true) || post.content.contains("completó", ignoreCase = true) || post.content.contains("finalizada", ignoreCase = true) -> "Se finalizó una actividad en la pantalla de socios"
            post.content.contains("Tarea reabierta", ignoreCase = true) -> "🔄 Se ha reabierto una tarea en socios"
            post.content.contains("Parte de Trabajo", ignoreCase = true) || post.content.contains("Parte de Campo", ignoreCase = true) -> "🚜 Nuevo parte de campo registrado en socios"
            post.content.contains("Parte de Producción", ignoreCase = true) || post.content.contains("Cosecha", ignoreCase = true) -> "🍊 Nuevo parte de producción en socios"
            post.content.contains("Gasto de Riego", ignoreCase = true) || post.content.contains("horario de riego", ignoreCase = true) -> "💧 Actualización de riego en socios"
            post.content.contains("Nueva parcela", ignoreCase = true) || post.content.contains("Nuevo Huerto", ignoreCase = true) || post.content.contains("alta", ignoreCase = true) -> "🌱 Se ha dado de alta un nuevo huerto en socios"
            post.content.contains("Huerto actualizado", ignoreCase = true) || post.content.contains("Huerto Modificado", ignoreCase = true) || post.content.contains("modificó", ignoreCase = true) || post.content.contains("modificado", ignoreCase = true) -> "✏️ Se ha modificado un huerto en socios"
            post.content.contains("Huerto eliminado", ignoreCase = true) -> "🗑️ Se ha dado de baja un huerto en socios"
            post.content.contains("Actividad eliminada", ignoreCase = true) -> "🗑️ Se ha eliminado una actividad en socios"
            post.content.contains("Tarea cancelada", ignoreCase = true) -> "🗑️ Se ha cancelado una tarea en socios"
            post.content.contains("foto de parcela", ignoreCase = true) -> "📸 Nueva foto de huerto publicada en socios"
            post.content.contains("Lonja", ignoreCase = true) || post.authorName.contains("Lonja", ignoreCase = true) -> {
                val firstPrice = post.content.lines().map { it.trim() }.firstOrNull { it.startsWith("•") }?.removePrefix("• ")
                if (!firstPrice.isNullOrBlank()) {
                    "📊 ${firstPrice.replace("(= (", "(=").replace("%))", "%)")}"
                } else {
                    "📊 Cotizaciones oficiales de lonja"
                }
            }
            post.isAlert -> post.alertTitle ?: "⚠️ Alerta prioritaria en el muro de socios"
            else -> {
                val clean = post.content.replace('\n', ' ').trim()
                if (clean.length > 70) clean.take(67).trimEnd() + "…" else clean
            }
        }
    }

    fun getActionIconType(post: SocialPostEntity): String {
        val isVyr = post.content.contains("V&R", ignoreCase = true) || post.content.contains("VYR", ignoreCase = true) || post.authorName.contains("V&R", ignoreCase = true) || (post.alertTitle?.contains("V&R", ignoreCase = true) == true)
        return when {
            post.content.contains("Resumen Semanal", ignoreCase = true) || (post.alertTitle?.contains("Resumen Semanal", ignoreCase = true) == true) -> if (isVyr) "weekly_costs_vyr" else "weekly_costs"
            post.content.contains("Balance Mensual", ignoreCase = true) || post.content.contains("Resumen Mensual", ignoreCase = true) || (post.alertTitle?.contains("Balance Mensual", ignoreCase = true) == true) -> if (isVyr) "monthly_costs_vyr" else "monthly_costs"
            post.content.contains("Helada", ignoreCase = true) || post.authorName.contains("Helada", ignoreCase = true) -> "frost"
            post.content.contains("Granizo", ignoreCase = true) || post.authorName.contains("Granizo", ignoreCase = true) -> "hail"
            post.content.contains("Lluvia", ignoreCase = true) || post.authorName.contains("Lluvia", ignoreCase = true) -> "rain"
            post.content.contains("Viento", ignoreCase = true) || post.authorName.contains("Viento", ignoreCase = true) -> "wind"
            post.content.contains("Calor", ignoreCase = true) || post.authorName.contains("Calor", ignoreCase = true) -> "heat"
            post.content.contains("Nueva tarea", ignoreCase = true) -> "new_task"
            post.content.contains("Tarea completada", ignoreCase = true) || post.content.contains("completó", ignoreCase = true) -> "task_completed"
            post.content.contains("Parte", ignoreCase = true) -> "work_part"
            post.content.contains("Nueva parcela", ignoreCase = true) || post.content.contains("alta", ignoreCase = true) -> "new_orchard"
            post.content.contains("modific", ignoreCase = true) -> "orchard_modified"
            post.content.contains("Lonja", ignoreCase = true) || post.authorName.contains("Lonja", ignoreCase = true) -> "market"
            post.isAlert -> "alert"
            else -> "social"
        }
    }

    fun formatNotificationTitle(post: SocialPostEntity): String {
        val author = post.authorName.trim()
        val orchard = post.orchardName.trim()

        if (!post.alertTitle.isNullOrBlank()) {
            return post.alertTitle
        }

        val isVyr = post.content.contains("V&R", ignoreCase = true) || post.content.contains("VYR", ignoreCase = true) || author.contains("V&R", ignoreCase = true)
        if (post.content.contains("Resumen Semanal", ignoreCase = true) || author.contains("Resumen Semanal", ignoreCase = true)) {
            return if (isVyr) "📊 Resumen Semanal de Costes V&R CB" else "📊 Resumen Semanal de Costes"
        }
        if (post.content.contains("Balance Mensual", ignoreCase = true) || post.content.contains("Resumen Mensual", ignoreCase = true) || author.contains("Resumen Mensual", ignoreCase = true)) {
            return if (isVyr) "📈 Balance Mensual de Costes V&R CB" else "📈 Balance Mensual de Costes"
        }

        val isLonja = post.content.contains("Lonja", ignoreCase = true) ||
                author.contains("Lonja", ignoreCase = true) ||
                orchard == "Mercado y Lonjas"

        if (isLonja) {
            return "📊 Precios Lonja de Cítricos"
        }

        val content = post.content
        val isFrost = content.contains("Helada", ignoreCase = true) || author.contains("Helada", ignoreCase = true)
        val isWind = content.contains("Viento", ignoreCase = true) || author.contains("Viento", ignoreCase = true)
        val isHeat = content.contains("Calor", ignoreCase = true) || author.contains("Calor", ignoreCase = true)
        val isRain = content.contains("Lluvia", ignoreCase = true) || author.contains("Lluvia", ignoreCase = true)
        val isHail = content.contains("Granizo", ignoreCase = true) || author.contains("Granizo", ignoreCase = true)

        val is48h = content.contains("48 HORAS", ignoreCase = true) || content.contains("48h", ignoreCase = true) ||
                content.contains("2 Días", ignoreCase = true) || content.contains("2 días", ignoreCase = true) ||
                author.contains("48 Horas", ignoreCase = true) || author.contains("2 Días", ignoreCase = true)
        val is7d = content.contains("7 DÍAS", ignoreCase = true) || content.contains("7 días", ignoreCase = true) ||
                author.contains("7 Días", ignoreCase = true)
        val isRealtime = content.contains("TIEMPO REAL", ignoreCase = true) || content.contains("Tiempo Real", ignoreCase = true) ||
                content.contains("Sensor actual", ignoreCase = true) || content.contains("ahora mismo", ignoreCase = true) ||
                content.contains("alcanzado los", ignoreCase = true)

        val hasOrchard = orchard.isNotBlank() && orchard != "General"
        val oLabel = if (hasOrchard) " • $orchard" else ""

        return when {
            isFrost -> when {
                is48h -> if (hasOrchard) "❄️ $orchard • Helada 48h" else "❄️ Helada a 48h"
                is7d -> if (hasOrchard) "❄️ $orchard • Helada 7 días" else "❄️ Helada a 7 días"
                isRealtime -> if (hasOrchard) "❄️ $orchard • Helada ahora" else "❄️ Helada ahora"
                else -> if (hasOrchard) "❄️ $orchard • Helada" else "❄️ Helada"
            }
            isWind -> when {
                is48h -> if (hasOrchard) "💨 $orchard • Viento 48h" else "💨 Viento a 48h"
                is7d -> if (hasOrchard) "💨 $orchard • Viento 7 días" else "💨 Viento a 7 días"
                isRealtime -> if (hasOrchard) "💨 $orchard • Viento fuerte" else "💨 Viento fuerte"
                else -> if (hasOrchard) "💨 $orchard • Viento" else "💨 Viento"
            }
            isHeat -> when {
                is48h -> if (hasOrchard) "☀️ $orchard • Calor 48h" else "☀️ Calor a 48h"
                is7d -> if (hasOrchard) "☀️ $orchard • Calor 7 días" else "☀️ Calor a 7 días"
                isRealtime -> if (hasOrchard) "☀️ $orchard • Ola de calor" else "☀️ Ola de calor"
                else -> if (hasOrchard) "☀️ $orchard • Calor" else "☀️ Calor"
            }
            isRain -> when {
                is48h -> if (hasOrchard) "🌧️ $orchard • Lluvia 48h" else "🌧️ Lluvia a 48h"
                is7d -> if (hasOrchard) "🌧️ $orchard • Lluvia 7 días" else "🌧️ Lluvia a 7 días"
                isRealtime -> if (hasOrchard) "🌧️ $orchard • Lluvia ahora" else "🌧️ Lluvia ahora"
                else -> if (hasOrchard) "🌧️ $orchard • Peligro lluvia" else "🌧️ Peligro de lluvia"
            }
            isHail -> when {
                is48h -> if (hasOrchard) "⛈️ $orchard • Granizo 48h" else "⛈️ Granizo a 48h"
                is7d -> if (hasOrchard) "⛈️ $orchard • Granizo 7 días" else "⛈️ Granizo a 7 días"
                isRealtime -> if (hasOrchard) "⛈️ $orchard • Granizo ahora" else "⛈️ Granizo ahora"
                else -> if (hasOrchard) "⛈️ $orchard • Granizo" else "⛈️ Granizo"
            }
            content.contains("Tarea completada", ignoreCase = true) || content.contains("completó", ignoreCase = true) || content.contains("terminada", ignoreCase = true) || content.contains("finalizada", ignoreCase = true) ->
                if (hasOrchard) "✅ $orchard • Tarea finalizada" else "Tarea finalizada"
            content.contains("Nueva tarea", ignoreCase = true) ->
                if (hasOrchard) "📅 $orchard • Nueva tarea" else "📅 Nueva tarea"
            content.contains("Tarea reabierta", ignoreCase = true) ->
                if (hasOrchard) "🔄 $orchard • Tarea reabierta" else "🔄 Tarea reabierta"
            content.contains("Tarea cancelada", ignoreCase = true) ->
                if (hasOrchard) "🗑️ $orchard • Tarea cancelada" else "🗑️ Tarea cancelada"
            content.contains("Parte de Trabajo", ignoreCase = true) || content.contains("Parte de Campo", ignoreCase = true) || content.contains("Parte", ignoreCase = true) ->
                if (hasOrchard) "🚜 $orchard • Parte de campo" else "🚜 Parte de campo"
            content.contains("Nueva parcela", ignoreCase = true) || content.contains("Nuevo Huerto", ignoreCase = true) ->
                "🌱 Nuevo huerto$oLabel"
            content.contains("Huerto actualizado", ignoreCase = true) || content.contains("modificó", ignoreCase = true) ->
                "✏️ Huerto actualizado$oLabel"
            content.contains("foto de parcela", ignoreCase = true) || content.contains("foto", ignoreCase = true) ->
                if (hasOrchard) "📸 $orchard • Foto parcela" else "📸 Foto de parcela"
            post.isAlert && !post.alertTitle.isNullOrBlank() -> {
                val clean = post.alertTitle
                    .replace(Regex("\\s*\\([^)]*\\)"), "")
                    .trim()
                if (hasOrchard && !clean.contains(orchard, ignoreCase = true)) {
                    "⚠️ $orchard • ${clean.take(18)}"
                } else if (clean.length > 28) {
                    clean.take(28)
                } else {
                    clean
                }
            }
            hasOrchard ->
                "📢 $orchard • $author"
            else ->
                "📢 Socio: $author"
        }
    }

    fun simplifyPostContent(post: SocialPostEntity): String {
        val orchard = post.orchardName.trim()
        val hasOrchard = orchard.isNotBlank() && orchard != "General"
        val orchardBullet = if (hasOrchard) "• Huerto: $orchard" else null

        val raw = post.content
            .replace(Regex("👉\\s*Toca para abrir[^.\n]*[.\n]?"), "")
            .replace("Consulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
            .replace("\n\nConsulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
            .trim()

        val isCostSummary = raw.contains("Gasto consolidado", ignoreCase = true) ||
                raw.contains("Inversión total", ignoreCase = true) ||
                raw.contains("Resumen Semanal", ignoreCase = true) ||
                raw.contains("Balance Mensual", ignoreCase = true) ||
                post.authorName.contains("Costes", ignoreCase = true) ||
                (post.alertTitle?.contains("Costes", ignoreCase = true) == true)

        val isLonja = raw.contains("Lonja", ignoreCase = true) ||
                post.authorName.contains("Lonja", ignoreCase = true) ||
                post.orchardName == "Mercado y Lonjas"

        if (isLonja) {
            val bulletLines = raw.lines().map { it.trim() }.filter { it.startsWith("•") }
            return if (bulletLines.isNotEmpty()) {
                bulletLines.joinToString("\n") { it.replace("(= (", "(=").replace("%))", "%)") }
            } else {
                raw.replace(Regex("📊 NUEVA COTIZACIÓN[^\n]*\n?"), "")
                    .replace(Regex("📅 Semana[^\n]*\n?"), "")
                    .replace("Cotizaciones oficiales para tus huertos:\n", "")
                    .replace("Cotizaciones oficiales para tus huertos:", "")
                    .replace("Precios de referencia oficiales destacados:\n", "")
                    .replace("Precios de referencia oficiales destacados:", "")
                    .trim()
            }
        }

        // Si ya viene formateado con viñetas limpias, devolverlo eliminando cualquier línea de medidas o pies de consulta
        val existingBullets = raw.lines().map { it.trim() }.filter { it.startsWith("•") }
        if (existingBullets.isNotEmpty()) {
            val filtered = existingBullets.filterNot { 
                it.startsWith("• Medidas:", ignoreCase = true) || 
                it.startsWith("• Consulta", ignoreCase = true)
            }
            if (filtered.isNotEmpty()) {
                if (isCostSummary) {
                    return filtered.joinToString("\n")
                }
                val hasOrchardInBullets = filtered.any { it.startsWith("• Huerto:", ignoreCase = true) || it.startsWith("• Parcela:", ignoreCase = true) }
                return if (!hasOrchardInBullets && orchardBullet != null) {
                    "$orchardBullet\n" + filtered.joinToString("\n")
                } else {
                    filtered.joinToString("\n")
                }
            }
        }

        // Si es una alerta climática sin viñetas (formato previo o legacy):
        if (post.isAlert || post.authorName.contains("Alerta", ignoreCase = true)) {
            val cleaned = raw
                .replace(Regex("^[❄️🚨📅⚠️☀️🌧️⛈️💨\\s]*(AVISO[^\n:]*:|¡ALERTA[^\n:]*:)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()

            val alertBody = when {
                cleaned.contains("mínima prevista de", ignoreCase = true) || cleaned.contains("riesgo inminente de helada", ignoreCase = true) || cleaned.contains("riesgo de helada", ignoreCase = true) -> {
                    val temp = Regex("mínima (?:prevista )?de ([^()]+?)(?:\\s*\\(|\\s*para|\\.)", RegexOption.IGNORE_CASE).find(cleaned)?.groupValues?.get(1)?.trim() ?: "0 °C"
                    val umbral = Regex("umbral(?:\\s+fijado|\\s+de aviso)?:?\\s*([^().]+)", RegexOption.IGNORE_CASE).find(cleaned)?.groupValues?.get(1)?.trim() ?: "2 °C"
                    val whenText = when {
                        cleaned.contains("pasado mañana", ignoreCase = true) || cleaned.contains("48", ignoreCase = true) -> "para pasado mañana"
                        cleaned.contains("7 días", ignoreCase = true) -> "en 7 días"
                        else -> "próximamente"
                    }
                    "• Previsión: Mínima de $temp $whenText (umbral: $umbral)"
                }
                cleaned.contains("alcanzado los", ignoreCase = true) || cleaned.contains("acaba de alcanzar", ignoreCase = true) -> {
                    val sensor = Regex("(\\d+(?:\\.\\d+)?\\s*°C)", RegexOption.IGNORE_CASE).find(cleaned)?.value ?: "baja"
                    val umbral = Regex("umbral(?:\\s+fijado)?:?\\s*(\\d+(?:\\.\\d+)?\\s*°C)", RegexOption.IGNORE_CASE).find(cleaned)?.groupValues?.get(1) ?: "2 °C"
                    "• Sensor actual: $sensor (umbral: $umbral alcanzado)"
                }
                cleaned.contains("viento", ignoreCase = true) -> {
                    val speed = Regex("(\\d+\\s*km/h)", RegexOption.IGNORE_CASE).find(cleaned)?.value ?: "fuertes"
                    "• Previsión: Rachas superiores a $speed"
                }
                cleaned.contains("calor", ignoreCase = true) || cleaned.contains("máximas", ignoreCase = true) -> {
                    val temp = Regex("(\\d+\\s*°C)", RegexOption.IGNORE_CASE).find(cleaned)?.value ?: "altas"
                    "• Previsión: Temperaturas superiores a $temp"
                }
                cleaned.contains("lluvia", ignoreCase = true) || cleaned.contains("precipitaci", ignoreCase = true) -> {
                    val prob = Regex("(\\d+\\s*%)", RegexOption.IGNORE_CASE).find(cleaned)?.value ?: "alta"
                    "• Previsión: Probabilidad de lluvia del $prob"
                }
                cleaned.contains("granizo", ignoreCase = true) || cleaned.contains("pedrisco", ignoreCase = true) -> {
                    val prob = Regex("(\\d+\\s*%)", RegexOption.IGNORE_CASE).find(cleaned)?.value ?: "alta"
                    "• Previsión: Riesgo de tormentas y granizo ($prob)"
                }
                else -> {
                    cleaned
                }
            }

            return if (orchardBullet != null && !alertBody.contains("• Huerto:", ignoreCase = true)) {
                "$orchardBullet\n$alertBody"
            } else {
                alertBody
            }
        }

        // Tarea completada / finalizada
        if (raw.contains("Tarea completada", ignoreCase = true) || raw.contains("Tarea finalizada", ignoreCase = true) || raw.contains("completó", ignoreCase = true) || raw.contains("terminada", ignoreCase = true)) {
            val taskName = Regex("'([^']+)'").find(raw)?.groupValues?.get(1)
            val author = post.authorName.ifBlank { "Socio" }
            return if (taskName != null) {
                "• Tarea: $taskName\n• Estado: Finalizada por $author"
            } else {
                raw.replace("✅", "").replace("Tarea completada:", "Tarea:").replace("Tarea finalizada:", "Tarea:").trim()
            }
        }

        // Nueva tarea
        if (raw.contains("Nueva tarea", ignoreCase = true)) {
            val taskName = Regex("'([^']+)'").find(raw)?.groupValues?.get(1)
            val date = Regex("para el día ([^()\n]+)").find(raw)?.groupValues?.get(1)?.trim()
            val hour = Regex("\\(([^)]+)\\)").find(raw)?.groupValues?.get(1)?.trim()
            return if (taskName != null) {
                buildString {
                    append("• Tarea: $taskName")
                    if (date != null) append("\n• Fecha: $date")
                    if (hour != null && hour != date) append(" ($hour)")
                }
            } else {
                raw.replace("📅 Nueva tarea programada:", "").trim()
            }
        }

        // Parte de trabajo
        if (raw.contains("Parte de Trabajo", ignoreCase = true) || raw.contains("Parte de Campo", ignoreCase = true)) {
            val taskName = Regex(":\\s*([^\n•]+)").find(raw)?.groupValues?.get(1)?.trim()
            val hours = Regex("•\\s*Horas[^:]*:\\s*([^\n]+)").find(raw)?.groupValues?.get(1)?.trim()
            val cost = Regex("•\\s*Coste[^:]*:\\s*([^\n]+)").find(raw)?.groupValues?.get(1)?.trim()
            if (taskName != null) {
                return buildString {
                    append("• Labor: $taskName")
                    if (hours != null && cost != null) append(" ($hours • $cost)")
                    append("\n• Registrado por: ${post.authorName}")
                }
            }
        }

        // Nuevo huerto
        if (raw.contains("Nueva parcela", ignoreCase = true) || raw.contains("Nuevo Huerto", ignoreCase = true)) {
            val name = Regex("'([^']+)'").find(raw)?.groupValues?.get(1) ?: post.orchardName
            val details = Regex("\\(([^)]+)\\)").find(raw)?.groupValues?.get(1)
            return buildString {
                append("• Parcela: $name")
                if (details != null) append(" ($details)")
                append("\n• Registrado por: ${post.authorName}")
            }
        }

        // Huerto modificado
        if (raw.contains("Huerto actualizado", ignoreCase = true) || raw.contains("modific", ignoreCase = true)) {
            return "• Parcela: ${post.orchardName}\n• Actualización: Datos modificados por ${post.authorName}"
        }

        return raw
    }

    /**
     * Notifica una nueva publicación o actividad en Socios siempre y cuando:
     * 1. Las notificaciones de socios estén habilitadas en ajustes (`notificationsEnabled = true`).
     * 2. El autor sea OTRA persona distinta al usuario actual (`authorName != currentUserName`).
     *
     * Si la modificación fue realizada por el propio usuario actual ("yo"), no se emite ninguna notificación
     * para cumplir estrictamente con el requisito de no auto-notificarse.
     */
    fun notifySocialActivity(
        context: Context,
        post: SocialPostEntity,
        currentUserName: String,
        notificationsEnabled: Boolean
    ): Boolean {
        if (!notificationsEnabled) return false

        val author = post.authorName.trim()
        val me = currentUserName.trim()

        // Regla estricta: NO notificar si el autor es el usuario actual ("siempre y cuando no lo modifique yo")
        if (author.equals(me, ignoreCase = true)) {
            return false
        }

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigateTo", "social")
            putExtra("postId", post.id)
            putExtra("notificationTimestamp", System.currentTimeMillis())
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (post.id % 10000).toInt(),
            intent,
            pendingIntentFlags
        )

        val isLonja = post.content.contains("Lonja", ignoreCase = true) ||
                post.authorName.contains("Lonja", ignoreCase = true) ||
                post.orchardName == "Mercado y Lonjas"

        // Para notificaciones de la Lonja, extraer directamente las líneas con precios sin preámbulos
        val lonjaBulletLines = if (isLonja) {
            post.content.lines().map { it.trim() }.filter { it.startsWith("•") }
        } else {
            emptyList()
        }

        val lonjaDirectPrices = if (lonjaBulletLines.isNotEmpty()) {
            lonjaBulletLines.joinToString("\n") { line ->
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

        val isCostSummary = post.content.contains("Gasto consolidado", ignoreCase = true) ||
                post.content.contains("Inversión total", ignoreCase = true) ||
                post.content.contains("Resumen Semanal", ignoreCase = true) ||
                post.content.contains("Balance Mensual", ignoreCase = true) ||
                post.authorName.contains("Costes", ignoreCase = true) ||
                (post.alertTitle?.contains("Costes", ignoreCase = true) == true)

        val orchard = post.orchardName.trim()
        val hasOrchard = orchard.isNotBlank() && orchard != "General" && orchard != "Balance Explotación" && orchard != "Balance"

        val title = formatNotificationTitle(post)
        val bigText = simplifyPostContent(post)
        val summaryLabel = when {
            isCostSummary -> "Control de Costes"
            isLonja -> "Cotizaciones"
            post.isAlert || post.authorName.contains("Alerta", ignoreCase = true) -> if (hasOrchard) orchard else "Alerta Climática"
            post.content.contains("Tarea", ignoreCase = true) -> if (hasOrchard) orchard else "Tareas"
            post.content.contains("Parte", ignoreCase = true) -> if (hasOrchard) orchard else "Partes de Campo"
            else -> if (hasOrchard) orchard else "Socios"
        }

        val shortSummary = if (isLonja) {
            val firstLine = lonjaBulletLines.firstOrNull()?.removePrefix("• ")
            if (!firstLine.isNullOrBlank()) {
                firstLine.replace("(= (", "(=").replace("%))", "%)")
            } else {
                lonjaDirectPrices.lines().firstOrNull()?.removePrefix("• ") ?: "Precios actualizados"
            }
        } else if (isCostSummary) {
            val firstBullet = bigText.lines().firstOrNull { it.trim().startsWith("•") }?.removePrefix("• ")?.trim()
            firstBullet ?: getActionSummary(post)
        } else if (hasOrchard && (post.isAlert || post.authorName.contains("Alerta", ignoreCase = true))) {
            val forecastLine = bigText.lines()
                .firstOrNull { line ->
                    val t = line.trim()
                    !t.startsWith("• Huerto:", ignoreCase = true) && !t.startsWith("• Parcela:", ignoreCase = true) && t.startsWith("•")
                }?.removePrefix("• ")?.trim()

            if (!forecastLine.isNullOrBlank()) {
                "$orchard • $forecastLine"
            } else {
                getActionSummary(post)
            }
        } else {
            val firstBullet = bigText.lines().firstOrNull { it.trim().startsWith("•") }?.removePrefix("• ")?.trim()
            if (!firstBullet.isNullOrBlank() && firstBullet.length <= 60) {
                firstBullet
            } else {
                getActionSummary(post)
            }
        }

        val bigStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(bigText)
            .setSummaryText(if (isCostSummary) "Costes" else if (hasOrchard) orchard else summaryLabel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SOCIOS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortSummary)
            .setStyle(bigStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // PRIORITY_HIGH for Heads-Up pop-up banner on mobile
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            val notificationId = if (post.id > 0) post.id.toInt() else (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            // Falta de permiso POST_NOTIFICATIONS en Android 13+
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Muestra la notificación flotante (Heads-Up) con sonido y vibración en el móvil
     * para una alarma de tarea pendiente programada desde el calendario o desde el huerto.
     */
    fun notifyTaskAlarm(
        context: Context,
        taskId: Long,
        taskTitle: String,
        orchardName: String,
        orchardId: Long? = null,
        timeText: String,
        dateKey: String,
        description: String = "",
        reminderDate: String = "",
        reminderTime: String = ""
    ): Boolean {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigateTo", "calendar")
            putExtra("taskId", taskId)
            putExtra("dateKey", dateKey)
            putExtra("orchardId", orchardId ?: -1L)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (taskId % 10000).toInt() + 50000,
            intent,
            pendingIntentFlags
        )

        // Acción para marcar completada directamente desde el banner del móvil
        val completeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_COMPLETE_TASK_ALARM
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
            putExtra("orchardName", orchardName)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId % 10000).toInt() + 60000,
            completeIntent,
            pendingIntentFlags
        )

        val effDate = reminderDate.ifBlank { dateKey }
        val effTime = reminderTime.ifBlank { timeText.ifBlank { "08:00" } }
        val effDateDisplay = formatDateToDisplay(effDate)
        val dateKeyDisplay = formatDateToDisplay(dateKey)

        val title = "⏰ $taskTitle • $orchardName"
        val contentText = "$effDateDisplay a las $effTime • $orchardName"

        val detailsText = buildString {
            append("• Huerto: $orchardName\n")
            append("• Hora: $timeText ($dateKeyDisplay)")
            if (effTime != timeText || effDate != dateKey) {
                append("\n• Aviso: $effDateDisplay a las $effTime")
            }
            if (description.isNotBlank()) {
                append("\n• Observaciones: $description")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK_ALARMS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(detailsText)
                    .setSummaryText("Alarma de Tarea")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX) // Heads-Up pop-up
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 450, 200, 450, 200, 450))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.mipmap.ic_launcher, "Finalizar Tarea", completePendingIntent)
            .build()

        return try {
            val notificationId = (taskId % 100000).toInt() + 30000
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}

data class InAppNotificationData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val authorName: String,
    val orchardName: String? = null,
    val fullContent: String = "",
    val iconType: String = "social",
    val timestamp: Long = System.currentTimeMillis()
)
