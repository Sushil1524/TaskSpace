package com.sg.taskspace.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? TaskSpaceApplication ?: return

        when (action) {
            NotificationHelper.ACTION_QUICK_ADD_TASK -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val rawInput = remoteInput?.getCharSequence(NotificationHelper.KEY_QUICK_ADD_TEXT)?.toString()?.trim()

                if (!rawInput.isNullOrBlank()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val taskRepo = app.container.taskRepository
                            val parsed = parseQuickAddInput(rawInput)
                            val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

                            val newTask = Task(
                                id = UUID.randomUUID().toString(),
                                title = parsed.title,
                                priority = parsed.priority,
                                tags = parsed.tags,
                                createdForDate = todayIso,
                                createdAt = System.currentTimeMillis()
                            )
                            taskRepo.insertTask(newTask)

                            // Refresh ongoing notification immediately to display the newly added task
                            NotificationHelper.updateDailyDashboardNotification(context)

                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Added: ${parsed.title} [${parsed.priority}]",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
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

    data class ParsedQuickTask(
        val title: String,
        val priority: String,
        val tags: String
    )

    companion object {
        fun parseQuickAddInput(rawInput: String): ParsedQuickTask {
            var text = rawInput.trim()

            // 1. Detect priority triggers: !high, !urgent, !low, !medium, !normal
            var priority = "Medium"
            val priorityRegex = Regex("""(?i)(?:^|\s)!(high|urgent|low|medium|normal)(?=\s|$)""")
            val priorityMatch = priorityRegex.find(text)
            if (priorityMatch != null) {
                val matched = priorityMatch.groupValues[1].lowercase()
                priority = when (matched) {
                    "high", "urgent" -> "High"
                    "low" -> "Low"
                    else -> "Medium"
                }
                text = text.replace(priorityRegex, " ")
            }

            // 2. Detect #tags: e.g. #work, #errand, #urgent
            val tagRegex = Regex("""(?:^|\s)(#[a-zA-Z0-9_\-]+)""")
            val tagsList = tagRegex.findAll(text).map { it.groupValues[1].trim() }.toList()
            if (tagsList.isNotEmpty()) {
                text = text.replace(tagRegex, " ")
            }
            val tagsString = tagsList.joinToString(",")

            // 3. Clean up title
            val cleanTitle = text.replace(Regex("""\s+"""), " ").trim()
            val finalTitle = if (cleanTitle.isNotBlank()) cleanTitle else "New Task"

            return ParsedQuickTask(
                title = finalTitle,
                priority = priority,
                tags = tagsString
            )
        }
    }
}
