package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sg.taskspace.data.AppDatabase
import com.sg.taskspace.data.Goal
import com.sg.taskspace.data.GoalRepository
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.HabitLog
import com.sg.taskspace.data.HabitRepository
import com.sg.taskspace.data.Task
import com.sg.taskspace.data.TaskRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AchievementCategory {
    Tasks,
    Streaks,
    Habits,
    Goals
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val category: AchievementCategory
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val current: Int,
    val unlocked: Boolean
) {
    val progressFraction: Float
        get() = if (definition.target == 0) {
            1f
        } else {
            (current.coerceAtMost(definition.target).toFloat() / definition.target).coerceIn(0f, 1f)
        }

    val progressLabel: String
        get() = "$current/${definition.target}"

    val remaining: Int
        get() = (definition.target - current).coerceAtLeast(0)
}

data class ProgressionSummary(
    val unlockedCount: Int,
    val totalCount: Int,
    val currentStreak: Int,
    val overallProgress: Float
)

data class AchievementsUiState(
    val summary: ProgressionSummary = ProgressionSummary(
        unlockedCount = 0,
        totalCount = 0,
        currentStreak = 0,
        overallProgress = 0f
    ),
    val nextAchievement: AchievementProgress? = null,
    val unlockedAchievements: List<AchievementProgress> = emptyList(),
    val inProgressAchievements: List<AchievementProgress> = emptyList()
)

object AchievementCalculator {
    private val definitions = listOf(
        AchievementDefinition(
            id = "task_first",
            title = "First Win",
            description = "Complete your first task.",
            target = 1,
            category = AchievementCategory.Tasks
        ),
        AchievementDefinition(
            id = "task_10",
            title = "Momentum Builder",
            description = "Complete 10 tasks.",
            target = 10,
            category = AchievementCategory.Tasks
        ),
        AchievementDefinition(
            id = "task_50",
            title = "Task Closer",
            description = "Complete 50 tasks.",
            target = 50,
            category = AchievementCategory.Tasks
        ),
        AchievementDefinition(
            id = "streak_3",
            title = "3-Day Streak",
            description = "Complete at least one task for 3 days in a row.",
            target = 3,
            category = AchievementCategory.Streaks
        ),
        AchievementDefinition(
            id = "streak_7",
            title = "7-Day Streak",
            description = "Complete at least one task for 7 days in a row.",
            target = 7,
            category = AchievementCategory.Streaks
        ),
        AchievementDefinition(
            id = "habit_created",
            title = "Habit Starter",
            description = "Create your first habit.",
            target = 1,
            category = AchievementCategory.Habits
        ),
        AchievementDefinition(
            id = "habit_7",
            title = "Habit Keeper",
            description = "Log 7 completed habits.",
            target = 7,
            category = AchievementCategory.Habits
        ),
        AchievementDefinition(
            id = "goal_created",
            title = "Vision Setter",
            description = "Create your first goal.",
            target = 1,
            category = AchievementCategory.Goals
        ),
        AchievementDefinition(
            id = "goal_completed",
            title = "Goal Crusher",
            description = "Complete your first goal.",
            target = 1,
            category = AchievementCategory.Goals
        )
    )

    fun calculate(
        tasks: List<Task>,
        habits: List<Habit>,
        habitLogs: List<HabitLog>,
        goals: List<Goal>,
        today: LocalDate = LocalDate.now()
    ): AchievementsUiState {
        val completedTasks = tasks.count { it.isCompleted }
        val currentStreak = computeCurrentStreak(tasks, today)
        val completedHabitLogs = habitLogs.count { it.isCompleted }
        val completedGoals = goals.count { it.isCompleted }

        val metrics = mapOf(
            "task_first" to completedTasks,
            "task_10" to completedTasks,
            "task_50" to completedTasks,
            "streak_3" to currentStreak,
            "streak_7" to currentStreak,
            "habit_created" to habits.size,
            "habit_7" to completedHabitLogs,
            "goal_created" to goals.size,
            "goal_completed" to completedGoals
        )

        val achievements = definitions.map { definition ->
            val current = metrics[definition.id] ?: 0
            AchievementProgress(
                definition = definition,
                current = current,
                unlocked = current >= definition.target
            )
        }

        val unlockedAchievements = achievements
            .filter { it.unlocked }
            .sortedBy { it.definition.target }

        val inProgressAchievements = achievements
            .filterNot { it.unlocked }
            .sortedWith(
                compareByDescending<AchievementProgress> { it.progressFraction }
                    .thenByDescending { it.current }
                    .thenBy { it.definition.target }
            )

        val unlockedCount = unlockedAchievements.size
        val totalCount = achievements.size

        return AchievementsUiState(
            summary = ProgressionSummary(
                unlockedCount = unlockedCount,
                totalCount = totalCount,
                currentStreak = currentStreak,
                overallProgress = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount
            ),
            nextAchievement = inProgressAchievements.firstOrNull(),
            unlockedAchievements = unlockedAchievements,
            inProgressAchievements = inProgressAchievements
        )
    }

    internal fun computeCurrentStreak(tasks: List<Task>, today: LocalDate): Int {
        val completedDates = tasks.asSequence()
            .filter { it.isCompleted }
            .mapNotNull { task ->
                runCatching {
                    LocalDate.parse(task.createdForDate, DateTimeFormatter.ISO_DATE)
                }.getOrNull()
            }
            .toSet()

        var streak = 0
        var currentDate = today

        while (completedDates.contains(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }
}

class AchievementsViewModel(
    taskRepository: TaskRepository,
    habitRepository: HabitRepository,
    goalRepository: GoalRepository
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = combine(
        taskRepository.getAllTasks(),
        habitRepository.allHabits,
        habitRepository.allHabitLogs,
        goalRepository.allGoals
    ) { tasks, habits, habitLogs, goals ->
        AchievementCalculator.calculate(
            tasks = tasks,
            habits = habits,
            habitLogs = habitLogs,
            goals = goals
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AchievementsUiState()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                return AchievementsViewModel(
                    taskRepository = TaskRepository(database.taskDao()),
                    habitRepository = HabitRepository(database.habitDao()),
                    goalRepository = GoalRepository(database.goalDao())
                ) as T
            }
        }
    }
}
