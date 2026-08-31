package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Goal
import com.sg.taskspace.data.GoalRepository
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.HabitRepository
import com.sg.taskspace.data.JournalEntry
import com.sg.taskspace.data.JournalRepository
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.ProjectRepository
import com.sg.taskspace.data.Task
import com.sg.taskspace.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TrashItem(val id: String, val title: String, val deletedAt: Long?, val typeName: String) {
    class TaskItem(val task: Task) : TrashItem(task.id, task.title, task.deletedAt, "Task")
    class HabitItem(val habit: Habit) : TrashItem(habit.id, habit.name, habit.deletedAt, "Habit")
    class JournalItem(val entry: JournalEntry) : TrashItem(entry.id, "Journal: ${entry.date}", entry.deletedAt, "Journal")
    class GoalItem(val goal: Goal) : TrashItem(goal.id, goal.title, goal.deletedAt, "Goal")
    class ProjectItem(val project: Project) : TrashItem(project.id, project.name, project.deletedAt, "Project")

    val daysRemaining: Int
        get() {
            if (deletedAt == null) return 30
            val elapsedMs = System.currentTimeMillis() - deletedAt
            val elapsedDays = (elapsedMs / (1000 * 60 * 60 * 24)).toInt()
            return (30 - elapsedDays).coerceAtLeast(0)
        }
}

class TrashViewModel(
    private val taskRepository: TaskRepository,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    init {
        purgeExpiredTrash()
    }

    val deletedTasks: StateFlow<List<Task>> = taskRepository.getDeletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedHabits: StateFlow<List<Habit>> = habitRepository.getDeletedHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedJournalEntries: StateFlow<List<JournalEntry>> = journalRepository.getDeletedJournalEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedGoals: StateFlow<List<Goal>> = goalRepository.getDeletedGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedProjects: StateFlow<List<Project>> = projectRepository.getDeletedProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrashItems: StateFlow<List<TrashItem>> = combine(
        deletedTasks,
        deletedHabits,
        deletedJournalEntries,
        deletedGoals,
        deletedProjects
    ) { tasks, habits, journal, goals, projects ->
        val items = mutableListOf<TrashItem>()
        tasks.forEach { items.add(TrashItem.TaskItem(it)) }
        habits.forEach { items.add(TrashItem.HabitItem(it)) }
        journal.forEach { items.add(TrashItem.JournalItem(it)) }
        goals.forEach { items.add(TrashItem.GoalItem(it)) }
        projects.forEach { items.add(TrashItem.ProjectItem(it)) }
        items.sortedByDescending { it.deletedAt ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Auto-purge items soft-deleted more than 30 days ago */
    fun purgeExpiredTrash() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            taskRepository.purgeExpiredTasks(thirtyDaysAgo)
            journalRepository.purgeExpiredJournalEntries(thirtyDaysAgo)
            habitRepository.purgeExpiredHabits(thirtyDaysAgo)
            goalRepository.purgeExpiredGoals(thirtyDaysAgo)
            projectRepository.purgeExpiredProjects(thirtyDaysAgo)
        }
    }

    fun restoreItem(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TaskItem -> taskRepository.restoreTask(item.id)
                is TrashItem.HabitItem -> habitRepository.restoreHabit(item.id)
                is TrashItem.JournalItem -> journalRepository.restoreJournalEntry(item.id)
                is TrashItem.GoalItem -> goalRepository.restoreGoal(item.id)
                is TrashItem.ProjectItem -> projectRepository.restoreProject(item.id)
            }
        }
    }

    fun deleteItemPermanently(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TaskItem -> taskRepository.deleteTaskById(item.id)
                is TrashItem.HabitItem -> habitRepository.deleteHabitById(item.id)
                is TrashItem.JournalItem -> journalRepository.deleteJournalEntryById(item.id)
                is TrashItem.GoalItem -> goalRepository.deleteGoalById(item.id)
                is TrashItem.ProjectItem -> projectRepository.deleteProjectById(item.id)
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            taskRepository.emptyTrashTasks()
            journalRepository.emptyTrashJournalEntries()
            habitRepository.emptyTrashHabits()
            goalRepository.emptyTrashGoals()
            projectRepository.emptyTrashProjects()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TaskSpaceApplication
                return TrashViewModel(
                    application.container.taskRepository,
                    application.container.journalRepository,
                    application.container.habitRepository,
                    application.container.goalRepository,
                    application.container.projectRepository
                ) as T
            }
        }
    }
}
