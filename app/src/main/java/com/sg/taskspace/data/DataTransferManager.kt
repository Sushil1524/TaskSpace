package com.sg.taskspace.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val tasks: List<Task>,
    val userPreferences: UserPreferences
)

class DataTransferManager(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportData(uri: Uri): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val tasks = taskRepository.getAllTasksSync() // Need to add this to Repo
                val prefs = userPreferencesRepository.fetchInitialPreferences()
                val backupData = BackupData(tasks, prefs)
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

                // Restore Data
                // 1. Clear existing tasks? Or merge?
                // Usually restore implies replacing or merging. Let's replace for simplicity/cleanliness, or maybe just add.
                // User said "restore", which usually means "bring back state".
                // I'll clear and insert.
                // Wait, clearing might be dangerous.
                // Let's just upsert (insert with conflict replacement).
                // Our Dao insert is OnConflictStrategy.REPLACE usually.
                
                backupData.tasks.forEach { task ->
                    taskRepository.insertTask(task)
                }
                
                // Restore Prefs
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
