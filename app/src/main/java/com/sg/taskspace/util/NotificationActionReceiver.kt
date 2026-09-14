package com.sg.taskspace.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sg.taskspace.TaskSpaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? TaskSpaceApplication ?: return

        when (action) {
            NotificationHelper.ACTION_COMPLETE_TOP_TASK -> {
                val taskId = intent.getStringExtra("EXTRA_TASK_ID")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val taskRepo = app.container.taskRepository
                        val userPrefsRepo = app.container.userPreferencesRepository

                        val targetTask = if (!taskId.isNullOrBlank()) {
                            taskRepo.getTaskById(taskId).firstOrNull()
                        } else {
                            val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                            val allTasks = taskRepo.getAllTasksSync()
                            allTasks.firstOrNull { it.createdForDate == todayIso && !it.isDeleted && !it.isCompleted }
                        }

                        if (targetTask != null && !targetTask.isCompleted) {
                            val xp = if (targetTask.priority == "High") 20 else 10
                            userPrefsRepo.addXp(xp)

                            if (targetTask.repeat != "None") {
                                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                                val completedInstance = targetTask.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    createdForDate = todayIso,
                                    isCompleted = true,
                                    repeat = "None",
                                    parentId = targetTask.id
                                )
                                taskRepo.insertTask(completedInstance)
                            } else {
                                val updated = targetTask.copy(isCompleted = true)
                                taskRepo.updateTask(updated)
                                NotificationHelper.cancelReminder(context, targetTask.id)
                            }
                        }

                        NotificationHelper.updateDailyDashboardNotification(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            NotificationHelper.ACTION_CHECK_TOP_HABIT -> {
                val habitId = intent.getStringExtra("EXTRA_HABIT_ID")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val habitRepo = app.container.habitRepository
                        val userPrefsRepo = app.container.userPreferencesRepository
                        val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

                        val targetHabit = if (!habitId.isNullOrBlank()) {
                            val habits = habitRepo.getAllHabitsSync()
                            habits.firstOrNull { it.id == habitId && !it.isDeleted && !it.type.equals("QUIT", ignoreCase = true) }
                        } else {
                            val habits = habitRepo.getAllHabitsSync().filter { !it.isDeleted && !it.type.equals("QUIT", ignoreCase = true) }
                            val todayLogs = habitRepo.getAllHabitLogsSync().filter { it.date == todayIso && it.isCompleted }
                            val completedIds = todayLogs.map { it.habitId }.toSet()
                            habits.firstOrNull { it.id !in completedIds }
                        }

                        if (targetHabit != null) {
                            val allLogs = habitRepo.getAllHabitLogsSync()
                            val existingLog = allLogs.find { it.habitId == targetHabit.id && it.date == todayIso }
                            if (existingLog != null) {
                                habitRepo.insertHabitLog(
                                    existingLog.copy(
                                        isCompleted = true,
                                        value = targetHabit.targetValue
                                    )
                                )
                            } else {
                                habitRepo.insertHabitLog(
                                    com.sg.taskspace.data.HabitLog(
                                        habitId = targetHabit.id,
                                        date = todayIso,
                                        isCompleted = true,
                                        value = targetHabit.targetValue
                                    )
                                )
                            }
                            userPrefsRepo.addXp(15)
                        }

                        NotificationHelper.updateDailyDashboardNotification(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            NotificationHelper.ACTION_REFRESH_DASHBOARD -> {
                NotificationHelper.updateDailyDashboardNotification(context)
            }
        }
    }
}
