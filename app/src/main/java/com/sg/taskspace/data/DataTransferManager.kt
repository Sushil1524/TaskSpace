package com.sg.taskspace.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class BackupData(
    @SerializedName("tasks") val tasks: List<Task>,
    @SerializedName("journalEntries") val journalEntries: List<JournalEntry>? = null,
    @SerializedName("habits") val habits: List<Habit>? = null,
    @SerializedName("habitLogs") val habitLogs: List<HabitLog>? = null,
    @SerializedName("goals") val goals: List<Goal>? = null,
    @SerializedName("projects") val projects: List<Project>? = null,
    @SerializedName("userPreferences") val userPreferences: UserPreferences
)

class DataTransferManager(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val projectRepository: ProjectRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private companion object {
        private const val ENCRYPTION_PREFIX = "TASKSPACE_ENCRYPTED_V1:"
        private val AES_SECRET_KEY = "TaskSpaceSecure2026SaltKeyBytes".toByteArray(Charsets.UTF_8).copyOf(16)
    }

    private fun encryptData(plainText: String): String {
        return try {
            val secretKeySpec = SecretKeySpec(AES_SECRET_KEY, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            ENCRYPTION_PREFIX + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    private fun decryptData(cipherText: String): String {
        if (!cipherText.startsWith(ENCRYPTION_PREFIX)) return cipherText
        return try {
            val rawPayload = cipherText.removePrefix(ENCRYPTION_PREFIX).trim()
            val decodedBytes = Base64.decode(rawPayload, Base64.NO_WRAP)
            val secretKeySpec = SecretKeySpec(AES_SECRET_KEY, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }

    suspend fun performAutoBackupIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                val prefs = userPreferencesRepository.fetchInitialPreferences()
                val freq = prefs.autoBackupFrequency
                if (freq == "Off") return@withContext

                val intervalMillis = if (freq == "Weekly") 7 * 86400000L else 86400000L
                val now = System.currentTimeMillis()

                if (now - prefs.lastAutoBackupTimeMillis >= intervalMillis) {
                    val tasks = taskRepository.getAllTasksSync()
                    val journalEntries = journalRepository.allJournalEntries.first()
                    val habits = habitRepository.allHabits.first()
                    val habitLogs = habitRepository.allHabitLogs.first()
                    val goals = goalRepository.allGoals.first()
                    val projects = projectRepository.getAllProjectsSync()
                    val backupData = BackupData(
                        tasks = tasks,
                        journalEntries = journalEntries,
                        habits = habits,
                        habitLogs = habitLogs,
                        goals = goals,
                        projects = projects,
                        userPreferences = prefs
                    )
                    val jsonString = gson.toJson(backupData)
                    val contentToWrite = if (prefs.isSecureBackupEnabled) encryptData(jsonString) else jsonString

                    val backupFile = File(context.filesDir, "taskspace_autobackup.json")
                    backupFile.writeText(contentToWrite)

                    userPreferencesRepository.updateLastAutoBackupTime(now)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun exportData(uri: Uri): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val tasks = taskRepository.getAllTasksSync()
                val journalEntries = journalRepository.allJournalEntries.first()
                val habits = habitRepository.allHabits.first()
                val habitLogs = habitRepository.allHabitLogs.first()
                val goals = goalRepository.allGoals.first()
                val projects = projectRepository.getAllProjectsSync()
                val prefs = userPreferencesRepository.fetchInitialPreferences()
                val backupData = BackupData(
                    tasks = tasks,
                    journalEntries = journalEntries,
                    habits = habits,
                    habitLogs = habitLogs,
                    goals = goals,
                    projects = projects,
                    userPreferences = prefs
                )
                val jsonString = gson.toJson(backupData)
                val payloadToWrite = if (prefs.isSecureBackupEnabled) encryptData(jsonString) else jsonString

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(payloadToWrite.toByteArray(Charsets.UTF_8))
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
                val rawContent = stringBuilder.toString()
                val jsonString = decryptData(rawContent)
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

                backupData.projects.orEmpty().forEach { project ->
                    projectRepository.insertProject(project)
                }

                userPreferencesRepository.restorePreferences(
                    userName = backupData.userPreferences.userName,
                    isOnboardingCompleted = backupData.userPreferences.isOnboardingCompleted,
                    themeMode = backupData.userPreferences.themeMode,
                    fontFamily = backupData.userPreferences.fontFamily,
                    colorPalette = backupData.userPreferences.colorPalette,
                    isSecureBackupEnabled = backupData.userPreferences.isSecureBackupEnabled,
                    autoBackupFrequency = backupData.userPreferences.autoBackupFrequency,
                    notificationsEnabled = backupData.userPreferences.notificationsEnabled,
                    soundEffectsEnabled = backupData.userPreferences.soundEffectsEnabled,
                    totalXp = backupData.userPreferences.totalXp
                )

                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
