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
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class BackupData(
    @SerializedName("tasks") val tasks: List<Task>? = null,
    @SerializedName("journalEntries") val journalEntries: List<JournalEntry>? = null,
    @SerializedName("habits") val habits: List<Habit>? = null,
    @SerializedName("habitLogs") val habitLogs: List<HabitLog>? = null,
    @SerializedName("goals") val goals: List<Goal>? = null,
    @SerializedName("projects") val projects: List<Project>? = null,
    @SerializedName("userPreferences") val userPreferences: UserPreferences? = null
)

/** Per-entity diff counts computed by [DataTransferManager.previewImport]. */
data class EntityDiff(
    val added: Int = 0,
    val modified: Int = 0,
    val deleted: Int = 0
) {
    val hasChanges: Boolean get() = added > 0 || modified > 0 || deleted > 0
    val totalImpact: Int get() = added + modified + deleted
}

/** Full diff result returned before any restore is committed. */
data class BackupDiffResult(
    val tasks: EntityDiff,
    val habits: EntityDiff,
    val journalEntries: EntityDiff,
    val goals: EntityDiff,
    val projects: EntityDiff,
    /** The parsed backup — passed back to importData() so we don't re-read/decrypt. */
    val parsedBackup: BackupData
) {
    val hasAnyChanges: Boolean
        get() = tasks.hasChanges || habits.hasChanges || journalEntries.hasChanges ||
                goals.hasChanges || projects.hasChanges
    val totalChanges: Int
        get() = tasks.totalImpact + habits.totalImpact + journalEntries.totalImpact +
                goals.totalImpact + projects.totalImpact
}


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
        private val AES_SECRET_KEY = com.sg.taskspace.BuildConfig.BACKUP_SECRET_KEY.toByteArray(Charsets.UTF_8).copyOf(16)
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

    private fun parseBackupJson(jsonString: String): BackupData? {
        return try {
            val backup = gson.fromJson(jsonString, BackupData::class.java)
            if (backup != null && (backup.tasks != null || backup.userPreferences != null || backup.habits != null || backup.projects != null || backup.goals != null || backup.journalEntries != null)) {
                backup
            } else {
                val listType = object : com.google.gson.reflect.TypeToken<List<Task>>() {}.type
                val rawTasks = gson.fromJson<List<Task>>(jsonString, listType)
                if (!rawTasks.isNullOrEmpty()) {
                    BackupData(tasks = rawTasks)
                } else null
            }
        } catch (e: Exception) {
            try {
                val listType = object : com.google.gson.reflect.TypeToken<List<Task>>() {}.type
                val rawTasks = gson.fromJson<List<Task>>(jsonString, listType)
                if (!rawTasks.isNullOrEmpty()) {
                    BackupData(tasks = rawTasks)
                } else null
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
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
                    val journalEntries = journalRepository.getAllJournalEntriesSync()
                    val habits = habitRepository.getAllHabitsSync()
                    val habitLogs = habitRepository.getAllHabitLogsSync()
                    val goals = goalRepository.getAllGoalsSync()
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
                val journalEntries = journalRepository.getAllJournalEntriesSync()
                val habits = habitRepository.getAllHabitsSync()
                val habitLogs = habitRepository.getAllHabitLogsSync()
                val goals = goalRepository.getAllGoalsSync()
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

                val bytes = payloadToWrite.toByteArray(Charsets.UTF_8)
                var bytesWritten = false

                // Method 1: Direct ParcelFileDescriptor with hardware sync
                runCatching {
                    context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                        java.io.FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.write(bytes)
                            fos.flush()
                            runCatching { pfd.fileDescriptor.sync() }
                            bytesWritten = true
                        }
                    }
                }

                // Method 2: Standard openOutputStream fallback
                if (!bytesWritten) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(bytes)
                            os.flush()
                            bytesWritten = true
                        }
                    }
                }

                if (!bytesWritten) {
                    return@withContext Result.failure(Exception("Failed to write backup bytes to $uri"))
                }

                android.util.Log.d("DataTransfer", "Successfully exported ${bytes.size} bytes to $uri")
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
                val rawContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                if (rawContent.isNullOrBlank()) return@withContext Result.failure(Exception("Backup file is empty"))

                val jsonString = decryptData(rawContent)
                val backupData = parseBackupJson(jsonString)
                    ?: return@withContext Result.failure(Exception("Failed to parse backup file"))

                importDataFromParsed(backupData)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Phase 1 of restore: read, decrypt, and diff the backup file against the
     * current database. NO writes are performed. The caller can inspect the
     * returned [BackupDiffResult] and then call [importDataFromParsed] to commit.
     */
    suspend fun previewImport(uri: Uri): Result<BackupDiffResult> {
        return withContext(Dispatchers.IO) {
            try {
                // ── Read & decrypt file ──
                val rawContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }?.takeIf { it.isNotBlank() }
                    ?: return@withContext Result.failure(Exception("Backup file is empty or unreadable"))

                val jsonString = decryptData(rawContent)
                val backupData = parseBackupJson(jsonString)
                    ?: return@withContext Result.failure(Exception("Failed to parse backup file"))

                // ── Load current DB state ──
                val currentTasks = taskRepository.getAllTasksSync().associateBy { it.id }
                val currentHabits = habitRepository.getAllHabitsSync().associateBy { it.id }
                val currentJournal = journalRepository.getAllJournalEntriesSync().associateBy { it.id }
                val currentGoals = goalRepository.getAllGoalsSync().associateBy { it.id }
                val currentProjects = projectRepository.getAllProjectsSync().associateBy { it.id }

                // ── Compute task diff ──
                val backupTasks = backupData.tasks.orEmpty()
                val backupTaskMap = backupTasks.associateBy { it.id }
                val taskDiff = EntityDiff(
                    added = backupTasks.count { it.id !in currentTasks },
                    modified = backupTasks.count { t ->
                        val cur = currentTasks[t.id]
                        cur != null && cur != t
                    },
                    deleted = currentTasks.count { it.key !in backupTaskMap }
                )

                // ── Compute habits diff ──
                val backupHabits = backupData.habits.orEmpty()
                val backupHabitMap = backupHabits.associateBy { it.id }
                val habitDiff = EntityDiff(
                    added = backupHabits.count { it.id !in currentHabits },
                    modified = backupHabits.count { h ->
                        val cur = currentHabits[h.id]; cur != null && cur != h
                    },
                    deleted = currentHabits.count { it.key !in backupHabitMap }
                )

                // ── Compute journal diff (by date since journal is date-keyed) ──
                val backupJournal = backupData.journalEntries.orEmpty()
                val backupJournalMap = backupJournal.associateBy { it.id }
                val journalDiff = EntityDiff(
                    added = backupJournal.count { it.id !in currentJournal },
                    modified = backupJournal.count { j ->
                        val cur = currentJournal[j.id]; cur != null && cur != j
                    },
                    deleted = currentJournal.count { it.key !in backupJournalMap }
                )

                // ── Compute goals diff ──
                val backupGoals = backupData.goals.orEmpty()
                val backupGoalMap = backupGoals.associateBy { it.id }
                val goalDiff = EntityDiff(
                    added = backupGoals.count { it.id !in currentGoals },
                    modified = backupGoals.count { g ->
                        val cur = currentGoals[g.id]; cur != null && cur != g
                    },
                    deleted = currentGoals.count { it.key !in backupGoalMap }
                )

                // ── Compute projects diff ──
                val backupProjects = backupData.projects.orEmpty()
                val backupProjectMap = backupProjects.associateBy { it.id }
                val projectDiff = EntityDiff(
                    added = backupProjects.count { it.id !in currentProjects },
                    modified = backupProjects.count { p ->
                        val cur = currentProjects[p.id]; cur != null && cur != p
                    },
                    deleted = currentProjects.count { it.key !in backupProjectMap }
                )

                val diffResult = BackupDiffResult(
                    tasks = taskDiff,
                    habits = habitDiff,
                    journalEntries = journalDiff,
                    goals = goalDiff,
                    projects = projectDiff,
                    parsedBackup = backupData
                )

                Result.success(diffResult)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    /**
     * Phase 2 of restore: takes the already-parsed [BackupData] (from [previewImport])
     * and commits all changes to the database. Clears existing data first.
     */
    suspend fun importDataFromParsed(backupData: BackupData): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // ── Clear all existing data first to prevent duplicates ──
                taskRepository.deleteAllTasks()
                journalRepository.deleteAllJournalEntries()
                habitRepository.deleteAllHabitLogs()
                habitRepository.deleteAllHabits()
                goalRepository.deleteAllGoals()
                projectRepository.deleteAllProjects()

                // ── Restore all data from backup ──
                backupData.tasks.orEmpty().forEach { task -> taskRepository.insertTask(task) }
                backupData.journalEntries.orEmpty().forEach { entry -> journalRepository.insertJournalEntry(entry) }
                backupData.habits.orEmpty().forEach { habit -> habitRepository.insertHabit(habit) }
                backupData.habitLogs.orEmpty().forEach { log -> habitRepository.insertHabitLog(log) }
                backupData.goals.orEmpty().forEach { goal -> goalRepository.insertGoal(goal) }
                backupData.projects.orEmpty().forEach { project -> projectRepository.insertProject(project) }

                // ── Restore preferences (all fields including swipe gestures) if present ──
                backupData.userPreferences?.let { prefs ->
                    userPreferencesRepository.restorePreferences(
                        userName = prefs.userName,
                        isOnboardingCompleted = prefs.isOnboardingCompleted,
                        themeMode = prefs.themeMode,
                        fontFamily = prefs.fontFamily,
                        colorPalette = prefs.colorPalette,
                        isSecureBackupEnabled = prefs.isSecureBackupEnabled,
                        autoBackupFrequency = prefs.autoBackupFrequency,
                        notificationsEnabled = prefs.notificationsEnabled,
                        soundEffectsEnabled = prefs.soundEffectsEnabled,
                        swipeRightAction = prefs.swipeRightAction,
                        swipeLeftAction = prefs.swipeLeftAction,
                        isAppLockEnabled = prefs.isAppLockEnabled,
                        appLockPinHash = prefs.appLockPinHash,
                        isBiometricEnabled = prefs.isBiometricEnabled,
                        autoLockTimeout = prefs.autoLockTimeout,
                        totalXp = prefs.totalXp
                    )
                }

                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
