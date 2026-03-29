package com.sg.taskspace.ui.viewmodel

import com.sg.taskspace.data.Goal
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.HabitLog
import com.sg.taskspace.data.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementCalculatorTest {

    @Test
    fun calculate_emptyState_hasNoUnlocks() {
        val result = AchievementCalculator.calculate(
            tasks = emptyList(),
            habits = emptyList(),
            habitLogs = emptyList(),
            goals = emptyList(),
            today = LocalDate.of(2026, 3, 25)
        )

        assertEquals(0, result.summary.unlockedCount)
        assertEquals(9, result.summary.totalCount)
        assertEquals(0, result.summary.currentStreak)
        assertFalse(result.inProgressAchievements.isEmpty())
    }

    @Test
    fun calculate_unlocksThresholdAchievementsExactly() {
        val tasks = (1..10).map { day ->
            completedTask("2026-03-${day.toString().padStart(2, '0')}")
        }
        val habit = Habit(name = "Read")
        val habitLogs = (1..7).map { day ->
            HabitLog(
                habitId = habit.id,
                date = "2026-03-${day.toString().padStart(2, '0')}",
                isCompleted = true
            )
        }
        val goals = listOf(
            Goal(title = "Ship feature", deadlineDate = "2026-03-30", isCompleted = true)
        )

        val result = AchievementCalculator.calculate(
            tasks = tasks,
            habits = listOf(habit),
            habitLogs = habitLogs,
            goals = goals,
            today = LocalDate.of(2026, 3, 10)
        )

        val unlockedIds = result.unlockedAchievements.map { it.definition.id }.toSet()

        assertTrue(unlockedIds.contains("task_first"))
        assertTrue(unlockedIds.contains("task_10"))
        assertTrue(unlockedIds.contains("habit_created"))
        assertTrue(unlockedIds.contains("habit_7"))
        assertTrue(unlockedIds.contains("goal_created"))
        assertTrue(unlockedIds.contains("goal_completed"))
        assertEquals(10, result.summary.currentStreak)
    }

    @Test
    fun computeCurrentStreak_stopsWhenDayIsMissing() {
        val tasks = listOf(
            completedTask("2026-03-25"),
            completedTask("2026-03-24"),
            completedTask("2026-03-22")
        )

        val streak = AchievementCalculator.computeCurrentStreak(
            tasks = tasks,
            today = LocalDate.of(2026, 3, 25)
        )

        assertEquals(2, streak)
    }

    @Test
    fun calculate_picksMostAdvancedLockedAchievementAsNext() {
        val tasks = listOf(
            completedTask("2026-03-24"),
            completedTask("2026-03-25")
        )

        val result = AchievementCalculator.calculate(
            tasks = tasks,
            habits = emptyList(),
            habitLogs = emptyList(),
            goals = emptyList(),
            today = LocalDate.of(2026, 3, 25)
        )

        assertEquals("streak_3", result.nextAchievement?.definition?.id)
    }

    private fun completedTask(date: String): Task {
        return Task(
            title = "Done",
            createdForDate = date,
            isCompleted = true
        )
    }
}
