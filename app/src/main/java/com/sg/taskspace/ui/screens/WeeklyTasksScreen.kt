package com.sg.taskspace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sg.taskspace.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTasksScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    // We need a way to get tasks for the current week.
    // Let's add a property to ViewModel for this.
    val tasks by viewModel.weeklyTasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("This Week's Tasks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (tasks.isEmpty()) {
                item {
                    Text("No tasks for this week.", modifier = Modifier.padding(16.dp))
                }
            } else {
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}
