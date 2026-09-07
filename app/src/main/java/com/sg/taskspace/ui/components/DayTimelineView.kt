package com.sg.taskspace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityMedium

@Composable
fun DayTimelineView(
    tasks: List<Task>,
    projectsMap: Map<String, Project>,
    onTaskClick: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val timedTasks = remember(tasks) {
        tasks.filter { !it.time.isNullOrBlank() }
            .sortedBy { it.time }
    }

    val anytimeTasks = remember(tasks) {
        tasks.filter { it.time.isNullOrBlank() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scheduled Timed Tasks Section
        if (timedTasks.isNotEmpty()) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            timedTasks.forEachIndexed { index, task ->
                TimelineTaskItem(
                    task = task,
                    projectName = task.projectId?.let { projectsMap[it]?.name },
                    isLast = index == timedTasks.lastIndex && anytimeTasks.isEmpty(),
                    onClick = { onTaskClick(task) },
                    onToggle = { onToggleTask(task) }
                )
            }
        }

        // Anytime / Unscheduled Tasks Section
        if (anytimeTasks.isNotEmpty()) {
            if (timedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "Anytime Today (${anytimeTasks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            anytimeTasks.forEach { task ->
                TimelineTaskItem(
                    task = task,
                    projectName = task.projectId?.let { projectsMap[it]?.name },
                    isLast = true,
                    isAnytime = true,
                    onClick = { onTaskClick(task) },
                    onToggle = { onToggleTask(task) }
                )
            }
        }
    }
}

@Composable
private fun TimelineTaskItem(
    task: Task,
    projectName: String?,
    isLast: Boolean,
    isAnytime: Boolean = false,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "High" -> PriorityHigh
        "Medium" -> PriorityMedium
        else -> PriorityLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Time Column & Node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(68.dp)
        ) {
            Text(
                text = if (isAnytime) "Anytime" else (task.time ?: "--:--"),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAnytime) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) Color(0xFF10B981) else priorityColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(54.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }

        // Task Content Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.estimatedMinutes > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${task.estimatedMinutes}m", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!projectName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(projectName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (task.tags.isNotBlank()) {
                            val firstTag = task.tags.split(",").firstOrNull()?.trim()
                            if (!firstTag.isNullOrBlank()) {
                                Text(
                                    text = if (firstTag.startsWith("#")) firstTag else "#$firstTag",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
