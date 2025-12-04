package com.sg.taskspace.data

import android.content.Context

interface AppContainer {
    val taskRepository: TaskRepository
    val userPreferencesRepository: UserPreferencesRepository
    val dataTransferManager: DataTransferManager
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val taskRepository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getDatabase(context).taskDao())
    }
    
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val dataTransferManager: DataTransferManager by lazy {
        DataTransferManager(context, taskRepository, userPreferencesRepository)
    }
}
