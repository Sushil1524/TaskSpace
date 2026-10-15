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

enum class RestoreStrategy(val label: String, val description: String) {
    MERGE_BACKUP_WINS("Merge (Backup Overwrites Conflicts)", "Keep current local data + add backup items. If IDs match, use backup version."),
    MERGE_LOCAL_WINS("Merge (Keep Local Conflicts)", "Keep current local data + add new backup items. If IDs match, keep local version."),
    REPLACE_LOCAL("Replace Category", "Wipe current local data for this category and overwrite with backup items."),
    KEEP_LOCAL("Keep Local (Skip)", "Do not change current local data for this category; ignore backup items.")
}

enum class ItemDiffType {
    ADDED,
    MODIFIED,
    DELETED_IN_BACKUP
}

data class ItemDiffDetails(
    val id: String,
    val title: String,
    val diffType: ItemDiffType
)

data class CategoryRestoreConfig(
    val strategy: RestoreStrategy = RestoreStrategy.MERGE_BACKUP_WINS,
    val excludedItemIds: Set<String> = emptySet()
)

data class RestoreSelectionConfig(
    val taskConfig: CategoryRestoreConfig = CategoryRestoreConfig(),
    val habitConfig: CategoryRestoreConfig = CategoryRestoreConfig(),
    val journalConfig: CategoryRestoreConfig = CategoryRestoreConfig(),
    val goalConfig: CategoryRestoreConfig = CategoryRestoreConfig(),
    val projectConfig: CategoryRestoreConfig = CategoryRestoreConfig(),
    val restorePreferences: Boolean = true
)

