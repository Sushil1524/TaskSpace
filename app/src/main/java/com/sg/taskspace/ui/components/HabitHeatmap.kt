package com.sg.taskspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.SecondaryEmerald
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HabitHeatmap(
    completedDatesCountMap: Map<String, Int>,
    modifier: Modifier = Modifier,
    weeksCount: Int = 12
) {
    val today = remember { LocalDate.now() }
    val daysToSubtract = remember { today.dayOfWeek.value % 7 }
    val endOfCurrentWeek = remember { today.plusDays((6 - daysToSubtract).toLong()) }
    val startDate = remember { endOfCurrentWeek.minusWeeks(weeksCount.toLong() - 1).minusDays(6) }

    // Generate grid: weeksCount columns, each column has 7 days (Sun -> Sat)
    val grid = remember(completedDatesCountMap, startDate) {
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habit Consistency Heatmap",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Last 12 Weeks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isMonochrome = MaterialTheme.colorScheme.primary == Color.White || MaterialTheme.colorScheme.primary == Color.Black
            val activeColor = if (isMonochrome) Color(0xFFE4E4E7) else SecondaryEmerald
            val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            val futureColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

            // 12-week scrollable grid
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
                                futureColor = futureColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5-Shade GitHub-Style Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less ",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 5 shades: Level 0 (no activity), 1 (low), 2 (moderate), 3 (high), 4 (max)
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

                Text(
                    text = " More",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            1 -> Color(0xFF52525B) // Zinc-600
            2 -> Color(0xFFA1A1AA) // Zinc-400
            3 -> Color(0xFFE4E4E7) // Zinc-200
            4 -> Color(0xFFFFFFFF) // White
            else -> Color(0xFFFFFFFF)
        }
        else -> when (level) {
            1 -> activeColor.copy(alpha = 0.35f) // Level 1 (Low activity)
            2 -> activeColor.copy(alpha = 0.60f) // Level 2 (Moderate activity)
            3 -> activeColor.copy(alpha = 0.82f) // Level 3 (High activity)
            4 -> activeColor.copy(alpha = 1.00f) // Level 4 (Max activity)
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
