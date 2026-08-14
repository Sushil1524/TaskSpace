package com.sg.taskspace.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentDayOfWeek = LocalDate.now().dayOfWeek.value % 7
    val thisSunday = LocalDate.now().minusDays(currentDayOfWeek.toLong())
    var currentWeekStart by remember { mutableStateOf(thisSunday) }

    val weeklyStats by viewModel.getWeekStats(currentWeekStart).collectAsState(initial = emptyList())
    val weekTasks by viewModel.getWeekStats(currentWeekStart).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Completed, Active
    var selectedPriority by remember { mutableStateOf("All") }
    var taskToCopy by remember { mutableStateOf<Task?>(null) }

    val totalTasks = weeklyStats.sumOf { it.totalTasks }
    val completedTasks = weeklyStats.sumOf { it.completedTasks }
    val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100).toInt() else 0
    val bestDayStat = weeklyStats.maxByOrNull { it.completedTasks }

    val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val isCurrentWeek = currentWeekStart == thisSunday

    // Copy Task Dialog
    taskToCopy?.let { task ->
        CopyTaskDialog(
            task = task,
            currentWeekStart = currentWeekStart,
            onDismiss = { taskToCopy = null },
            onCopy = { taskTarget, targetDate ->
                viewModel.addTask(
                    title = taskTarget.title,
                    notes = taskTarget.notes ?: "",
                    priority = taskTarget.priority,
                    category = taskTarget.category,
                    repeat = taskTarget.repeat,
                    date = targetDate,
                    estimatedMinutes = taskTarget.estimatedMinutes,
                    projectId = taskTarget.projectId,
                    tags = taskTarget.tags,
                    time = taskTarget.time
                )
                val formatted = targetDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                Toast.makeText(context, "Copied task to $formatted!", Toast.LENGTH_SHORT).show()
                taskToCopy = null
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Search tasks…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Week Navigator
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Week", tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${currentWeekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${currentWeekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCurrentWeek) {
                                Text("This Week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(
                            onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) },
                            enabled = !isCurrentWeek
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Week",
                                tint = if (!isCurrentWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Summary stats
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total",
                        value = totalTasks.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Done",
                        value = completedTasks.toString(),
                        color = SecondaryEmerald
                    )
                    SummaryStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Rate",
                        value = "$completionRate%",
                        color = PriorityMedium
                    )
                    bestDayStat?.let { stat ->
                        SummaryStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Best Day",
                            value = stat.date.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() },
                            color = PriorityHigh
                        )
                    }
                }
            }

            // Progress bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Weekly Completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$completionRate%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { completionRate / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Filter chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val statusFilters = listOf("All", "Completed", "Active")
                        items(statusFilters.size) { i ->
                            FilterChip(
                                selected = selectedFilter == statusFilters[i],
                                onClick = { selectedFilter = statusFilters[i] },
                                label = { Text(statusFilters[i]) }
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val priorityFilters = listOf("All", "High", "Medium", "Low")
                        items(priorityFilters.size) { i ->
                            FilterChip(
                                selected = selectedPriority == priorityFilters[i],
                                onClick = { selectedPriority = priorityFilters[i] },
                                label = { Text(priorityFilters[i]) },
                                leadingIcon = if (priorityFilters[i] != "All") {
                                    {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (priorityFilters[i]) {
                                                        "High" -> PriorityHigh
                                                        "Medium" -> PriorityMedium
                                                        else -> PriorityLow
                                                    }
                                                )
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Daily breakdown — each day with its tasks
            items(weeklyStats) { stat ->
                val date = stat.date
                val dateLabel = when (date) {
                    LocalDate.now() -> "Today"
                    LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> date.format(dayFormatter)
                }

                DayHistoryCard(
                    dateLabel = dateLabel,
                    stat = stat,
                    searchQuery = searchQuery,
                    statusFilter = selectedFilter,
                    priorityFilter = selectedPriority,
                    onCopyTask = { task -> taskToCopy = task }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyTaskDialog(
    task: Task,
    currentWeekStart: LocalDate,
    onDismiss: () -> Unit,
    onCopy: (Task, LocalDate) -> Unit
) {
    val daysOfWeek = remember(currentWeekStart) {
        (0..6).map { currentWeekStart.plusDays(it.toLong()) }
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Create a copy of \"${task.title}\" for:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick options: Today & Tomorrow
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedDate == LocalDate.now(),
                        onClick = { selectedDate = LocalDate.now() },
                        label = { Text("Today") },
                        leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedDate == LocalDate.now().plusDays(1),
                        onClick = { selectedDate = LocalDate.now().plusDays(1) },
                        label = { Text("Tomorrow") },
                        leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Or select day of week:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(daysOfWeek) { day ->
                        val isSelected = selectedDate == day
                        val dayLabel = day.format(DateTimeFormatter.ofPattern("EEE d"))
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDate = day },
                            label = { Text(dayLabel, fontSize = 11.sp) }
                        )
                    }
                }

                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCopy(task, selectedDate) }) {
                Text("Copy Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DayHistoryCard(
    dateLabel: String,
    stat: com.sg.taskspace.ui.viewmodel.TaskViewModel.DayStats,
    searchQuery: String,
    statusFilter: String,
    priorityFilter: String,
    onCopyTask: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(dateLabel == "Today" || dateLabel == "Yesterday") }

    val filteredTasks = stat.tasks.filter { task ->
        val matchesSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.notes?.contains(searchQuery, ignoreCase = true) == true
        val matchesStatus = when (statusFilter) {
            "Completed" -> task.isCompleted
            "Active" -> !task.isCompleted
            else -> true
        }
        val matchesPriority = priorityFilter == "All" || task.priority == priorityFilter
        matchesSearch && matchesStatus && matchesPriority
    }

    if (searchQuery.isNotBlank() && filteredTasks.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.animateContentSize()
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val completionColor = when {
                        stat.totalTasks == 0 -> MaterialTheme.colorScheme.outline
                        stat.completedTasks == stat.totalTasks -> SecondaryEmerald
                        stat.completedTasks > 0 -> PriorityMedium
                        else -> PriorityHigh
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(completionColor))
                    Column {
                        Text(dateLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${stat.completedTasks}/${stat.totalTasks} tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (stat.totalTasks > 0) {
                        val pct = (stat.completedTasks.toFloat() / stat.totalTasks * 100).toInt()
                        Text("$pct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mini progress bar
            if (stat.totalTasks > 0) {
                LinearProgressIndicator(
                    progress = { stat.completedTasks.toFloat() / stat.totalTasks },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = SecondaryEmerald,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }

            // Task list
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val displayTasks = if (searchQuery.isNotBlank() || statusFilter != "All" || priorityFilter != "All") filteredTasks else stat.tasks
                    if (displayTasks.isEmpty()) {
                        Text("No tasks match filters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        displayTasks.forEach { task ->
                            HistoryTaskRow(task = task, onCopy = { onCopyTask(task) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTaskRow(task: Task, onCopy: () -> Unit) {
    val priorityColor = when (task.priority) {
        "High" -> PriorityHigh
        "Medium" -> PriorityMedium
        else -> PriorityLow
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.category.isNotBlank()) {
                Text(task.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (task.isCompleted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = SecondaryEmerald, modifier = Modifier.size(18.dp))
            }
            TextButton(
                onClick = onCopy,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Copy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
