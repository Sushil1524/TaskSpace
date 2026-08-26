package com.sg.taskspace.ui.screens

import com.sg.taskspace.data.Task
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.sg.taskspace.ui.components.TaskCard
import com.sg.taskspace.ui.components.WeeklyProgressRow
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    projectViewModel: com.sg.taskspace.ui.viewmodel.ProjectViewModel? = null,
    onWeeklyTasksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHabitsClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onJournalClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    val tasks by viewModel.currentDisplayTasks.collectAsState()
    val projects by (projectViewModel?.projects?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val projectsMap = remember(projects) { projects.associateBy { it.id } }
    val formattedDate = viewModel.formattedDate
    val weekNumber = viewModel.weekNumber
    val selectedDate by viewModel.selectedDate.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val weeklyTasks by viewModel.weeklyTasks.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()

    var showFinishedSection by remember { mutableStateOf(true) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var selectedDayForDialog by remember { mutableStateOf<LocalDate?>(null) }

    // Task Detail Dialog
    if (selectedTask != null) {
        com.sg.taskspace.ui.components.TaskDetailDialog(
            task = selectedTask!!,
            projects = projects,
            onDismissRequest = { selectedTask = null },
            onUpdateTask = { updatedTask ->
                viewModel.updateTaskDetails(updatedTask)
                selectedTask = null
            },
            onDeleteTask = { taskToDelete ->
                viewModel.deleteTask(taskToDelete)
                selectedTask = null
            }
        )
    }

    // Day Details Dialog
    if (selectedDayForDialog != null) {
        val date = selectedDayForDialog!!
        val dateIso = date.format(java.time.format.DateTimeFormatter.ISO_DATE)
        val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        val dayTasks = weeklyTasks.filter { task ->
            val isSpecific = task.createdForDate == dateIso
            val isRepeating = task.repeat == "Daily" || (task.repeat == "Weekly" && task.repeatDayOfWeek == dayName)
            isSpecific || isRepeating
        }.filter { task ->
            if (task.repeat != "None") {
                weeklyTasks.none { it.parentId == task.id && it.createdForDate == dateIso }
            } else true
        }

        val stats = weeklyStats.find { it.date == date }

        com.sg.taskspace.ui.components.DayDetailsDialog(
            date = date,
            stats = stats,
            tasks = dayTasks,
            onDismissRequest = { selectedDayForDialog = null },
            onTaskClick = { task ->
                selectedDayForDialog = null
                selectedTask = task
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TaskSpace",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onAchievementsClick) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onInsightsClick) {
                            Icon(Icons.Default.BarChart, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weekNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Daily Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DAILY PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val total = tasks.size
                        val done = tasks.count { it.isCompleted }
                        val motivation = when {
                            total == 0 -> "No tasks yet. Add one!"
                            done == total -> "All tasks completed! Great job! 🎉"
                            else -> "You've completed $done out of $total tasks."
                        }
                        Text(
                            text = motivation,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val progress = if (total > 0) (done.toFloat() / total * 100).toInt() else 0
                            StatColumn("Total", total.toString())
                            StatColumn("Done", done.toString())
                            StatColumn("Progress", "$progress%")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val progressFraction = if (total > 0) done.toFloat() / total else 0f
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }

            // Quick Actions: Habits, Goals, Journal, Projects
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard("Habits", Icons.Default.Loop, Modifier.weight(1f), onHabitsClick)
                    QuickActionCard("Goals", Icons.Default.Flag, Modifier.weight(1f), onGoalsClick)
                    QuickActionCard("Journal", Icons.Default.Book, Modifier.weight(1f), onJournalClick)
                    QuickActionCard("Projects", Icons.Default.FolderOpen, Modifier.weight(1f), onProjectsClick)
                }
            }

            // Weekly Progress
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This Week's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    WeeklyProgressRow(
                        selectedDate = selectedDate,
                        weeklyStats = weeklyStats,
                        onDateSelected = { },
                        onDayClick = { date -> selectedDayForDialog = date }
                    )
                }
            }

            item {
                Button(
                    onClick = onWeeklyTasksClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Up Weekly Plan", style = MaterialTheme.typography.titleSmall)
                }
            }

            // Today's Tasks header
            item {
                Text(
                    text = "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Active + Finished sections
            val activeTasks = tasks.filter { !it.isCompleted }
            val completedTasks = tasks.filter { it.isCompleted }

            if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tasks for today.\nAdd some tasks to get started!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val swipeRightAction = userPreferences?.swipeRightAction ?: "Complete"
                val swipeLeftAction = userPreferences?.swipeLeftAction ?: "Delete"

                // Active tasks first
                items(activeTasks) { task ->
                    TaskCard(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                        onClick = { selectedTask = task },
                        onDelete = { viewModel.deleteTask(task) },
                        onSnooze = { viewModel.snoozeTaskToTomorrow(task) },
                        projectName = task.projectId?.let { projectsMap[it]?.name },
                        swipeRightAction = swipeRightAction,
                        swipeLeftAction = swipeLeftAction
                    )
                }

                // Finished section (collapsible)
                if (completedTasks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFinishedSection = !showFinishedSection }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Finished (${completedTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (showFinishedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showFinishedSection) {
                        items(completedTasks) { task ->
                            TaskCard(
                                task = task,
                                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                onClick = { selectedTask = task },
                                onDelete = { viewModel.deleteTask(task) },
                                onSnooze = { viewModel.snoozeTaskToTomorrow(task) },
                                projectName = task.projectId?.let { projectsMap[it]?.name },
                                swipeRightAction = swipeRightAction,
                                swipeLeftAction = swipeLeftAction
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
