package com.sg.taskspace.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
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
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.components.TaskCard
import com.sg.taskspace.ui.components.WeeklyProgressRow
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onWeeklyTasksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val tasks by viewModel.currentDisplayTasks.collectAsState()
    val formattedDate = viewModel.formattedDate
    val weekNumber = viewModel.weekNumber
    val selectedDate by viewModel.selectedDate.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val weeklyTasks by viewModel.weeklyTasks.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var selectedDayForDialog by remember { mutableStateOf<LocalDate?>(null) }

    // Task Detail Dialog
    if (selectedTask != null) {
        com.sg.taskspace.ui.components.TaskDetailDialog(
            task = selectedTask!!,
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
            } else {
                true
            }
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
    if (showBottomSheet) {
        com.sg.taskspace.ui.components.AddTaskBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            onSaveTask = { title, notes, priority, category, repeat ->
                viewModel.addTask(title, notes, priority, category, repeat)
                showBottomSheet = false
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
                        .statusBarsPadding(), // Handle status bar overlap
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
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Trophies", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                onClick = { showBottomSheet = true },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedDate, // e.g., Friday, November 28, 2025
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weekNumber, // e.g., Week 48
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Motivation/Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
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
                        val motivation = if (total == 0) "No tasks yet. Add one!" 
                                         else if (done == total) "All tasks completed! Great job!"
                                         else "You've completed $done out of $total tasks."

                        Text(
                            text = motivation,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val progress = if (total > 0) (done.toFloat() / total * 100).toInt() else 0
                            
                            StatColumn("Total", total.toString())
                            StatColumn("Done", done.toString())
                            StatColumn("Progress", "$progress%")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        // Progress Bar
                        val progressFraction = if (total > 0) done.toFloat() / total else 0f
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.background // Darker track
                        )
                    }
                }
            }

            // 3.5 Level/XP Card

            // 4. Weekly Progress
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This Week's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    WeeklyProgressRow(
                        selectedDate = selectedDate,
                        weeklyStats = weeklyStats,
                        onDateSelected = { /* Do nothing to main view */ },
                        onDayClick = { date -> selectedDayForDialog = date }
                    )
                }
            }
            item {
                Button(
                    onClick = { onWeeklyTasksClick() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Up Weekly Plan", style = MaterialTheme.typography.titleMedium)
                }
            }

            // 5. Today's Task Section
            item {
                Text(
                    text = "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (tasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, // Using CheckCircle for thicker look
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
                items(tasks) { task ->
                    TaskCard(
                        task = task, 
                        onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                        onClick = { selectedTask = task },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}




