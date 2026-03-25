package com.sg.taskspace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.ui.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTasksScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val weeklyTasks by viewModel.weeklyTasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var currentTabDate by remember { mutableStateOf(selectedDate) }
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Calculate week days starting from SUNDAY
    val weekDays = remember(currentTabDate) {
        val daysToSubtract = currentTabDate.dayOfWeek.value % 7
        val startOfWeek = currentTabDate.minusDays(daysToSubtract.toLong())
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    // Filter tasks for the selected tab date
    val dateIso = currentTabDate.format(java.time.format.DateTimeFormatter.ISO_DATE)
    val dayName = currentTabDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    
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

    if (showBottomSheet) {
        com.sg.taskspace.ui.components.AddTaskBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            onSaveTask = { title, notes, priority, category, repeat ->
                // Add task for the SELECTED tab date
                // Now passing the date explicitly, so we don't need to change VM state.
                viewModel.addTask(title, notes, priority, category, repeat, currentTabDate)
                showBottomSheet = false
            }
        )
    }
    
    if (taskToEdit != null) {
        com.sg.taskspace.ui.components.TaskDetailDialog(
            task = taskToEdit!!,
            onDismissRequest = { taskToEdit = null },
            onUpdateTask = { updatedTask ->
                viewModel.updateTaskDetails(updatedTask)
                taskToEdit = null
            },
            onDeleteTask = { taskToDelete ->
                viewModel.deleteTask(taskToDelete)
                taskToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Weekly Plan") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                // Day Tabs
                PrimaryScrollableTabRow(
                    selectedTabIndex = weekDays.indexOf(currentTabDate),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    weekDays.forEach { date ->
                        val isSelected = date == currentTabDate
                        Tab(
                            selected = isSelected,
                            onClick = { 
                                currentTabDate = date 
                            },
                            text = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()))
                                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            if (dayTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tasks for ${currentTabDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(dayTasks) { task ->
                    TaskCard(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                        onClick = { taskToEdit = task },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }
}
