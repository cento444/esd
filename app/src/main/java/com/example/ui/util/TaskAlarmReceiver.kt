package com.example.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_ALARM = "com.example.ACTION_TASK_ALARM"
        const val ACTION_COMPLETE_TASK_ALARM = "com.example.ACTION_COMPLETE_TASK_ALARM"
        private const val TAG = "TaskAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        Log.d(TAG, "onReceive action: $action")

        when (action) {
            ACTION_TASK_ALARM -> {
                val taskId = intent.getLongExtra("taskId", -1L)
                val taskTitle = intent.getStringExtra("taskTitle") ?: "Tarea agrícola"
                val orchardName = intent.getStringExtra("orchardName") ?: "Huerto"
                val orchardId = intent.getLongExtra("orchardId", -1L).takeIf { it > 0 }
                val timeText = intent.getStringExtra("timeText") ?: "08:00"
                val dateKey = intent.getStringExtra("dateKey") ?: ""
                val description = intent.getStringExtra("description") ?: ""
                val reminderDate = intent.getStringExtra("reminderDate") ?: ""
                val reminderTime = intent.getStringExtra("reminderTime") ?: ""

                if (taskId <= 0) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val task = db.calendarTaskDao().getTaskById(taskId)
                        // Validar que la tarea siga existiendo, no esté completada y tenga la alarma activa
                        if (task != null && !task.isDone && task.reminderEnabled) {
                            val showed = AppNotificationHelper.notifyTaskAlarm(
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
                            Log.d(TAG, "Task alarm notification shown for task $taskId: $showed")
                        } else {
                            Log.d(TAG, "Task $taskId is already done or reminder disabled, skipping notification.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in TaskAlarmReceiver for task $taskId", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_COMPLETE_TASK_ALARM -> {
                val taskId = intent.getLongExtra("taskId", -1L)
                val taskTitle = intent.getStringExtra("taskTitle") ?: "Tarea"
                if (taskId <= 0) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        db.calendarTaskDao().setTaskDone(taskId, true, System.currentTimeMillis())
                        TaskAlarmScheduler.cancelTaskAlarm(context, taskId)
                        val notificationId = (taskId % 100000).toInt() + 30000
                        NotificationManagerCompat.from(context).cancel(notificationId)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Tarea completada: $taskTitle", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error completing task from notification action", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        TaskAlarmScheduler.rescheduleAllActiveAlarms(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescheduling alarms on boot", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
