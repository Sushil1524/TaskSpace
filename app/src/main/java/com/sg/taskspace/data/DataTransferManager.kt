package com.sg.taskspace.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("tasks") val tasks: List<Task>,
    @SerializedName("journalEntries") val journalEntries: List<JournalEntry>? = null,
    @SerializedName("habits") val habits: List<Habit>? = null,
    @SerializedName("habitLogs") val habitLogs: List<HabitLog>? = null,
    @SerializedName("goals") val goals: List<Goal>? = null,
    @SerializedName("userPreferences") val userPreferences: UserPreferences
)

class DataTransferManager(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportData(uri: Uri): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val tasks = taskRepository.getAllTasksSync()
                val journalEntries = journalRepository.allJournalEntries.first()
                val habits = habitRepository.allHabits.first()
                val habitLogs = habitRepository.allHabitLogs.first()
                val goals = goalRepository.allGoals.first()
                val prefs = userPreferencesRepository.fetchInitialPreferences()
                val backupData = BackupData(
                    tasks = tasks,
                    journalEntries = journalEntries,
                    habits = habits,
                    habitLogs = habitLogs,
                    goals = goals,
                    userPreferences = prefs
                )
                val jsonString = gson.toJson(backupData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun importData(uri: Uri): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonString = stringBuilder.toString()
                val backupData = gson.fromJson(jsonString, BackupData::class.java)

                backupData.tasks.forEach { task ->
                    taskRepository.insertTask(task)
                }

                backupData.journalEntries.orEmpty().forEach { entry ->
                    journalRepository.insertJournalEntry(entry)
                }

                backupData.habits.orEmpty().forEach { habit ->
                    habitRepository.insertHabit(habit)
                }

                backupData.habitLogs.orEmpty().forEach { log ->
                    habitRepository.insertHabitLog(log)
                }

                backupData.goals.orEmpty().forEach { goal ->
                    goalRepository.insertGoal(goal)
                }
                
                userPreferencesRepository.restorePreferences(
                    backupData.userPreferences.userName,
                    backupData.userPreferences.isOnboardingCompleted
                )
                
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
