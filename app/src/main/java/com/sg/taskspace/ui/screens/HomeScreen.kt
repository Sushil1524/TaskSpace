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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.sg.taskspace.ui.components.DayTimelineView
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
    val allUniqueTags by viewModel.allUniqueTags.collectAsState(initial = emptyList())
    val projects by (projectViewModel?.projects?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val projectsMap = remember(projects) { projects.associateBy { it.id } }
    val formattedDate = viewModel.formattedDate
    val weekNumber = viewModel.weekNumber
    val selectedDate by viewModel.selectedDate.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val weeklyTasks by viewModel.weeklyTasks.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val overdueTasks by viewModel.overdueTasks.collectAsState()

    var selectedTag by remember { mutableStateOf("All") }
    var isTimelineView by remember { mutableStateOf(false) }
    var showFinishedSection by remember { mutableStateOf(true) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var selectedDayForDialog by remember { mutableStateOf<LocalDate?>(null) }
    var showVacationDialog by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedTag) {
        if (selectedTag == "All") tasks
        else tasks.filter { task ->
            task.tags.split(",").map { it.trim().removePrefix("#").lowercase() }
                .contains(selectedTag.removePrefix("#").lowercase())
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun deleteTaskWithUndo(taskToDelete: Task) {
        viewModel.deleteTask(taskToDelete)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "'${taskToDelete.title}' moved to trash",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTask(taskToDelete.id)
            }
        }
    }

    fun moveOverdueTaskWithUndo(task: Task) {
        val originalDate = task.createdForDate
        viewModel.moveTaskToToday(task)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Moved '${task.title}' to Today",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTaskDate(task, originalDate)
            }
        }
    }

    fun moveAllOverdueWithUndo(tasksToMove: List<Task>) {
        if (tasksToMove.isEmpty()) return
        val backupList = tasksToMove.map { it to it.createdForDate }
        viewModel.moveAllOverdueTasksToToday(tasksToMove)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Moved ${backupList.size} overdue tasks to Today",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTasksDates(backupList)
            }
        }
    }

    if (showVacationDialog && userPreferences != null) {
        com.sg.taskspace.ui.components.VacationModeDialog(
            userPreferences = userPreferences!!,
            onDismiss = { showVacationDialog = false },
            onSave = { enabled, startDate, endDate, reason, freezeHabits, pauseReminders ->
                viewModel.setVacationMode(enabled, startDate, endDate, reason, freezeHabits, pauseReminders)
            }
        )
    }

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
                deleteTaskWithUndo(taskToDelete)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        IconButton(onClick = { showVacationDialog = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Vacation Mode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                userPreferences?.let { prefs ->
                    com.sg.taskspace.ui.components.VacationModeBanner(
                        userPreferences = prefs,
                        onEndVacation = { viewModel.clearVacationMode() }
                    )
                }
            }
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
                        val totalEstMinutes = tasks.sumOf { it.estimatedMinutes }
                        val doneEstMinutes = tasks.filter { it.isCompleted }.sumOf { it.estimatedMinutes }
                        val motivation = when {
                            total == 0 -> "No tasks yet. Add one!"
                            done == total -> "All tasks completed! Great job!"
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
                            StatColumn("Est. Time", formatMinutes(totalEstMinutes))
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

                        // Daily Workload & Time Estimates Pill
                        if (totalEstMinutes > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Daily Workload",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${formatMinutes(doneEstMinutes)} of ${formatMinutes(totalEstMinutes)} completed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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

            // Today's Tasks header & View Mode Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = if (!isTimelineView) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { isTimelineView = false }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "List View",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (!isTimelineView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "List",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isTimelineView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = if (isTimelineView) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { isTimelineView = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Timeline View",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isTimelineView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Timeline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTimelineView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tag Filter Chips (Scrollable)
            item {
                val tagsFromTasks = tasks.flatMap { t -> t.tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() } }
                val uniqueTagsInTasks = (allUniqueTags.map { it.removePrefix("#") } + tagsFromTasks).distinct().sorted()

                if (uniqueTagsInTasks.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == "All",
                                onClick = { selectedTag = "All" },
                                label = { Text("All (${tasks.size})") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(uniqueTagsInTasks.size) { i ->
                            val tag = uniqueTagsInTasks[i]
                            val count = tasks.count { t -> t.tags.split(",").map { it.trim().removePrefix("#").lowercase() }.contains(tag.lowercase()) }
                            FilterChip(
                                selected = selectedTag.equals(tag, ignoreCase = true),
                                onClick = { selectedTag = if (selectedTag.equals(tag, ignoreCase = true)) "All" else tag },
                                label = { Text("#$tag ($count)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Overdue Tasks Banner / Section
            if (overdueTasks.isNotEmpty() && selectedDate == java.time.LocalDate.now()) {
                item {
                    val filteredOverdueTasks = remember(overdueTasks, selectedTag) {
                        if (selectedTag == "All") overdueTasks
                        else overdueTasks.filter { task ->
                            task.tags.split(",").map { it.trim().removePrefix("#").lowercase() }
                                .contains(selectedTag.removePrefix("#").lowercase())
                        }
                    }
                    if (filteredOverdueTasks.isNotEmpty()) {
                        com.sg.taskspace.ui.components.OverdueSection(
                            overdueTasks = filteredOverdueTasks,
                            projectsMap = projectsMap,
                            onMoveTaskToToday = { moveOverdueTaskWithUndo(it) },
                            onMoveAllToToday = { moveAllOverdueWithUndo(filteredOverdueTasks) },
                            onToggleTask = { viewModel.toggleTaskCompletion(it) },
                            onTaskClick = { selectedTask = it },
                            onDeleteTask = { deleteTaskWithUndo(it) },
                            swipeRightAction = userPreferences?.swipeRightAction ?: "Complete",
                            swipeLeftAction = userPreferences?.swipeLeftAction ?: "Delete",
                            soundEffectsEnabled = userPreferences?.soundEffectsEnabled ?: true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else if (overdueTasks.isNotEmpty() && selectedDate != java.time.LocalDate.now()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectDate(java.time.LocalDate.now()) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${overdueTasks.size} tasks overdue",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "View Today →",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Tasks Rendering (Timeline or List View)
            val activeTasks = filteredTasks.filter { !it.isCompleted }
            val completedTasks = filteredTasks.filter { it.isCompleted }

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
                            text = if (selectedTag != "All") "No tasks with tag #$selectedTag for today."
                            else "No tasks for today.\nAdd some tasks to get started!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isTimelineView) {
                // Interactive Day Timeline Schedule View
                item {
                    DayTimelineView(
                        tasks = filteredTasks,
                        projectsMap = projectsMap,
                        onTaskClick = { selectedTask = it },
                        onToggleTask = { viewModel.toggleTaskCompletion(it) }
                    )
                }
            } else {
                val swipeRightAction = userPreferences?.swipeRightAction ?: "Complete"
                val swipeLeftAction = userPreferences?.swipeLeftAction ?: "Delete"
                val soundEffectsEnabled = userPreferences?.soundEffectsEnabled ?: true

                // Active tasks first
                items(activeTasks) { task ->
                    TaskCard(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                        onClick = { selectedTask = task },
                        onDelete = { deleteTaskWithUndo(task) },
                        onSnooze = { viewModel.snoozeTaskToTomorrow(task) },
                        projectName = task.projectId?.let { projectsMap[it]?.name },
                        swipeRightAction = swipeRightAction,
                        swipeLeftAction = swipeLeftAction,
                        soundEffectsEnabled = soundEffectsEnabled
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
                                onDelete = { deleteTaskWithUndo(task) },
                                onSnooze = { viewModel.snoozeTaskToTomorrow(task) },
                                projectName = task.projectId?.let { projectsMap[it]?.name },
                                swipeRightAction = swipeRightAction,
                                swipeLeftAction = swipeLeftAction,
                                soundEffectsEnabled = soundEffectsEnabled
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

private fun formatMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0 && mins > 0) "${hours}h ${mins}m" else if (hours > 0) "${hours}h" else "${mins}m"
}
