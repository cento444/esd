package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orchards")
data class OrchardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerType: String = "propios", // "propios", "v_y_r_cb", "otros"
    val ownerName: String = "",
    val variety: String = "Clemenules",
    val rootstock: String = "",
    val fruitType: String = "Cítricos", // "Cítricos", "Aguacate", "Naranjo"
    val locationGps: String = "",
    val municipality: String = "",
    val partida: String = "",
    val polygon: String = "",
    val parcel: String = "",
    val hanegadas: Double = 0.0,
    val plantingYear: Int = 0,
    val pozo: String = "",
    val sector: String = "",
    val hidrante: String = "",
    val regadorName: String = "",
    val regadorPhone: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

@Entity(tableName = "work_parts")
data class WorkPartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orchardId: Long,
    val orchardName: String,
    val type: String, // "tareas", "goteo", "produccion"
    val taskName: String,
    val hours: Double = 0.0,
    val pricePerHour: Double = 0.0,
    val totalCost: Double = 0.0,
    val observations: String = "",
    val materialsJson: String = "", // JSON list of materials
    val photoUri: String? = null,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val isMonthlyRepeat: Boolean = false,
    val kilos: Double = 0.0,
    val pricePerKg: Double = 0.0,
    val kilosDestrio: Double = 0.0,
    val precioDestrio: Double = 0.0,
    val indemnizacionSeguro: Double = 0.0,
    val ownerCategory: String = "Mío" // "Mío", "V&R", "Otros"
)

data class MaterialItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val isAdvancedByMe: Boolean = false
) {
    val totalCost: Double get() = quantity * unitPrice
}

object MaterialsJsonHelper {
    fun toJson(items: List<MaterialItem>): String {
        if (items.isEmpty()) return ""
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject()
            obj.put("name", item.name)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("isAdvancedByMe", item.isAdvancedByMe)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<MaterialItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<MaterialItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MaterialItem(
                        name = obj.optString("name", ""),
                        quantity = obj.optDouble("quantity", 0.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        isAdvancedByMe = obj.optBoolean("isAdvancedByMe", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Entity(tableName = "social_posts")
data class SocialPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orchardId: Long? = null,
    val authorName: String = "Carlos Vicente",
    val authorAvatar: String? = null,
    val orchardName: String = "Finca Norte",
    val timestampText: String = "Hace un momento",
    val timestamp: Long = System.currentTimeMillis(),
    val content: String,
    val photoUri: String? = null,
    val isAlert: Boolean = false,
    val alertTitle: String? = null,
    val isAlertResolved: Boolean = false,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false
)

@Entity(
    tableName = "post_comments",
    indices = [androidx.room.Index(value = ["postId"])]
)
data class PostCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorName: String = "Carlos Vicente",
    val authorAvatar: String? = null,
    val content: String,
    val timestampText: String = "Hace un momento",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_tasks")
data class CalendarTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orchardId: Long? = null,
    val orchardName: String = "Huerto",
    val fruitType: String = "orange", // "orange" (cítricos), "avocado" (aguacate)
    val dateKey: String, // "YYYY-MM-DD" e.g. "2026-09-01"
    val timeText: String = "08:00",
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val completedAt: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderDate: String = "",
    val reminderTime: String = ""
)

@Entity(tableName = "custom_reminders")
data class CustomReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateText: String, // e.g. "30 Ago" or "2023-08-30"
    val timeText: String, // e.g. "10:00"
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val id: Int = 1,
    // Heladas
    val frostAlert: Boolean = true,
    val frostThresholdTemp: String = "2",
    val frostNotify7Days: Boolean = true,
    val frostNotify2Days: Boolean = true,
    val frostNotifyRealtime: Boolean = true,

    // Viento Fuerte
    val windAlert: Boolean = true,
    val windThresholdKmh: String = "45",
    val windNotify7Days: Boolean = true,
    val windNotify2Days: Boolean = true,
    val windNotifyRealtime: Boolean = true,

    // Olas de Calor
    val heatAlert: Boolean = true,
    val heatThresholdTemp: String = "36",
    val heatNotify7Days: Boolean = true,
    val heatNotify2Days: Boolean = true,
    val heatNotifyRealtime: Boolean = true,

    // Lluvia
    val rainAlert: Boolean = true,
    val rainProbabilityThreshold: String = "60",
    val rainNotify7Days: Boolean = true,
    val rainNotify2Days: Boolean = true,
    val rainNotifyRealtime: Boolean = true,

    // Granizo
    val hailAlert: Boolean = true,
    val hailProbabilityThreshold: String = "40",
    val hailNotify7Days: Boolean = true,
    val hailNotify2Days: Boolean = true,
    val hailNotifyRealtime: Boolean = true,

    val irrigationReminder: Boolean = true,
    val irrigationAdvanceTime: String = "30 min antes",
    val phytosanitaryReminder: Boolean = true,
    val phytosanitaryAdvanceTime: String = "24h antes",
    val weeklyCostSummary: Boolean = true,
    val monthlyCostSummary: Boolean = true,
    val weeklyCostSummaryVyr: Boolean = true,
    val monthlyCostSummaryVyr: Boolean = true,
    val weeklyCostSummaryOtros: Boolean = true,
    val monthlyCostSummaryOtros: Boolean = true,
    val newSocialPosts: Boolean = true
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Carlos Vicente",
    val role: String = "Management / Admin",
    val photoUri: String? = null,
    val isDarkMode: Boolean = false,
    val titularPropios: String = "Carlos Vicente",
    val titularVr: String = "V&R C.B.",
    val titularOtros: String = "Otros"
)

@Entity(tableName = "irrigation_schedules")
data class IrrigationScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orchardId: Long,
    val dayOfWeek: String, // "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    val startTime: String = "08:00",
    val endTime: String = "10:00",
    val isEnabled: Boolean = true
)
