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

enum class AchievementCategory(val displayName: String, val iconEmoji: String) {
    Tasks("Tasks", "📋"),
    Streaks("Streaks", "🔥"),
    BuildHabits("Build Habits", "🌱"),
    QuitHabits("Quit Habits", "🛑"),
    Goals("Goals", "🎯"),
    Mastery("Mastery", "👑")
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val category: AchievementCategory,
    val iconEmoji: String = "🏆",
    val xpReward: Int = 50
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
    val overallProgress: Float,
    val totalXpEarned: Int = 0,
    val totalPossibleXp: Int = 0
)

data class AchievementsUiState(
    val summary: ProgressionSummary = ProgressionSummary(
        unlockedCount = 0,
        totalCount = 0,
        currentStreak = 0,
        overallProgress = 0f,
        totalXpEarned = 0,
        totalPossibleXp = 0
    ),
    val nextAchievement: AchievementProgress? = null,
    val unlockedAchievements: List<AchievementProgress> = emptyList(),
    val inProgressAchievements: List<AchievementProgress> = emptyList()
) {
    val allAchievements: List<AchievementProgress>
        get() = unlockedAchievements + inProgressAchievements
}

object AchievementCalculator {
    val definitions = listOf(
        // ── Tasks ──
        AchievementDefinition(
            id = "task_first",
            title = "First Win",
            description = "Complete your first task.",
            target = 1,
            category = AchievementCategory.Tasks,
            iconEmoji = "🎯",
            xpReward = 25
        ),
        AchievementDefinition(
            id = "task_10",
            title = "Momentum Builder",
            description = "Complete 10 tasks.",
            target = 10,
            category = AchievementCategory.Tasks,
            iconEmoji = "⚡",
            xpReward = 50
        ),
        AchievementDefinition(
            id = "task_50",
            title = "Task Closer",
            description = "Complete 50 tasks.",
            target = 50,
            category = AchievementCategory.Tasks,
            iconEmoji = "📋",
            xpReward = 100
        ),
        AchievementDefinition(
            id = "task_100",
            title = "Century Club",
            description = "Complete 100 tasks.",
            target = 100,
            category = AchievementCategory.Tasks,
            iconEmoji = "💯",
            xpReward = 250
        ),
        AchievementDefinition(
            id = "task_high",
            title = "Priority Fighter",
            description = "Complete 5 high priority tasks.",
            target = 5,
            category = AchievementCategory.Tasks,
            iconEmoji = "⚔️",
            xpReward = 50
        ),

        // ── Streaks ──
        AchievementDefinition(
            id = "streak_3",
            title = "3-Day Streak",
            description = "Complete at least one task for 3 days in a row.",
            target = 3,
            category = AchievementCategory.Streaks,
            iconEmoji = "🔥",
            xpReward = 25
        ),
        AchievementDefinition(
            id = "streak_7",
            title = "7-Day Streak",
            description = "Complete at least one task for 7 days in a row.",
            target = 7,
            category = AchievementCategory.Streaks,
            iconEmoji = "🌟",
            xpReward = 75
        ),
        AchievementDefinition(
            id = "streak_14",
            title = "Fortnight Warrior",
            description = "Maintain a 14-day task completion streak.",
            target = 14,
            category = AchievementCategory.Streaks,
            iconEmoji = "🛡️",
            xpReward = 150
        ),
        AchievementDefinition(
            id = "streak_30",
            title = "Monthly Titan",
            description = "Maintain a 30-day task completion streak.",
            target = 30,
            category = AchievementCategory.Streaks,
            iconEmoji = "👑",
            xpReward = 300
        ),

        // ── Build Habits ──
        AchievementDefinition(
            id = "habit_build_starter",
            title = "Foundation Layer",
            description = "Create your first positive habit to build.",
            target = 1,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "🌱",
            xpReward = 25
        ),
        AchievementDefinition(
            id = "habit_checkin_1",
            title = "First Step",
            description = "Log your first habit check-in.",
            target = 1,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "👣",
            xpReward = 25
        ),
        AchievementDefinition(
            id = "habit_checkin_10",
            title = "Routine in Motion",
            description = "Complete 10 habit check-ins.",
            target = 10,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "🔄",
            xpReward = 50
        ),
        AchievementDefinition(
            id = "habit_checkin_50",
            title = "Consistency Champion",
            description = "Complete 50 habit check-ins.",
            target = 50,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "🏆",
            xpReward = 100
        ),
        AchievementDefinition(
            id = "habit_checkin_100",
            title = "Habit Centurion",
            description = "Complete 100 habit check-ins.",
            target = 100,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "🎖️",
            xpReward = 250
        ),
        AchievementDefinition(
            id = "habit_streak_7",
            title = "Weekly Ritual",
            description = "Reach a 7-day streak on any build habit.",
            target = 7,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "⚡",
            xpReward = 75
        ),
        AchievementDefinition(
            id = "habit_streak_21",
            title = "21-Day Transformation",
            description = "Reach a 21-day streak to form a habit.",
            target = 21,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "🧬",
            xpReward = 150
        ),
        AchievementDefinition(
            id = "habit_streak_66",
            title = "Automaticity",
            description = "Reach a 66-day streak — the habit is now second nature.",
            target = 66,
            category = AchievementCategory.BuildHabits,
            iconEmoji = "💎",
            xpReward = 350
        ),

        // ── Quit Habits ("The Unquittables") ──
        AchievementDefinition(
            id = "habit_quit_starter",
            title = "Declaration of Freedom",
            description = "Start your first journey to quit an unwanted habit.",
            target = 1,
            category = AchievementCategory.QuitHabits,
            iconEmoji = "🛑",
            xpReward = 25
        ),
        AchievementDefinition(
            id = "habit_quit_3d",
            title = "Breaking the Chain",
            description = "Stay 3 days clean from a bad habit.",
            target = 3,
            category = AchievementCategory.QuitHabits,
            iconEmoji = "⛓️",
            xpReward = 50
        ),
        AchievementDefinition(
            id = "habit_quit_7d",
            title = "One Week Clean",
            description = "Reach 7 days clean from a bad habit.",
            target = 7,
            category = AchievementCategory.QuitHabits,
            iconEmoji = "🌿",
            xpReward = 100
        ),
        AchievementDefinition(
            id = "habit_quit_14d",
            title = "Fortified Will",
            description = "Reach 14 days clean — cravings begin to fade.",
            target = 14,
            category = AchievementCategory.QuitHabits,
            iconEmoji = "🏰",
            xpReward = 150
        ),
        AchievementDefinition(
            id = "habit_quit_30d",
            title = "The Unquittable",
            description = "Reach 30 days clean — conquering the impulse!",
            target = 30,
            category = AchievementCategory.QuitHabits,
            iconEmoji = "🦅",
            xpReward = 300
        ),
        AchievementDefinition(
            id = "habit_quit_90d",
            title = "Iron Will",
            description = "Reach 90 days clean — a complete lifestyle reboot.",
            target = 90,
            category = AchievementCategory.QuitHabits,
            iconEmoji = "🛡️",
            xpReward = 500
        ),

        // ── Mastery & Habit Architecture ──
        AchievementDefinition(
            id = "habit_architect",
            title = "Habit Architect",
            description = "Maintain 3 or more active habits simultaneously.",
            target = 3,
            category = AchievementCategory.Mastery,
            iconEmoji = "🏛️",
            xpReward = 75
        ),
        AchievementDefinition(
            id = "habit_dual_master",
            title = "Yin & Yang",
            description = "Actively build a good habit while quitting a bad one.",
            target = 1,
            category = AchievementCategory.Mastery,
            iconEmoji = "☯️",
            xpReward = 100
        ),

        // ── Goals ──
        AchievementDefinition(
            id = "goal_created",
            title = "Vision Setter",
            description = "Create your first goal.",
            target = 1,
            category = AchievementCategory.Goals,
            iconEmoji = "🎯",
            xpReward = 25
        ),
        AchievementDefinition(
            id = "goal_completed",
            title = "Goal Crusher",
            description = "Complete your first goal.",
            target = 1,
            category = AchievementCategory.Goals,
            iconEmoji = "🚀",
            xpReward = 75
        ),
        AchievementDefinition(
            id = "goal_3",
            title = "Master Planner",
            description = "Complete 3 goals.",
            target = 3,
            category = AchievementCategory.Goals,
            iconEmoji = "🏔️",
            xpReward = 150
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
        val highPriorityCompleted = tasks.count { it.isCompleted && it.priority == "High" }
        val currentStreak = computeCurrentStreak(tasks, today)
        val completedGoals = goals.count { it.isCompleted }

        // Habit categorization
        val activeHabits = habits.filter { !it.isDeleted }
        val buildHabits = activeHabits.filter { !it.type.equals("QUIT", ignoreCase = true) }
        val quitHabits = activeHabits.filter { it.type.equals("QUIT", ignoreCase = true) }

        val totalBuildCheckins = habitLogs.count { it.isCompleted || it.value > 0f }
        val maxBuildStreak = buildHabits.maxOfOrNull { computeMaxBuildStreak(it, habitLogs, today) } ?: 0
        val maxQuitDaysClean = quitHabits.maxOfOrNull { computeQuitDaysClean(it, habitLogs, today) } ?: 0
        val isDualMaster = if (buildHabits.isNotEmpty() && quitHabits.isNotEmpty()) 1 else 0

        val metrics = mapOf(
            // Tasks
            "task_first" to completedTasks,
            "task_10" to completedTasks,
            "task_50" to completedTasks,
            "task_100" to completedTasks,
            "task_high" to highPriorityCompleted,
            // Streaks
            "streak_3" to currentStreak,
            "streak_7" to currentStreak,
            "streak_14" to currentStreak,
            "streak_30" to currentStreak,
            // Build habits
            "habit_build_starter" to buildHabits.size,
            "habit_checkin_1" to totalBuildCheckins,
            "habit_checkin_10" to totalBuildCheckins,
            "habit_checkin_50" to totalBuildCheckins,
            "habit_checkin_100" to totalBuildCheckins,
            "habit_streak_7" to maxBuildStreak,
            "habit_streak_21" to maxBuildStreak,
            "habit_streak_66" to maxBuildStreak,
            // Quit habits
            "habit_quit_starter" to quitHabits.size,
            "habit_quit_3d" to maxQuitDaysClean,
            "habit_quit_7d" to maxQuitDaysClean,
            "habit_quit_14d" to maxQuitDaysClean,
            "habit_quit_30d" to maxQuitDaysClean,
            "habit_quit_90d" to maxQuitDaysClean,
            // Mastery
            "habit_architect" to activeHabits.size,
            "habit_dual_master" to isDualMaster,
            // Goals
            "goal_created" to goals.size,
            "goal_completed" to completedGoals,
            "goal_3" to completedGoals
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
        val totalXpEarned = unlockedAchievements.sumOf { it.definition.xpReward }
        val totalPossibleXp = definitions.sumOf { it.xpReward }

        return AchievementsUiState(
            summary = ProgressionSummary(
                unlockedCount = unlockedCount,
                totalCount = totalCount,
                currentStreak = currentStreak,
                overallProgress = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount,
                totalXpEarned = totalXpEarned,
                totalPossibleXp = totalPossibleXp
            ),
            nextAchievement = inProgressAchievements.firstOrNull(),
            unlockedAchievements = unlockedAchievements,
            inProgressAchievements = inProgressAchievements
        )
    }

    private fun computeMaxBuildStreak(habit: Habit, logs: List<HabitLog>, today: LocalDate): Int {
        val completedDates = logs.filter { it.habitId == habit.id && (if (habit.type == "NUMERIC") it.value >= habit.targetValue else it.isCompleted) }
            .mapNotNull { runCatching { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }.getOrNull() }
            .toSet()

        if (completedDates.isEmpty()) return 0

        var currentStreak = 0
        var checkDate = today
        if (!completedDates.contains(today)) {
            checkDate = today.minusDays(1)
        }
        while (completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }
        return currentStreak
    }

    private fun computeQuitDaysClean(habit: Habit, logs: List<HabitLog>, today: LocalDate): Int {
        val createdDate = runCatching {
            java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }.getOrDefault(today)

        val relapseDates = logs.filter { it.habitId == habit.id && (it.relapseNotes != null || !it.isCompleted) }
            .mapNotNull { runCatching { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }.getOrNull() }

        val lastRelapse = relapseDates.maxOrNull()
        return if (lastRelapse != null) {
            java.time.temporal.ChronoUnit.DAYS.between(lastRelapse, today).toInt().coerceAtLeast(0)
        } else {
            java.time.temporal.ChronoUnit.DAYS.between(createdDate, today).toInt().coerceAtLeast(0)
        }
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
    goalRepository: GoalRepository,
    userPreferencesRepository: com.sg.taskspace.data.UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<com.sg.taskspace.data.UserPreferences?> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
                    goalRepository = GoalRepository(database.goalDao()),
                    userPreferencesRepository = com.sg.taskspace.data.UserPreferencesRepository(application)
                ) as T
            }
        }
    }
}
