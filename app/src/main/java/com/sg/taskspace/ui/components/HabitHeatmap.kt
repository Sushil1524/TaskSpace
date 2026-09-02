package com.sg.taskspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.SecondaryEmerald
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class HeatmapViewMode {
    WEEKS_12,
    MONTHLY_CALENDAR,
    ANNUAL_YEAR
}

@Composable
fun HabitHeatmap(
    completedDatesCountMap: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(HeatmapViewMode.WEEKS_12) }
    val today = remember { LocalDate.now() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Habit Consistency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (viewMode) {
                            HeatmapViewMode.WEEKS_12 -> "Last 12 Weeks"
                            HeatmapViewMode.MONTHLY_CALENDAR -> today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                            HeatmapViewMode.ANNUAL_YEAR -> "Full Year (${today.year})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // View Mode Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = viewMode == HeatmapViewMode.WEEKS_12,
                        onClick = { viewMode = HeatmapViewMode.WEEKS_12 },
                        label = { Text("3M", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = viewMode == HeatmapViewMode.MONTHLY_CALENDAR,
                        onClick = { viewMode = HeatmapViewMode.MONTHLY_CALENDAR },
                        label = { Text("Month", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = viewMode == HeatmapViewMode.ANNUAL_YEAR,
                        onClick = { viewMode = HeatmapViewMode.ANNUAL_YEAR },
                        label = { Text("Year", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isMonochrome = MaterialTheme.colorScheme.primary == Color.White || MaterialTheme.colorScheme.primary == Color.Black
            val activeColor = if (isMonochrome) Color(0xFFE4E4E7) else SecondaryEmerald
            val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            val futureColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

            when (viewMode) {
                HeatmapViewMode.WEEKS_12, HeatmapViewMode.ANNUAL_YEAR -> {
                    val weeksCount = if (viewMode == HeatmapViewMode.WEEKS_12) 12 else 52
                    val daysToSubtract = remember { today.dayOfWeek.value % 7 }
                    val endOfCurrentWeek = remember { today.plusDays((6 - daysToSubtract).toLong()) }
                    val startDate = remember(weeksCount) { endOfCurrentWeek.minusWeeks(weeksCount.toLong() - 1).minusDays(6) }

                    val grid = remember(completedDatesCountMap, startDate, weeksCount) {
                        val weeks = mutableListOf<List<Pair<LocalDate, Int>>>()
                        var current = startDate
                        for (w in 0 until weeksCount) {
                            val weekDays = mutableListOf<Pair<LocalDate, Int>>()
                            for (d in 0 until 7) {
                                val dateIso = current.format(DateTimeFormatter.ISO_DATE)
                                val count = completedDatesCountMap[dateIso] ?: 0
                                weekDays.add(current to count)
                                current = current.plusDays(1)
                            }
                            weeks.add(weekDays)
                        }
                        weeks
                    }

                    val maxCount = remember(completedDatesCountMap) {
                        (completedDatesCountMap.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(grid.size) { weekIdx ->
                            val weekDays = grid[weekIdx]
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                weekDays.forEach { (date, count) ->
                                    val isFuture = date > today
                                    val level = remember(count, maxCount) {
                                        getHeatmapLevel(count, maxCount)
                                    }

                                    HeatmapCell(
                                        level = level,
                                        isFuture = isFuture,
                                        isMonochrome = isMonochrome,
                                        activeColor = activeColor,
                                        emptyColor = emptyColor,
                                        futureColor = futureColor,
                                        cellSize = if (weeksCount == 52) 11.dp else 16.dp
                                    )
                                }
                            }
                        }
                    }
                }

                HeatmapViewMode.MONTHLY_CALENDAR -> {
                    MonthlyCalendarView(
                        completedDatesCountMap = completedDatesCountMap,
                        activeColor = activeColor,
                        emptyColor = emptyColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5-Shade Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less ", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (0..4).forEach { lvl ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(11.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                    ) {
                        HeatmapCell(
                            level = lvl,
                            isFuture = false,
                            isMonochrome = isMonochrome,
                            activeColor = activeColor,
                            emptyColor = emptyColor,
                            futureColor = futureColor,
                            cellSize = 11.dp
                        )
                    }
                }
                Text(" More", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MonthlyCalendarView(
    completedDatesCountMap: Map<String, Int>,
    activeColor: Color,
    emptyColor: Color
) {
    val currentMonth = remember { YearMonth.now() }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7 // Monday = 0

    val maxCount = remember(completedDatesCountMap) {
        (completedDatesCountMap.values.maxOrNull() ?: 1).coerceAtLeast(1)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Day Headers (Mon - Sun)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Days
        val totalCells = firstDayOfWeekOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - firstDayOfWeekOffset + 1

                    if (dayNum in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNum)
                        val dateIso = date.format(DateTimeFormatter.ISO_DATE)
                        val count = completedDatesCountMap[dateIso] ?: 0
                        val isFuture = date > LocalDate.now()
                        val level = getHeatmapLevel(count, maxCount)

                        val cellBg = when {
                            isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            level == 0 -> emptyColor
                            else -> activeColor.copy(alpha = (level * 0.22f + 0.12f).coerceIn(0.35f, 1f))
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (count > 0 && level >= 3) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

private fun getHeatmapLevel(count: Int, maxCount: Int): Int {
    if (count <= 0) return 0
    if (maxCount <= 4) {
        return count.coerceAtMost(4)
    }
    val ratio = count.toFloat() / maxCount
    return when {
        ratio <= 0.25f -> 1
        ratio <= 0.50f -> 2
        ratio <= 0.75f -> 3
        else -> 4
    }
}

@Composable
private fun HeatmapCell(
    level: Int,
    isFuture: Boolean,
    isMonochrome: Boolean,
    activeColor: Color,
    emptyColor: Color,
    futureColor: Color,
    cellSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    val baseColor = when {
        isFuture -> futureColor
        else -> emptyColor
    }

    val overlayColor = when {
        isFuture || level == 0 -> Color.Transparent
        isMonochrome -> when (level) {
            1 -> Color(0xFF52525B)
            2 -> Color(0xFFA1A1AA)
            3 -> Color(0xFFE4E4E7)
            4 -> Color(0xFFFFFFFF)
            else -> Color(0xFFFFFFFF)
        }
        else -> when (level) {
            1 -> activeColor.copy(alpha = 0.35f)
            2 -> activeColor.copy(alpha = 0.60f)
            3 -> activeColor.copy(alpha = 0.82f)
            4 -> activeColor.copy(alpha = 1.00f)
            else -> activeColor
        }
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(3.dp))
            .background(baseColor)
    ) {
        if (overlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }
    }
}
