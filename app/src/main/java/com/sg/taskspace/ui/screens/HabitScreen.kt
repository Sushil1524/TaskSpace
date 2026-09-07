package com.sg.taskspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.HabitLog
import com.sg.taskspace.ui.components.AddEditHabitDialog
import com.sg.taskspace.ui.components.HabitHeatmap
import com.sg.taskspace.ui.viewmodel.HabitViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit
) {
    val habits by viewModel.habits.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val logsForSelectedDate by viewModel.logsForSelectedDate.collectAsState()
    val habitStats by viewModel.habitStats.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitForRelapseModal by remember { mutableStateOf<Habit?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showVacationDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (showVacationDialog) {
        com.sg.taskspace.ui.components.VacationModeDialog(
            userPreferences = userPreferences,
            onDismiss = { showVacationDialog = false },
            onSave = { enabled, startDate, endDate, reason, freezeHabits, pauseReminders ->
                viewModel.setVacationMode(enabled, startDate, endDate, reason, freezeHabits, pauseReminders)
            }
        )
    }

    val filteredHabits = remember(habits, selectedFilter) {
        when (selectedFilter) {
            "BUILD" -> habits.filter { it.type == "BUILD" }
            "QUIT" -> habits.filter { it.type == "QUIT" }
            "NUMERIC" -> habits.filter { it.type == "NUMERIC" }
            else -> habits
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Habit Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showVacationDialog = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Vacation Mode")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    habitToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                com.sg.taskspace.ui.components.VacationModeBanner(
                    userPreferences = userPreferences,
                    onEndVacation = { viewModel.clearVacationMode() }
                )
            }

            // Heatmap Section
            item {
                val completedMap = remember(allLogs) {
                    allLogs.filter { it.isCompleted }.groupBy { it.date }.mapValues { entry -> entry.value.size }
                }
                HabitHeatmap(completedDatesCountMap = completedMap)
            }

            // Overview Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val activeCount = habits.size
                    val todayCompleted = habits.count { h ->
                        val log = logsForSelectedDate.find { it.habitId == h.id }
                        log?.isCompleted == true
                    }
                    val completionRate = if (activeCount > 0) (todayCompleted * 100) / activeCount else 0

                    QuickStatCard(
                        title = "Today's Rate",
                        value = "$completionRate%",
                        subtitle = "$todayCompleted / $activeCount Done",
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )

                    val maxStreak = habitStats.values.maxOfOrNull { it.maxStreak } ?: 0
                    QuickStatCard(
                        title = "Best Streak",
                        value = "$maxStreak Days",
                        subtitle = "Personal Best",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFFF59E0B)
                    )
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "BUILD" to "Build",
                        "QUIT" to "Quit",
                        "NUMERIC" to "Target"
                    )
                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // 7-Day Day Selector Bubble Bar
            item {
                val startOfWeek = selectedDate.with(DayOfWeek.MONDAY)
                val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        weekDays.forEach { date ->
                            val isSelected = date == selectedDate

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectDate(date) }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = date.dayOfWeek.name.take(3),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // List of Habits
            if (filteredHabits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No habits found. Tap + to build or quit a habit!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(filteredHabits, key = { it.id }) { habit ->
                val log = logsForSelectedDate.find { it.habitId == habit.id }
                val stats = habitStats[habit.id]

                HabitCard(
                    habit = habit,
                    todayLog = log,
                    allHabitLogs = allLogs.filter { it.habitId == habit.id },
                    stats = stats,
                    onToggleToday = { viewModel.toggleHabitCompletion(habit, selectedDate, logsForSelectedDate) },
                    onDayToggle = { date -> viewModel.toggleHabitCompletion(habit, date, allLogs) },
                    onNumericDelta = { delta -> viewModel.updateNumericProgress(habit, selectedDate, delta, logsForSelectedDate) },
                    onRelapseClick = { habitForRelapseModal = habit },
                    onEditClick = {
                        habitToEdit = habit
                        showAddDialog = true
                    },
                    onDeleteClick = {
                        val habitToDelete = habit
                        viewModel.deleteHabit(habitToDelete)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "'${habitToDelete.name}' moved to trash",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreHabit(habitToDelete.id)
                            }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AddEditHabitDialog(
            initialHabit = habitToEdit,
            onSave = { habitToSave ->
                if (habitToEdit == null) {
                    viewModel.addHabit(habitToSave)
                } else {
                    viewModel.updateHabit(habitToSave)
                }
                showAddDialog = false
                habitToEdit = null
            },
            onDismiss = {
                showAddDialog = false
                habitToEdit = null
            }
        )
    }

    // Relapse Reflection Dialog for Quit Habits
    if (habitForRelapseModal != null) {
        val targetHabit = habitForRelapseModal!!
        var relapseNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { habitForRelapseModal = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Log Relapse") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "It's okay! Relapses are part of the journey. What triggered this relapse?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = relapseNotes,
                        onValueChange = { relapseNotes = it },
                        placeholder = { Text("Trigger / Reflection notes...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logRelapse(targetHabit, today, relapseNotes.ifBlank { null }, allLogs)
                        habitForRelapseModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Relapse")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitForRelapseModal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    todayLog: HabitLog?,
    allHabitLogs: List<HabitLog>,
    stats: HabitViewModel.HabitStats?,
    onToggleToday: () -> Unit,
    onDayToggle: (LocalDate) -> Unit,
    onNumericDelta: (Float) -> Unit,
    onRelapseClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val habitColor = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF10B981)
        }
    }

    val isCompletedToday = todayLog?.isCompleted == true
    val currentValue = todayLog?.value ?: 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(habitColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val typeBadge = when (habit.type) {
                                "QUIT" -> "Quit"
                                "NUMERIC" -> "Target"
                                else -> "Build"
                            }
                            Text(
                                text = typeBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val scheduleText = when (habit.scheduleType) {
                            "SPECIFIC_DAYS" -> habit.selectedDays
                            "TIMES_PER_WEEK" -> "${habit.weeklyTargetCount}x / Week"
                            "INTERVAL" -> "Every ${habit.intervalDays} Days"
                            else -> "Daily"
                        }
                        Text(
                            text = scheduleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Strength & Streak badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (stats != null && stats.habitStrength > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${stats.habitStrength}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (stats != null && stats.currentStreak > 0) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${stats.currentStreak}d",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-Day Mon-Sun Weekly Check-In Bubbles
            val today = LocalDate.now()
            val startOfWeek = today.with(DayOfWeek.MONDAY)
            val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { date ->
                    val dateIso = date.format(DateTimeFormatter.ISO_DATE)
                    val dayLog = allHabitLogs.find { it.habitId == habit.id && it.date == dateIso }
                    val isFuture = date > today
                    val isToday = date == today

                    val isQuit = habit.type == "QUIT"
                    val hasRelapse = isQuit && (dayLog != null && (dayLog.relapseNotes != null || !dayLog.isCompleted))
                    val isDone = if (isQuit) (!hasRelapse && !isFuture) else (dayLog?.isCompleted == true)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(enabled = isToday) {
                            onDayToggle(date)
                        }
                    ) {
                        Text(
                            text = date.dayOfWeek.name.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        hasRelapse -> MaterialTheme.colorScheme.error
                                        isDone -> habitColor
                                        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    }
                                )
                                .border(
                                    width = if (isToday) 2.dp else 0.dp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasRelapse) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Type-Specific Interaction Section
            when (habit.type) {
                "NUMERIC" -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentValue.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedIconButton(
                                    onClick = { onNumericDelta(-1f) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onNumericDelta(1f) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("+ 1", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        val progressRatio = (currentValue / habit.targetValue).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = habitColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                }

                "QUIT" -> {
                    val hasRelapseToday = todayLog != null && (todayLog.relapseNotes != null || !todayLog.isCompleted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (hasRelapseToday) MaterialTheme.colorScheme.errorContainer else habitColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasRelapseToday) "Relapsed Today" else "Clean Today • ${stats?.daysClean ?: 0}d Clean",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasRelapseToday) MaterialTheme.colorScheme.onErrorContainer else habitColor
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRelapseClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Relapse", fontSize = 11.sp)
                        }
                    }
                }

                else -> {
                    // BUILD habit standard completion button
                    Button(
                        onClick = onToggleToday,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompletedToday) habitColor else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                            contentColor = if (isCompletedToday) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (isCompletedToday) Icons.Default.CheckCircle else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompletedToday) "Completed Today" else "Mark Complete Today",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