/** Per-entity diff counts computed by [DataTransferManager.previewImport]. */
data class EntityDiff(
    val added: Int = 0,
    val modified: Int = 0,
    val deleted: Int = 0,
    val items: List<ItemDiffDetails> = emptyList()
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

data class BackupSnapshot(
    val file: File,
    val fileName: String,
    val timestampMillis: Long,
    val sizeBytes: Long,
    val isEncrypted: Boolean,
    val formattedDate: String,
    val relativeTime: String,
    val isLatest: Boolean = false,
    val hash: String = ""
) {
    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            return if (digitGroups == 0) {
                "$sizeBytes B"
            } else {
                String.format(java.util.Locale.US, "%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
            }
        }
}

private data class SnapshotFileMeta(
    val file: File,
    val timestamp: Long,
    val date: java.time.LocalDate
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
        private val AES_SECRET_KEY = com.sg.taskspace.BuildConfig.BACKUP_SECRET_KEY.toByteArray(Charsets.UTF_8).copyOf(16)
        private val FILE_DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", java.util.Locale.US)
        private val DISPLAY_DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", java.util.Locale.US)
    }

    fun computeSha256(content: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(content.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun computeFileSha256(file: File): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatRelativeTime(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - timestampMillis).coerceAtLeast(0L)
        if (diff < 60_000L) return "Just now"
        if (diff < 3600_000L) return "${diff / 60_000L}m ago"
        if (diff < 86400_000L) return "${diff / 3600_000L}h ago"
        val days = diff / 86400_000L
        if (days == 1L) return "Yesterday"
        if (days < 7L) return "${days}d ago"
        return "${days / 7L}w ago"
    }

    private fun parseTimestampFromFileName(fileName: String): Long? {
        return try {
            val match = Regex("""autobackup_(\d{8}_\d{6})""").find(fileName)
            if (match != null) {
                val dateTimeStr = match.groupValues[1]
                val localDateTime = java.time.LocalDateTime.parse(dateTimeStr, FILE_DATE_FORMATTER)
                localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getAutoBackupDirectory(): File {
        val dir = File(File(context.filesDir, "backups"), "autobackups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun migrateLegacyBackupIfNeeded(backupDir: File) {
        try {
            val legacyFile = File(context.filesDir, "taskspace_autobackup.json")
            if (legacyFile.exists() && legacyFile.length() > 0) {
                val timestamp = legacyFile.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
                val instant = java.time.Instant.ofEpochMilli(timestamp)
                val dt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                val dateStr = dt.format(FILE_DATE_FORMATTER)
                val targetFile = File(backupDir, "autobackup_${dateStr}.json")
                if (!targetFile.exists()) {
                    legacyFile.copyTo(targetFile, overwrite = true)
                }
                legacyFile.delete()
                android.util.Log.d("DataTransferManager", "Migrated legacy autobackup to ${targetFile.name}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pruneOldBackups(): Int {
        val backupDir = getAutoBackupDirectory()
        val files = backupDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?: return 0

        if (files.isEmpty()) return 0

        val zoneId = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zoneId)

        val filesWithMeta = files.map { file ->
            val ts = parseTimestampFromFileName(file.name) ?: file.lastModified()
            val localDate = java.time.Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate()
            SnapshotFileMeta(file, ts, localDate)
        }.sortedByDescending { it.timestamp }

        val keepFiles = mutableSetOf<File>()

        // 1. Keep the 3 most recent backups
        keepFiles.addAll(filesWithMeta.take(3).map { it.file })

        // 2. Keep 1 daily snapshot per day for the last 7 days (today = day 0..6)
        val filesByDate = filesWithMeta.groupBy { it.date }
        for (dayOffset in 0L..6L) {
            val targetDate = today.minusDays(dayOffset)
            val filesOnDate = filesByDate[targetDate]
            if (!filesOnDate.isNullOrEmpty()) {
                val latestOnDate = filesOnDate.maxByOrNull { it.timestamp }
                if (latestOnDate != null) {
                    keepFiles.add(latestOnDate.file)
                }
            }
        }

        // 3. Auto-prune anything older / not in keepFiles
        var prunedCount = 0
        for (meta in filesWithMeta) {
            if (meta.file !in keepFiles) {
                if (meta.file.delete()) {
                    prunedCount++
                }
            }
        }
        return prunedCount
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

                    // Reset volatile runtime fields for stable content hashing
                    val cleanPrefs = prefs.copy(
                        lastAutoBackupTimeMillis = 0L,
                        lastAutoBackupHash = "",
                        lastBackgroundTimeMillis = 0L
                    )
                    val backupData = BackupData(
                        tasks = tasks,
                        journalEntries = journalEntries,
                        habits = habits,
                        habitLogs = habitLogs,
                        goals = goals,
                        projects = projects,
                        userPreferences = cleanPrefs
                    )
                    val jsonString = gson.toJson(backupData)
                    val contentHash = computeSha256(jsonString)

                    // Fingerprint hashing check: Skip redundant backup writes if payload hasn't changed
                    if (contentHash.isNotBlank() && contentHash == prefs.lastAutoBackupHash) {
                        android.util.Log.d("DataTransferManager", "Auto-backup skipped: payload hash unchanged ($contentHash)")
                        userPreferencesRepository.updateLastAutoBackupTime(now)
                        return@withContext
                    }

                    val backupDir = getAutoBackupDirectory()
                    migrateLegacyBackupIfNeeded(backupDir)

                    val nowInstant = java.time.Instant.ofEpochMilli(now)
                    val localDateTime = java.time.LocalDateTime.ofInstant(nowInstant, java.time.ZoneId.systemDefault())
                    val dateStr = localDateTime.format(FILE_DATE_FORMATTER)
                    val backupFile = File(backupDir, "autobackup_${dateStr}.json")

                    val contentToWrite = if (prefs.isSecureBackupEnabled) encryptData(jsonString) else jsonString
                    backupFile.writeText(contentToWrite)

                    userPreferencesRepository.updateLastAutoBackup(now, contentHash)
                    pruneOldBackups()
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

                val diffResult = computeBackupDiff(backupData)
                Result.success(diffResult)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun getAutoBackupSnapshots(): List<BackupSnapshot> {
        return withContext(Dispatchers.IO) {
            try {
                val backupDir = getAutoBackupDirectory()
                migrateLegacyBackupIfNeeded(backupDir)

                val files = backupDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                    ?: return@withContext emptyList()

                val zoneId = java.time.ZoneId.systemDefault()
                val sortedFiles = files.map { file ->
                    val ts = parseTimestampFromFileName(file.name) ?: file.lastModified()
                    file to ts
                }.sortedByDescending { it.second }

                sortedFiles.mapIndexed { index, (file, ts) ->
                    val instant = java.time.Instant.ofEpochMilli(ts)
                    val ldt = java.time.LocalDateTime.ofInstant(instant, zoneId)
                    val formattedDate = ldt.format(DISPLAY_DATE_FORMATTER)
                    val relativeTime = formatRelativeTime(ts)
                    val isEncrypted = try {
                        file.bufferedReader().use { reader ->
                            val buf = CharArray(ENCRYPTION_PREFIX.length)
                            val read = reader.read(buf)
                            read == ENCRYPTION_PREFIX.length && String(buf) == ENCRYPTION_PREFIX
                        }
                    } catch (e: Exception) {
                        false
                    }
                    val fileHash = computeFileSha256(file)

                    BackupSnapshot(
                        file = file,
                        fileName = file.name,
                        timestampMillis = ts,
                        sizeBytes = file.length(),
                        isEncrypted = isEncrypted,
                        formattedDate = formattedDate,
                        relativeTime = relativeTime,
                        isLatest = (index == 0),
                        hash = fileHash
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun createAutoBackupSnapshot(): Result<BackupSnapshot> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = userPreferencesRepository.fetchInitialPreferences()
                val now = System.currentTimeMillis()
                val tasks = taskRepository.getAllTasksSync()
                val journalEntries = journalRepository.getAllJournalEntriesSync()
                val habits = habitRepository.getAllHabitsSync()
                val habitLogs = habitRepository.getAllHabitLogsSync()
                val goals = goalRepository.getAllGoalsSync()
                val projects = projectRepository.getAllProjectsSync()

                val cleanPrefs = prefs.copy(
                    lastAutoBackupTimeMillis = 0L,
                    lastAutoBackupHash = "",
                    lastBackgroundTimeMillis = 0L
                )
                val backupData = BackupData(
                    tasks = tasks,
                    journalEntries = journalEntries,
                    habits = habits,
                    habitLogs = habitLogs,
                    goals = goals,
                    projects = projects,
                    userPreferences = cleanPrefs
                )
                val jsonString = gson.toJson(backupData)
                val contentHash = computeSha256(jsonString)

                val backupDir = getAutoBackupDirectory()
                migrateLegacyBackupIfNeeded(backupDir)

                val nowInstant = java.time.Instant.ofEpochMilli(now)
                val localDateTime = java.time.LocalDateTime.ofInstant(nowInstant, java.time.ZoneId.systemDefault())
                val dateStr = localDateTime.format(FILE_DATE_FORMATTER)
                val backupFile = File(backupDir, "autobackup_${dateStr}.json")

                val contentToWrite = if (prefs.isSecureBackupEnabled) encryptData(jsonString) else jsonString
                backupFile.writeText(contentToWrite)

                userPreferencesRepository.updateLastAutoBackup(now, contentHash)
                pruneOldBackups()

                val formattedDate = localDateTime.format(DISPLAY_DATE_FORMATTER)
                val snapshot = BackupSnapshot(
                    file = backupFile,
                    fileName = backupFile.name,
                    timestampMillis = now,
                    sizeBytes = backupFile.length(),
                    isEncrypted = prefs.isSecureBackupEnabled,
                    formattedDate = formattedDate,
                    relativeTime = "Just now",
                    isLatest = true,
                    hash = computeFileSha256(backupFile)
                )
                Result.success(snapshot)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun deleteSnapshot(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (file.exists()) file.delete() else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun previewSnapshot(file: File): Result<BackupDiffResult> {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) {
                    return@withContext Result.failure(Exception("Snapshot file is empty or missing"))
                }
                val rawContent = file.readText(Charsets.UTF_8)
                val jsonString = decryptData(rawContent)
                val backupData = parseBackupJson(jsonString)
                    ?: return@withContext Result.failure(Exception("Failed to parse snapshot file"))

                val diffResult = computeBackupDiff(backupData)
                Result.success(diffResult)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private suspend fun computeBackupDiff(backupData: BackupData): BackupDiffResult {
        val currentTasks = taskRepository.getAllTasksSync().associateBy { it.id }
        val currentHabits = habitRepository.getAllHabitsSync().associateBy { it.id }
        val currentJournal = journalRepository.getAllJournalEntriesSync().associateBy { it.id }
        val currentGoals = goalRepository.getAllGoalsSync().associateBy { it.id }
        val currentProjects = projectRepository.getAllProjectsSync().associateBy { it.id }

        val taskDiff = computeCategoryDiff(currentTasks, backupData.tasks.orEmpty(), { it.id }, { it.title })
        val habitDiff = computeCategoryDiff(currentHabits, backupData.habits.orEmpty(), { it.id }, { it.name })
        val journalDiff = computeCategoryDiff(currentJournal, backupData.journalEntries.orEmpty(), { it.id }, { "Journal Entry (${it.date})" })
        val goalDiff = computeCategoryDiff(currentGoals, backupData.goals.orEmpty(), { it.id }, { it.title })
        val projectDiff = computeCategoryDiff(currentProjects, backupData.projects.orEmpty(), { it.id }, { it.name })

        return BackupDiffResult(
            tasks = taskDiff,
            habits = habitDiff,
            journalEntries = journalDiff,
            goals = goalDiff,
            projects = projectDiff,
            parsedBackup = backupData
        )
    }

    private fun <T> computeCategoryDiff(
        currentMap: Map<String, T>,
        backupList: List<T>,
        getId: (T) -> String,
        getTitle: (T) -> String
    ): EntityDiff {
        val backupMap = backupList.associateBy(getId)
        val itemDetails = mutableListOf<ItemDiffDetails>()
        var added = 0
        var modified = 0
        var deleted = 0

        for (b in backupList) {
            val id = getId(b)
            val cur = currentMap[id]
            if (cur == null) {
                added++
                itemDetails.add(ItemDiffDetails(id = id, title = getTitle(b), diffType = ItemDiffType.ADDED))
            } else if (cur != b) {
                modified++
                itemDetails.add(ItemDiffDetails(id = id, title = getTitle(b), diffType = ItemDiffType.MODIFIED))
            }
        }

        for ((id, cur) in currentMap) {
            if (id !in backupMap) {
                deleted++
                itemDetails.add(ItemDiffDetails(id = id, title = getTitle(cur), diffType = ItemDiffType.DELETED_IN_BACKUP))
            }
        }

        return EntityDiff(
            added = added,
            modified = modified,
            deleted = deleted,
            items = itemDetails
        )
    }

    /** Legacy full replace import. */
    suspend fun importDataFromParsed(backupData: BackupData): Result<Boolean> {
        val replaceAllConfig = RestoreSelectionConfig(
            taskConfig = CategoryRestoreConfig(RestoreStrategy.REPLACE_LOCAL),
            habitConfig = CategoryRestoreConfig(RestoreStrategy.REPLACE_LOCAL),
            journalConfig = CategoryRestoreConfig(RestoreStrategy.REPLACE_LOCAL),
            goalConfig = CategoryRestoreConfig(RestoreStrategy.REPLACE_LOCAL),
            projectConfig = CategoryRestoreConfig(RestoreStrategy.REPLACE_LOCAL),
            restorePreferences = true
        )
        return importDataWithSelection(backupData, replaceAllConfig)
    }

    /** Granular restore with per-category strategy & itemized selection. */
    suspend fun importDataWithSelection(
        backupData: BackupData,
        selectionConfig: RestoreSelectionConfig
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Tasks
                val taskConfig = selectionConfig.taskConfig
                when (taskConfig.strategy) {
                    RestoreStrategy.REPLACE_LOCAL -> {
                        taskRepository.deleteAllTasks()
                        backupData.tasks.orEmpty()
                            .filter { it.id !in taskConfig.excludedItemIds }
                            .forEach { task -> taskRepository.insertTask(task) }
                    }
                    RestoreStrategy.MERGE_BACKUP_WINS -> {
                        backupData.tasks.orEmpty()
                            .filter { it.id !in taskConfig.excludedItemIds }
                            .forEach { task -> taskRepository.insertTask(task) }
                    }
                    RestoreStrategy.MERGE_LOCAL_WINS -> {
                        val currentTaskIds = taskRepository.getAllTasksSync().map { it.id }.toSet()
                        backupData.tasks.orEmpty()
                            .filter { it.id !in taskConfig.excludedItemIds && it.id !in currentTaskIds }
                            .forEach { task -> taskRepository.insertTask(task) }
                    }
                    RestoreStrategy.KEEP_LOCAL -> { /* Skip */ }
                }

                // 2. Habits
                val habitConfig = selectionConfig.habitConfig
                when (habitConfig.strategy) {
                    RestoreStrategy.REPLACE_LOCAL -> {
                        habitRepository.deleteAllHabitLogs()
                        habitRepository.deleteAllHabits()
                        backupData.habits.orEmpty()
                            .filter { it.id !in habitConfig.excludedItemIds }
                            .forEach { habit -> habitRepository.insertHabit(habit) }
                        backupData.habitLogs.orEmpty().forEach { log -> habitRepository.insertHabitLog(log) }
                    }
                    RestoreStrategy.MERGE_BACKUP_WINS -> {
                        backupData.habits.orEmpty()
                            .filter { it.id !in habitConfig.excludedItemIds }
                            .forEach { habit -> habitRepository.insertHabit(habit) }
                        backupData.habitLogs.orEmpty().forEach { log -> habitRepository.insertHabitLog(log) }
                    }
                    RestoreStrategy.MERGE_LOCAL_WINS -> {
                        val currentHabitIds = habitRepository.getAllHabitsSync().map { it.id }.toSet()
                        backupData.habits.orEmpty()
                            .filter { it.id !in habitConfig.excludedItemIds && it.id !in currentHabitIds }
                            .forEach { habit -> habitRepository.insertHabit(habit) }
                        backupData.habitLogs.orEmpty().forEach { log -> habitRepository.insertHabitLog(log) }
                    }
                    RestoreStrategy.KEEP_LOCAL -> { /* Skip */ }
                }

                // 3. Journal Entries
                val journalConfig = selectionConfig.journalConfig
                when (journalConfig.strategy) {
                    RestoreStrategy.REPLACE_LOCAL -> {
                        journalRepository.deleteAllJournalEntries()
                        backupData.journalEntries.orEmpty()
                            .filter { it.id !in journalConfig.excludedItemIds }
                            .forEach { entry -> journalRepository.insertJournalEntry(entry) }
                    }
                    RestoreStrategy.MERGE_BACKUP_WINS -> {
                        backupData.journalEntries.orEmpty()
                            .filter { it.id !in journalConfig.excludedItemIds }
                            .forEach { entry -> journalRepository.insertJournalEntry(entry) }
                    }
                    RestoreStrategy.MERGE_LOCAL_WINS -> {
                        val currentJournalIds = journalRepository.getAllJournalEntriesSync().map { it.id }.toSet()
                        backupData.journalEntries.orEmpty()
                            .filter { it.id !in journalConfig.excludedItemIds && it.id !in currentJournalIds }
                            .forEach { entry -> journalRepository.insertJournalEntry(entry) }
                    }
                    RestoreStrategy.KEEP_LOCAL -> { /* Skip */ }
                }

                // 4. Goals
                val goalConfig = selectionConfig.goalConfig
                when (goalConfig.strategy) {
                    RestoreStrategy.REPLACE_LOCAL -> {
                        goalRepository.deleteAllGoals()
                        backupData.goals.orEmpty()
                            .filter { it.id !in goalConfig.excludedItemIds }
                            .forEach { goal -> goalRepository.insertGoal(goal) }
                    }
                    RestoreStrategy.MERGE_BACKUP_WINS -> {
                        backupData.goals.orEmpty()
                            .filter { it.id !in goalConfig.excludedItemIds }
                            .forEach { goal -> goalRepository.insertGoal(goal) }
                    }
                    RestoreStrategy.MERGE_LOCAL_WINS -> {
                        val currentGoalIds = goalRepository.getAllGoalsSync().map { it.id }.toSet()
                        backupData.goals.orEmpty()
                            .filter { it.id !in goalConfig.excludedItemIds && it.id !in currentGoalIds }
                            .forEach { goal -> goalRepository.insertGoal(goal) }
                    }
                    RestoreStrategy.KEEP_LOCAL -> { /* Skip */ }
                }

                // 5. Projects
                val projectConfig = selectionConfig.projectConfig
                when (projectConfig.strategy) {
                    RestoreStrategy.REPLACE_LOCAL -> {
                        projectRepository.deleteAllProjects()
                        backupData.projects.orEmpty()
                            .filter { it.id !in projectConfig.excludedItemIds }
                            .forEach { project -> projectRepository.insertProject(project) }
                    }
                    RestoreStrategy.MERGE_BACKUP_WINS -> {
                        backupData.projects.orEmpty()
                            .filter { it.id !in projectConfig.excludedItemIds }
                            .forEach { project -> projectRepository.insertProject(project) }
                    }
                    RestoreStrategy.MERGE_LOCAL_WINS -> {
                        val currentProjectIds = projectRepository.getAllProjectsSync().map { it.id }.toSet()
                        backupData.projects.orEmpty()
                            .filter { it.id !in projectConfig.excludedItemIds && it.id !in currentProjectIds }
                            .forEach { project -> projectRepository.insertProject(project) }
                    }
                    RestoreStrategy.KEEP_LOCAL -> { /* Skip */ }
                }

                // Preferences
                if (selectionConfig.restorePreferences) {
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
                            persistentDailyDashboardEnabled = prefs.persistentDailyDashboardEnabled,
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
                }

                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
