package com.example.ui.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.model.CalendarTaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskAlarmScheduler {

    private const val TAG = "TaskAlarmScheduler"

    /**
     * Programa la alarma en AlarmManager para una tarea del calendario o del huerto.
     */
    fun scheduleTaskAlarm(context: Context, task: CalendarTaskEntity) {
        if (!task.reminderEnabled || task.isDone) {
            cancelTaskAlarm(context, task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val effectiveDate = task.reminderDate.ifBlank { task.dateKey }.trim()
        val effectiveTime = task.reminderTime.ifBlank { task.timeText.ifBlank { "08:00" } }.trim()

        val triggerTime = computeTriggerTimeMillis(effectiveDate, effectiveTime)
        val now = System.currentTimeMillis()

        // Si la fecha y hora calculada ya pasaron hace más de 15 minutos de un día anterior, no programar
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
        val isForToday = effectiveDate == todayKey

        val finalTriggerTime = if (triggerTime <= now) {
            if (isForToday && (now - triggerTime) < 15 * 60 * 1000L) {
                // Programada para hoy y es la hora actual o hace unos minutos: disparar en 2 segundos
                now + 2000L
            } else if (triggerTime < now) {
                // Tarea de un día pasado: no programar
                Log.d(TAG, "Task ${task.id} alarm is in the past ($effectiveDate $effectiveTime), skipping.")
                return
            } else {
                now + 2000L
            }
        } else {
            triggerTime
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_ALARM
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("orchardName", task.orchardName)
            putExtra("orchardId", task.orchardId ?: -1L)
            putExtra("timeText", task.timeText)
            putExtra("dateKey", task.dateKey)
            putExtra("description", task.description)
            putExtra("reminderDate", effectiveDate)
            putExtra("reminderTime", effectiveTime)
        }

        val requestCode = getAlarmRequestCode(task.id)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm for task ${task.id} (${task.title}) at $effectiveDate $effectiveTime (in ${(finalTriggerTime - now) / 1000}s)")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed fallback alarm scheduling", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling task alarm", e)
        }
    }

    /**
     * Cancela la alarma programada en AlarmManager.
     */
    fun cancelTaskAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_ALARM
        }
        val requestCode = getAlarmRequestCode(taskId)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled alarm for task $taskId")
        }
    }

    /**
     * Dispara inmediatamente la alarma para pruebas o cuando se activa en el momento exacto.
     */
    fun triggerImmediateTaskAlarm(context: Context, task: CalendarTaskEntity): Boolean {
        return AppNotificationHelper.notifyTaskAlarm(
            context = context,
            taskId = task.id,
            taskTitle = task.title,
            orchardName = task.orchardName,
            orchardId = task.orchardId,
            timeText = task.timeText,
            dateKey = task.dateKey,
            description = task.description,
            reminderDate = task.reminderDate,
            reminderTime = task.reminderTime
        )
    }

    /**
     * Re-programa todas las tareas activas pendientes que tengan alarma activada.
     */
    suspend fun rescheduleAllActiveAlarms(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val pendingTasks = db.calendarTaskDao().getPendingTasksWithReminders()
            for (task in pendingTasks) {
                scheduleTaskAlarm(context, task)
            }
            Log.d(TAG, "Rescheduled ${pendingTasks.size} active task alarms")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling active task alarms", e)
        }
    }

    private fun getAlarmRequestCode(taskId: Long): Int {
        return (taskId % 50000).toInt() + 10000
    }

    private fun computeTriggerTimeMillis(dateKey: String, timeText: String): Long {
        return try {
            val cal = Calendar.getInstance()
            val dateParts = dateKey.split("-")
            val timeParts = timeText.split(":")
            if (dateParts.size == 3 && timeParts.size >= 2) {
                cal.set(Calendar.YEAR, dateParts[0].toInt())
                cal.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                cal.set(Calendar.MINUTE, timeParts[1].toInt())
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
                sdf.parse("$dateKey $timeText")?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
