package com.sg.taskspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

import com.sg.taskspace.ui.viewmodel.TaskViewModel

@Composable
fun WeeklyProgressRow(
    selectedDate: LocalDate,
    weeklyStats: List<TaskViewModel.DayStats>,
    onDateSelected: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Use passed stats or generate empty ones if loading/empty
        val displayStats = weeklyStats.ifEmpty {
            val current = LocalDate.now()
            val start = current.minusDays((current.dayOfWeek.value - 1).toLong())
            (0..6).map {
                TaskViewModel.DayStats(start.plusDays(it.toLong()), 0, 0)
            }
        }

        displayStats.forEach { stat ->
            val isSelected = stat.date == selectedDate
            val dayName = stat.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase()
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { 
                        onDateSelected(stat.date)
                        onDayClick(stat.date)
                    }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stat.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Task Count 0/0
                Text(
                    text = "${stat.completedTasks}/${stat.totalTasks}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (stat.totalTasks > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                // Indicator dot/dash
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(
                            if (stat.totalTasks > 0) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, 
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
