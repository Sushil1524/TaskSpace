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

object ReportExporter {

    fun generateMarkdownReport(
        month: YearMonth,
        insightsData: TaskViewModel.InsightsData?,
        tasks: List<Task>,
        habits: List<Habit>,
        habitStats: Map<String, HabitViewModel.HabitStats>,
        projects: List<Project>
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

        builder.appendLine("---")
        builder.appendLine("*Report exported from TaskSpace*")

        return builder.toString()
    }

    fun generateCsvReport(
        tasks: List<Task>,
        habits: List<Habit>,
        habitStats: Map<String, HabitViewModel.HabitStats>
    ): String {
        val builder = StringBuilder()

        builder.appendLine("=== TASKS ===")
        builder.appendLine("Title,Category,Priority,DueDate,Time,EstimatedMinutes,Completed")
        tasks.forEach { task ->
            val cleanTitle = task.title.replace("\"", "\"\"")
            val cleanCat = task.category.replace("\"", "\"\"")
            builder.appendLine("\"$cleanTitle\",\"$cleanCat\",${task.priority},\"${task.createdForDate}\",\"${task.time ?: ""}\",${task.estimatedMinutes},${task.isCompleted}")
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
