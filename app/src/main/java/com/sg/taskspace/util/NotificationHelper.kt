package com.sg.taskspace.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sg.taskspace.MainActivity
import com.sg.taskspace.TaskSpaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("EXTRA_ID") ?: return
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "TaskSpace Reminder"
        val notes = intent.getStringExtra("EXTRA_NOTES") ?: "You have a scheduled reminder pending."

        NotificationHelper.showNotification(context, reminderId.hashCode(), title, notes)
    }
}

object NotificationHelper {
    const val CHANNEL_ID = "taskspace_reminders"
    const val CHANNEL_NAME = "Task Reminders"

    const val DASHBOARD_CHANNEL_ID = "taskspace_daily_dashboard"
    const val DASHBOARD_CHANNEL_NAME = "Daily Dashboard"
    const val NOTIFICATION_ID_DASHBOARD = 9999

    const val ACTION_ADD_TASK = "com.sg.taskspace.ACTION_ADD_TASK"
    const val ACTION_COMPLETE_TOP_TASK = "com.sg.taskspace.ACTION_COMPLETE_TOP_TASK"
    const val ACTION_CHECK_TOP_HABIT = "com.sg.taskspace.ACTION_CHECK_TOP_HABIT"
    const val ACTION_REFRESH_DASHBOARD = "com.sg.taskspace.ACTION_REFRESH_DASHBOARD"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Reminder channel (High importance)
            val reminderChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for TaskSpace task and project reminders"
            }
            notificationManager.createNotificationChannel(reminderChannel)

            // Daily Dashboard channel (Low importance, silent, ongoing)
            val dashboardChannel = NotificationChannel(
                DASHBOARD_CHANNEL_ID,
                DASHBOARD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent ongoing notification for today's tasks, habits, and quick actions"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(dashboardChannel)
        }
    }

    fun showNotification(context: Context, notificationId: Int, title: String, content: String) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun updateDailyDashboardNotification(context: Context) {
        val app = context.applicationContext as? TaskSpaceApplication ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPrefsRepo = app.container.userPreferencesRepository
                val prefs = userPrefsRepo.userPreferencesFlow.first()

                if (!prefs.notificationsEnabled || !prefs.persistentDailyDashboardEnabled) {
                    cancelDailyDashboardNotification(context)
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        return@launch
                    }
                }

                createNotificationChannel(context)

                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val allTasks = app.container.taskRepository.getAllTasksSync()
                val todayTasks = allTasks.filter { it.createdForDate == todayIso && !it.isDeleted }
                val activeTasks = todayTasks.filter { !it.isCompleted }
                val completedTasks = todayTasks.filter { it.isCompleted }
                val totalTasksCount = todayTasks.size
                val completedTasksCount = completedTasks.size

                val allHabits = app.container.habitRepository.getAllHabitsSync().filter { !it.isDeleted }
                val allHabitLogs = app.container.habitRepository.getAllHabitLogsSync().filter { it.date == todayIso && it.isCompleted }
                val completedHabitIds = allHabitLogs.map { it.habitId }.toSet()
                val pendingHabits = allHabits.filter { it.id !in completedHabitIds }
                val completedHabitsCount = allHabits.size - pendingHabits.size
                val totalHabitsCount = allHabits.size

                val title = when {
                    totalTasksCount > 0 -> {
                        val percent = if (totalTasksCount > 0) (completedTasksCount * 100) / totalTasksCount else 0
                        "Today: $completedTasksCount/$totalTasksCount Tasks ($percent%)"
                    }
                    totalHabitsCount > 0 -> {
                        "Today: $completedHabitsCount/$totalHabitsCount Habits"
                    }
                    else -> "TaskSpace: Today"
                }

                val shortContent = when {
                    activeTasks.isNotEmpty() -> {
                        val topTask = activeTasks.first()
                        val timeStr = if (!topTask.time.isNullOrBlank()) " at ${topTask.time}" else ""
                        "Next: ${topTask.title}$timeStr"
                    }
                    pendingHabits.isNotEmpty() -> {
                        "All tasks done. ${pendingHabits.size} habits pending."
                    }
                    totalTasksCount > 0 || totalHabitsCount > 0 -> {
                        "All items completed for today."
                    }
                    else -> "No tasks scheduled for today. Tap + to add one."
                }

                val expandedText = buildString {
                    if (activeTasks.isNotEmpty()) {
                        appendLine("Tasks:")
                        activeTasks.take(3).forEach { t ->
                            val timePart = if (!t.time.isNullOrBlank()) " (${t.time})" else ""
                            appendLine("  • ${t.title}$timePart")
                        }
                        if (activeTasks.size > 3) {
                            appendLine("  ...and ${activeTasks.size - 3} more")
                        }
                    } else if (totalTasksCount > 0) {
                        appendLine("Tasks: All $totalTasksCount completed.")
                    }

                    if (pendingHabits.isNotEmpty()) {
                        if (activeTasks.isNotEmpty()) appendLine()
                        appendLine("Pending Habits:")
                        pendingHabits.take(3).forEach { h ->
                            appendLine("  • ${h.name}")
                        }
                        if (pendingHabits.size > 3) {
                            appendLine("  ...and ${pendingHabits.size - 3} more")
                        }
                    } else if (totalHabitsCount > 0) {
                        if (activeTasks.isNotEmpty()) appendLine()
                        appendLine("Habits: All $totalHabitsCount completed.")
                    }

                    if (totalTasksCount == 0 && totalHabitsCount == 0) {
                        append("No tasks scheduled for today. Tap + to add one.")
                    }
                }.trimEnd()

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    100,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, DASHBOARD_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(shortContent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(contentPendingIntent)
                    .setOngoing(true)
                    .setSilent(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)

                if (totalTasksCount > 0) {
                    builder.setProgress(totalTasksCount, completedTasksCount, false)
                }

                // Action 1: Add Task
                val addTaskIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_ADD_TASK
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_NAVIGATE_TO", "add_task")
                }
                val addTaskPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    addTaskIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "+ Add Task", addTaskPendingIntent)

                // Action 2: Complete Next Task (if active task exists)
                if (activeTasks.isNotEmpty()) {
                    val topTaskId = activeTasks.first().id
                    val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                        action = ACTION_COMPLETE_TOP_TASK
                        putExtra("EXTRA_TASK_ID", topTaskId)
                    }
                    val donePendingIntent = PendingIntent.getBroadcast(
                        context,
                        102,
                        doneIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, "Done Task", donePendingIntent)
                }

                // Action 3: Check-in Next Habit (if pending habits exist)
                if (pendingHabits.isNotEmpty()) {
                    val topHabit = pendingHabits.first()
                    val checkHabitIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                        action = ACTION_CHECK_TOP_HABIT
                        putExtra("EXTRA_HABIT_ID", topHabit.id)
                    }
                    val checkHabitPendingIntent = PendingIntent.getBroadcast(
                        context,
                        103,
                        checkHabitIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val habitActionLabel = if (topHabit.name.length > 12) "Check Habit" else "Check ${topHabit.name}"
                    builder.addAction(0, habitActionLabel, checkHabitPendingIntent)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID_DASHBOARD, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelDailyDashboardNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIFICATION_ID_DASHBOARD)
    }

    fun scheduleReminder(context: Context, id: String, title: String, notes: String, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_NOTES", notes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode().and(0xFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode().and(0xFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

