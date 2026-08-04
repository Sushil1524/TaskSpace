package com.sg.taskspace.data

import android.content.Context

interface AppContainer {
    val taskRepository: TaskRepository
    val userPreferencesRepository: UserPreferencesRepository
    val dataTransferManager: DataTransferManager
    val projectRepository: ProjectRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    private val journalRepository: JournalRepository by lazy {
        JournalRepository(AppDatabase.getDatabase(context).journalDao())
    }

    private val habitRepository: HabitRepository by lazy {
        HabitRepository(AppDatabase.getDatabase(context).habitDao())
    }

    private val goalRepository: GoalRepository by lazy {
        GoalRepository(AppDatabase.getDatabase(context).goalDao())
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getDatabase(context).taskDao())
    }

    override val projectRepository: ProjectRepository by lazy {
        ProjectRepository(AppDatabase.getDatabase(context).projectDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val dataTransferManager: DataTransferManager by lazy {
        DataTransferManager(
            context = context,
            taskRepository = taskRepository,
            journalRepository = journalRepository,
            habitRepository = habitRepository,
            goalRepository = goalRepository,
            userPreferencesRepository = userPreferencesRepository,
            projectRepository = projectRepository
        )
    }
}
