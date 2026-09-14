package com.sg.taskspace.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val PATTERNS = listOf(
        "hh:mm a",
        "h:mm a",
        "hh:mma",
        "h:mma",
        "HH:mm",
        "H:mm"
    )

    private val TIME_FORMATTERS = buildList {
        // System default locale formatters (case-insensitive)
        for (pattern in PATTERNS) {
            add(
                java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.getDefault())
            )
        }
        // Universal US ASCII locale formatters (case-insensitive fallback)
        for (pattern in PATTERNS) {
            add(
                java.time.format.DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.US)
            )
        }
        add(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    fun parseLocalTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        val clean = timeStr.trim()
        for (formatter in TIME_FORMATTERS) {
            try {
                return LocalTime.parse(clean, formatter)
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Converts a time string to total minutes from midnight for sorting (0..1439).
     * Returns Int.MAX_VALUE if time is null or unparseable.
     */
    fun timeToMinutes(timeStr: String?): Int {
        val parsed = parseLocalTime(timeStr) ?: return Int.MAX_VALUE
        return parsed.hour * 60 + parsed.minute
    }
}
