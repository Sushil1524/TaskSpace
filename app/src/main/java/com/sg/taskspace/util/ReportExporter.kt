package com.sg.taskspace.util

import android.content.Context
import android.content.Intent
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.viewmodel.HabitViewModel
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

import com.sg.taskspace.data.JournalEntry
import com.sg.taskspace.ui.viewmodel.AchievementProgress
import com.sg.taskspace.ui.viewmodel.AchievementsUiState
import com.sg.taskspace.ui.viewmodel.JournalViewModel

object ReportExporter {

    fun generateMarkdownReport(
        month: YearMonth,
        insightsData: TaskViewModel.InsightsData?,
        tasks: List<Task>,
        habits: List<Habit>,
        habitStats: Map<String, HabitViewModel.HabitStats>,
        projects: List<Project>,
        journalEntries: List<JournalEntry> = emptyList(),
        journalStats: JournalViewModel.JournalStats? = null,
        achievementsUiState: AchievementsUiState? = null
    ): String {
        val monthTitle = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        val builder = StringBuilder()

        builder.appendLine("# TaskSpace Productivity Report — $monthTitle")
        builder.appendLine("Generated on: ${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}")
        builder.appendLine()

        // 1. Task Summary
        builder.appendLine("## Task Overview")
        if (insightsData != null) {
            builder.appendLine("- Total Tasks: ${insightsData.totalTasksThisMonth}")
            builder.appendLine("- Completed Tasks: ${insightsData.completedTasksThisMonth}")
            builder.appendLine("- Completion Rate: ${insightsData.completionRateThisMonth}%")
            builder.appendLine("- Current Task Streak: ${insightsData.currentStreak} days")
            builder.appendLine("- Best Day of Week: ${insightsData.bestDayOfWeek}")
            builder.appendLine("- Total Planned Workload: ${insightsData.totalEstimatedMinutes / 60}h ${insightsData.totalEstimatedMinutes % 60}m")
            builder.appendLine("- Completed Workload: ${insightsData.completedEstimatedMinutes / 60}h ${insightsData.completedEstimatedMinutes % 60}m")
            builder.appendLine("- Total Focus Time Logged: ${insightsData.totalTrackedMinutes / 60}h ${insightsData.totalTrackedMinutes % 60}m")
            builder.appendLine("- Focus on Completed Tasks: ${insightsData.completedTrackedMinutes / 60}h ${insightsData.completedTrackedMinutes % 60}m")
            builder.appendLine("- Daily Focus Average: ${insightsData.avgDailyFocusMinutes}m/day")
            if (insightsData.topFocusedTasks.isNotEmpty()) {
                builder.appendLine("- Top Focused Task: '${insightsData.topFocusedTasks.first().title}' (${insightsData.topFocusedTasks.first().trackedMinutes}m)")
            }
        } else {
            builder.appendLine("No task data recorded for this month.")
        }
        builder.appendLine()

        // 2. Category Breakdown
        if (insightsData != null && insightsData.categoryDistribution.isNotEmpty()) {
            builder.appendLine("## Category Breakdown")
            builder.appendLine("| Category | Total Tasks | Completed Tasks | Rate |")
            builder.appendLine("| --- | --- | --- | --- |")
            insightsData.categoryDistribution.forEach { (cat, stats) ->
                val (total, comp) = stats
                val rate = if (total > 0) (comp * 100) / total else 0
                val catName = cat.ifEmpty { "Uncategorized" }
                builder.appendLine("| $catName | $total | $comp | $rate% |")
            }
            builder.appendLine()
        }

        // 3. Projects Progress
        if (projects.isNotEmpty()) {
            builder.appendLine("## Projects Progress")
            builder.appendLine("| Project | Total Tasks | Completed | Progress |")
            builder.appendLine("| --- | --- | --- | --- |")
            projects.forEach { proj ->
                val (total, comp) = insightsData?.projectDistribution?.get(proj.id) ?: Pair(0, 0)
                val progress = if (total > 0) (comp * 100) / total else 0
                builder.appendLine("| ${proj.name} | $total | $comp | $progress% |")
            }
            builder.appendLine()
        }

        // 4. Habits Consistency & Strength
        if (habits.isNotEmpty()) {
            builder.appendLine("## Habit Consistency & Strength")
            builder.appendLine("| Habit | Type | Current Streak | Max Streak | Habit Strength |")
            builder.appendLine("| --- | --- | --- | --- | --- |")
            habits.forEach { habit ->
                val stat = habitStats[habit.id]
                val streak = stat?.currentStreak ?: 0
                val maxStreak = stat?.maxStreak ?: 0
                val strength = stat?.habitStrength ?: 0
                val typeName = when (habit.type) {
                    "QUIT" -> "Quit"
                    "NUMERIC" -> "Target"
                    else -> "Build"
                }
                builder.appendLine("| ${habit.name} | $typeName | ${streak}d | ${maxStreak}d | $strength% |")
            }
            builder.appendLine()
        }

        // 5. Journal & Mindfulness Overview
        if (journalStats != null && journalStats.totalEntriesThisMonth > 0) {
            builder.appendLine("## Journal & Mood Reflection")
            builder.appendLine("- Total Journal Entries: ${journalStats.totalEntriesThisMonth}")
            builder.appendLine("- Total Words Written: ${journalStats.totalWordsThisMonth}")
            builder.appendLine("- Average Words / Entry: ${journalStats.avgWordsPerEntry}")
            builder.appendLine("- Journaling Streak: ${journalStats.currentStreak} days")
            builder.appendLine("- Dominant Mood: ${journalStats.dominantMoodEmoji} ${journalStats.dominantMood}")
            if (journalStats.avgMoodScore > 0f) {
                builder.appendLine("- Average Mood Score: ${String.format(java.util.Locale.US, "%.1f", journalStats.avgMoodScore)} / 5.0")
            }
            if (journalStats.moodDistribution.isNotEmpty()) {
                builder.appendLine()
                builder.appendLine("| Mood | Count |")
                builder.appendLine("| --- | --- |")
                journalStats.moodDistribution.filter { it.value > 0 }.forEach { (mood, count) ->
                    builder.appendLine("| $mood | $count |")
                }
            }
            builder.appendLine()
        }

        // 6. Achievements & Milestones
        if (achievementsUiState != null) {
            builder.appendLine("## Achievements & Milestones")
            val summary = achievementsUiState.summary
            builder.appendLine("- Badges Unlocked: ${summary.unlockedCount} / ${summary.totalCount} (${(summary.overallProgress * 100).toInt()}%)")
            builder.appendLine("- Total Badge XP Earned: ${summary.totalXpEarned} / ${summary.totalPossibleXp} XP")
            builder.appendLine()
            builder.appendLine("| Badge | Category | Status | Progress | XP Reward |")
            builder.appendLine("| --- | --- | --- | --- | --- |")
            achievementsUiState.allAchievements.forEach { ach ->
                val status = if (ach.unlocked) "Unlocked" else "In Progress"
                builder.appendLine("| ${ach.definition.iconEmoji} ${ach.definition.title} | ${ach.definition.category.displayName} | $status | ${ach.current}/${ach.definition.target} | +${ach.definition.xpReward} XP |")
            }
            builder.appendLine()
        }

        builder.appendLine("---")
        builder.appendLine("*Report exported from TaskSpace*")

        return builder.toString()
    }

