package com.sg.taskspace

import com.sg.taskspace.data.Task
import com.sg.taskspace.util.ReportExporter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ReportExporterTest {

    @Test
    fun generateMarkdownReport_includesOverdueTasksWhenPresent() {
        val overdueTasks = listOf(
            Task(
                id = "1",
                title = "Submit tax form",
                createdForDate = "2026-10-20",
                priority = "High",
                tags = "#finance, #urgent"
            ),
            Task(
                id = "2",
                title = "Renew domain",
                createdForDate = "2026-10-22",
                priority = "Medium",
                tags = ""
            )
        )

        val report = ReportExporter.generateMarkdownReport(
            month = YearMonth.of(2026, 10),
            insightsData = null,
            tasks = emptyList(),
            habits = emptyList(),
            habitStats = emptyMap(),
            projects = emptyList(),
            overdueTasks = overdueTasks
        )

        assertTrue(report.contains("## Overdue Tasks (2)"))
        assertTrue(report.contains("Submit tax form"))
        assertTrue(report.contains("2026-10-20"))
        assertTrue(report.contains("High"))
        assertTrue(report.contains("#finance, #urgent"))
        assertTrue(report.contains("Renew domain"))
    }
}

