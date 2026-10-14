package com.sg.taskspace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun OverdueSection(
    overdueTasks: List<Task>,
    projectsMap: Map<String, Project>,
    onMoveTaskToToday: (Task) -> Unit,
    onMoveAllToToday: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    swipeRightAction: String = "Complete",
    swipeLeftAction: String = "Delete",
    soundEffectsEnabled: Boolean = true,
    onFocusClick: ((Task) -> Unit)? = null,
    activeFocusTaskId: String? = null,
    isFocusRunning: Boolean = false,
    focusElapsedSeconds: Long = 0L,
    modifier: Modifier = Modifier
) {
    if (overdueTasks.isEmpty()) return

    var isExpanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevronRotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = "Overdue",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Overdue Tasks",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.height(18.dp)
                            ) {
                                Text(
                                    text = "${overdueTasks.size}",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (overdueTasks.size == 1) "1 task past due" else "${overdueTasks.size} tasks past due",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bulk Action: Move All to Today
                    FilledTonalButton(
                        onClick = onMoveAllToToday,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Move All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Chevron toggle
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
            }

            // Expanded List of Overdue Tasks
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    overdueTasks.forEach { task ->
                        TaskCard(
                            task = task,
                            onCheckedChange = { onToggleTask(task) },
                            onClick = { onTaskClick(task) },
                            onDelete = { onDeleteTask(task) },
                            onMoveToToday = { onMoveTaskToToday(task) },
                            onFocusClick = onFocusClick?.let { { it(task) } },
                            isFocusRunning = activeFocusTaskId == task.id && isFocusRunning,
                            isFocusActive = activeFocusTaskId == task.id,
                            focusElapsedSeconds = focusElapsedSeconds,
                            isOverdue = true,
                            overdueLabel = formatOverdueDays(task.createdForDate),
                            projectName = task.projectId?.let { projectsMap[it]?.name },
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            soundEffectsEnabled = soundEffectsEnabled
                        )
                    }
                }
            }
        }
    }
}

private fun formatOverdueDays(dateStr: String): String {
    return try {
        val taskDate = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(taskDate, today)
        when {
            days <= 0L -> "Due Today"
            days == 1L -> "1d overdue"
            days < 30L -> "${days}d overdue"
            else -> taskDate.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (_: Exception) {
        dateStr
    }
}
