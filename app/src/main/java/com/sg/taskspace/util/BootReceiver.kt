package com.sg.taskspace.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sg.taskspace.TaskSpaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val app = context.applicationContext as? TaskSpaceApplication ?: return
            val taskRepo = app.container.taskRepository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allTasks = taskRepo.getAllTasksSync()
                    val now = System.currentTimeMillis()

                    allTasks.filter { !it.isCompleted && !it.isDeleted && !it.time.isNullOrBlank() }.forEach { task ->
                        try {
                            val taskDate = LocalDate.parse(task.createdForDate, DateTimeFormatter.ISO_DATE)
                            val taskTime = DateTimeUtils.parseLocalTime(task.time) ?: return@forEach
                            val triggerMillis = LocalDateTime.of(taskDate, taskTime)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()

                            if (triggerMillis > now) {
                                NotificationHelper.scheduleReminder(
                                    context = context,
                                    id = task.id,
                                    title = "Task Reminder: ${task.title}",
                                    notes = task.notes ?: "Scheduled for ${task.time}",
                                    triggerAtMillis = triggerMillis
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    NotificationHelper.updateDailyDashboardNotification(context)
                    NotificationHelper.scheduleDailyEveningReminder(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
