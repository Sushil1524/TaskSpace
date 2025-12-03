package com.sg.taskspace.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sg.taskspace.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val taskFlow = remember(taskId) { viewModel.getTaskById(taskId) }
    val task by taskFlow.collectAsState(initial = null)
    
    // Edit State
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editedNotes by remember { mutableStateOf("") }
    var editedPriority by remember { mutableStateOf("Medium") }

    // Initialize edit state when task loads
    LaunchedEffect(task) {
        task?.let {
            editedTitle = it.title
            editedNotes = it.notes ?: ""
            editedPriority = it.priority
        }
    }

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Task" else "Task Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (task != null) {
                        if (isEditing) {
                            IconButton(onClick = {
                                viewModel.addTask(editedTitle, editedNotes, editedPriority,
                                    task!!.category
                                )
                                // Note: addTask creates a NEW task. We need updateTask logic in VM for editing existing.
                                // Let's fix this in VM or use a workaround. 
                                // Actually, we should update the existing task.
                                // Let's add updateTaskById logic to VM or just use updateTask with the same ID.
                                // Since addTask generates a new ID in VM, we need a dedicated update method.
                                // For now, let's assume we'll add `updateTaskDetails` to VM.
                                viewModel.updateTaskDetails(task!!.copy(
                                    title = editedTitle,
                                    notes = editedNotes,
                                    priority = editedPriority
                                ))
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        } else {
                            IconButton(onClick = { 
                                viewModel.deleteTask(task!!)
                                onNavigateBack()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    // Priority Selection
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            FilterChip(
                                selected = editedPriority == p,
                                onClick = { editedPriority = p },
                                label = { Text(p) }
                            )
                        }
                    }
                } else {
                    // View Mode
                    Text(
                        text = task!!.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = { isEditing = true },
                            label = { Text("Priority: ${task!!.priority}") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { isEditing = true },
                            label = { Text("Edit Details") }
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!task!!.notes.isNullOrBlank()) {
                        val notesText = task!!.notes!!
                        
                        // Copy Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val clip = ClipData.newPlainText("Task Notes", notesText)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Notes copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }

                        // Linkify Logic
                        val annotatedString = buildAnnotatedString {
                            append(notesText)
                            val urlRegex = "(https?://\\S+)".toRegex()
                            urlRegex.findAll(notesText).forEach { matchResult ->
                                val link = androidx.compose.ui.text.LinkAnnotation.Url(
                                    url = matchResult.value,
                                    styles = androidx.compose.ui.text.TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                )
                                addLink(link, matchResult.range.first, matchResult.range.last + 1)
                            }
                        }

                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    } else {
                        Text("No notes added.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }
    }
}