    fun generateCsvReport(
        tasks: List<Task>,
        habits: List<Habit>,
        habitStats: Map<String, HabitViewModel.HabitStats>,
        journalEntries: List<JournalEntry> = emptyList(),
        achievements: List<AchievementProgress> = emptyList()
    ): String {
        val builder = StringBuilder()

        builder.appendLine("=== TASKS ===")
        builder.appendLine("Title,Category,Priority,DueDate,Time,EstimatedMinutes,TrackedMinutes,Completed")
        tasks.forEach { task ->
            val cleanTitle = task.title.replace("\"", "\"\"")
            val cleanCat = task.category.replace("\"", "\"\"")
            builder.appendLine("\"$cleanTitle\",\"$cleanCat\",${task.priority},\"${task.createdForDate}\",\"${task.time ?: ""}\",${task.estimatedMinutes},${task.trackedMinutes},${task.isCompleted}")
        }

        builder.appendLine()
        builder.appendLine("=== HABITS ===")
        builder.appendLine("HabitName,Type,ScheduleType,CurrentStreak,MaxStreak,HabitStrengthPercent")
        habits.forEach { habit ->
            val stat = habitStats[habit.id]
            val cleanName = habit.name.replace("\"", "\"\"")
            val streak = stat?.currentStreak ?: 0
            val maxStreak = stat?.maxStreak ?: 0
            val strength = stat?.habitStrength ?: 0
            builder.appendLine("\"$cleanName\",${habit.type},${habit.scheduleType},$streak,$maxStreak,$strength")
        }

        if (journalEntries.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== JOURNAL ENTRIES ===")
            builder.appendLine("Date,Mood,WordCount,ContentPreview")
            journalEntries.filter { !it.isDeleted }.forEach { entry ->
                val words = if (entry.content.isBlank()) 0 else entry.content.trim().split("\\s+".toRegex()).size
                val cleanContent = entry.content.replace("\"", "\"\"").replace("\n", " ").take(100)
                builder.appendLine("\"${entry.date}\",\"${entry.mood}\",$words,\"$cleanContent\"")
            }
        }

        if (achievements.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("=== ACHIEVEMENTS ===")
            builder.appendLine("Title,Category,Unlocked,Current,Target,XpReward")
            achievements.forEach { ach ->
                val cleanTitle = ach.definition.title.replace("\"", "\"\"")
                builder.appendLine("\"$cleanTitle\",${ach.definition.category.name},${ach.unlocked},${ach.current},${ach.definition.target},${ach.definition.xpReward}")
            }
        }

        return builder.toString()
    }

    fun shareReport(context: Context, content: String, title: String, mimeType: String = "text/plain") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = mimeType
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Summary Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
