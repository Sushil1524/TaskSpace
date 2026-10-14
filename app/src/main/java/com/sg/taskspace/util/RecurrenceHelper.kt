package com.sg.taskspace.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object RecurrenceHelper {

    val DAYS_OF_WEEK = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    fun parseDayOfWeek(dayName: String?): DayOfWeek? {
        if (dayName.isNullOrBlank()) return null
        return try {
            DayOfWeek.valueOf(dayName.trim().uppercase(Locale.US))
        } catch (_: Exception) {
            when (dayName.trim().take(3).lowercase(Locale.US)) {
                "mon" -> DayOfWeek.MONDAY
                "tue" -> DayOfWeek.TUESDAY
                "wed" -> DayOfWeek.WEDNESDAY
                "thu" -> DayOfWeek.THURSDAY
                "fri" -> DayOfWeek.FRIDAY
                "sat" -> DayOfWeek.SATURDAY
                "sun" -> DayOfWeek.SUNDAY
                else -> null
            }
        }
    }

    fun getDisplayName(dayOfWeek: DayOfWeek): String {
        return dayOfWeek.name.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
    }

    fun getShortDisplayName(dayOfWeek: DayOfWeek): String {
        return dayOfWeek.name.take(3).lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
    }

    /**
     * Calculates the next [count] upcoming dates on or after [startDate] matching [repeat] and [repeatDayOfWeek].
     * If [startDate] is before today, calculations start from today so upcoming occurrences are strictly in the future.
     */
    fun calculateNextOccurrences(
        startDate: LocalDate,
        repeat: String,
        repeatDayOfWeek: String?,
        count: Int = 3,
        fromToday: Boolean = true
    ): List<LocalDate> {
        if (repeat == "None" || count <= 0) return emptyList()

        val today = LocalDate.now()
        val baseDate = if (fromToday) {
            if (startDate.isAfter(today)) startDate else today
        } else {
            startDate
        }

        return when (repeat) {
            "Daily" -> {
                (0 until count).map { baseDate.plusDays(it.toLong()) }
            }
            "Weekly" -> {
                val targetDow = parseDayOfWeek(repeatDayOfWeek) ?: baseDate.dayOfWeek
                val firstOccurrence = if (baseDate.dayOfWeek == targetDow) {
                    baseDate
                } else {
                    baseDate.with(TemporalAdjusters.next(targetDow))
                }
                (0 until count).map { firstOccurrence.plusWeeks(it.toLong()) }
            }
            else -> emptyList()
        }
    }

    /**
     * Formats a list of dates into a human-readable list string e.g. "Mon Oct 19, Mon Oct 26, Mon Nov 2".
     */
    fun formatOccurrencePreview(dates: List<LocalDate>): String {
        val formatter = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)
        return dates.joinToString(", ") { it.format(formatter) }
    }

    fun formatOccurrenceDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
        return date.format(formatter)
    }

    fun formatRelativeBadge(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days in 2L..6L -> "In ${days}d"
            days in 7L..13L -> "In 1w"
            days >= 14L -> "In ${days / 7}w"
            days == -1L -> "Yesterday"
            else -> "${-days}d ago"
        }
    }

    fun formatRecurrenceRule(repeat: String, repeatDayOfWeek: String?): String {
        return when (repeat) {
            "Daily" -> "Repeats daily"
            "Weekly" -> {
                val day = repeatDayOfWeek?.let { parseDayOfWeek(it)?.let { d -> getDisplayName(d) } } ?: "selected day"
                "Repeats weekly on ${day}s"
            }
            else -> "Does not repeat"
        }
    }
}
