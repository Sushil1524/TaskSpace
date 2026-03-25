package com.sg.taskspace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sg.taskspace.data.Goal
import com.sg.taskspace.ui.viewmodel.GoalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    viewModel: GoalViewModel,
    onNavigateBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Long-Term Goals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val active = goals.filter { !it.isCompleted }
            val completed = goals.filter { it.isCompleted }

            if (active.isEmpty() && completed.isEmpty()) {
                item {
                    Text(
                        text = "No goals set. Add one to start tracking long-term targets!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (active.isNotEmpty()) {
                item {
                    Text("Active Goals", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(active) { goal ->
                    GoalCard(goal, onToggle = { viewModel.toggleGoalCompletion(goal) }, onDelete = { viewModel.deleteGoal(goal) })
                }
            }

            if (completed.isNotEmpty()) {
                item {
                    Text("Completed Goals", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(completed) { goal ->
                    GoalCard(goal, onToggle = { viewModel.toggleGoalCompletion(goal) }, onDelete = { viewModel.deleteGoal(goal) })
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var deadline by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Goal") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline (e.g. End of Month)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank() && deadline.isNotBlank()) {
                            viewModel.addGoal(title, deadline)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GoalCard(goal: Goal, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = goal.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title, 
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (goal.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Text("Deadline: ${goal.deadlineDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
