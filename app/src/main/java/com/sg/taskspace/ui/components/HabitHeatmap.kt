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

            // 12-week scrollable grid
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(grid.size) { weekIdx ->
                    val weekDays = grid[weekIdx]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEach { (date, count) ->
                            val intensity = if (count == 0) 0f else (count.toFloat() / maxCount).coerceIn(0.2f, 1f)
                            val cellColor = when {
                                date > today -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                count == 0 -> MaterialTheme.colorScheme.surface
                                else -> SecondaryEmerald.copy(alpha = intensity)
                            }

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less ", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0f, 0.3f, 0.6f, 1.0f).forEach { alpha ->
                    val color = if (alpha == 0f) MaterialTheme.colorScheme.surface else SecondaryEmerald.copy(alpha = alpha)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                Text(" More", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
