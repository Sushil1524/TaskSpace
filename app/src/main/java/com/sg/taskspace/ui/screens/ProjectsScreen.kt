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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.Project
import com.sg.taskspace.ui.viewmodel.ProjectViewModel
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.util.NotificationHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val ProjectColors = listOf(
    "#6366F1", // Indigo
    "#10B981", // Emerald
    "#EF4444", // Rose
    "#F59E0B", // Amber
    "#0EA5E9", // Sky
    "#8B5CF6", // Violet
    "#EC4899", // Pink
    "#14B8A6", // Teal
    "#F97316", // Orange
    "#84CC16"  // Lime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectViewModel: ProjectViewModel,
    taskViewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val projects by projectViewModel.projects.collectAsState()
    val allTasks by taskViewModel.currentDisplayTasks.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Project?>(null) }

    // Add/Edit Project Dialog
    if (showAddDialog || editingProject != null) {
        ProjectDialog(
            project = editingProject,
            onDismiss = {
                showAddDialog = false
                editingProject = null
            },
            onSave = { name, description, colorHex, dueDate, reminderMillis ->
                if (editingProject != null) {
                    val updated = editingProject!!.copy(
                        name = name,
                        description = description,
                        colorHex = colorHex,
                        dueDate = dueDate,
                        reminderTimeMillis = reminderMillis
                    )
                    projectViewModel.updateProject(updated)
                    if (reminderMillis != null) {
                        NotificationHelper.scheduleReminder(
                            context = context,
                            id = updated.id,
                            title = "Project Reminder: ${updated.name}",
                            notes = updated.description ?: "Project deadline is approaching!",
                            triggerAtMillis = reminderMillis
                        )
                    }
                } else {
                    val newProj = Project(
                        name = name,
                        description = description,
                        colorHex = colorHex,
                        dueDate = dueDate,
                        reminderTimeMillis = reminderMillis
                    )
                    projectViewModel.addProject(name, description, colorHex, dueDate, reminderMillis)
                    if (reminderMillis != null) {
                        NotificationHelper.scheduleReminder(
                            context = context,
                            id = newProj.id,
                            title = "Project Reminder: ${newProj.name}",
                            notes = newProj.description ?: "Project deadline is approaching!",
                            triggerAtMillis = reminderMillis
                        )
                    }
                }
                showAddDialog = false
                editingProject = null
            }
        )
    }

    // Delete Confirmation
    showDeleteConfirm?.let { project ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Project?") },
            text = { Text("\"${project.name}\" will be deleted. Tasks assigned to this project will remain unaffected.") },
            confirmButton = {
                TextButton(onClick = {
                    projectViewModel.deleteProject(project)
                    NotificationHelper.cancelReminder(context, project.id)
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "No projects yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Create a project to group your tasks\nunder a greater purpose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    val projectTasks = allTasks.filter { it.projectId == project.id }
                    val completedCount = projectTasks.count { it.isCompleted }
                    val totalCount = projectTasks.size
                    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                    ProjectCard(
                        project = project,
                        totalTasks = totalCount,
                        completedTasks = completedCount,
                        progress = progress,
                        onEdit = { editingProject = project },
                        onDelete = { showDeleteConfirm = project }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    totalTasks: Int,
    completedTasks: Int,
    progress: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val projectColor = try {
        Color(android.graphics.Color.parseColor(project.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(projectColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Color dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(projectColor)
                        )
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }

                if (!project.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Due date and Reminder chips
                if (!project.dueDate.isNullOrBlank() || project.reminderTimeMillis != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!project.dueDate.isNullOrBlank()) {
                            Surface(
                                color = projectColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = projectColor)
                                    Text(
                                        text = "Due: ${project.dueDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = projectColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (project.reminderTimeMillis != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Reminder Set",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalTasks == 0) "No tasks assigned" else "$completedTasks / $totalTasks tasks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (totalTasks > 0) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = projectColor
                        )
                    }
                }

                if (totalTasks > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = projectColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String, String?, Long?) -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var selectedColor by remember { mutableStateOf(project?.colorHex ?: ProjectColors.first()) }
    var dueDate by remember { mutableStateOf(project?.dueDate ?: "") }
    var setReminder by remember { mutableStateOf(project?.reminderTimeMillis != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "New Project" else "Edit Project", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (e.g. 2026-08-30)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Set Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = setReminder,
                        onCheckedChange = { setReminder = it }
                    )
                }

                Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProjectColors.size) { i ->
                        val hex = ProjectColors[i]
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        val isSelected = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val reminderMillis = if (setReminder) {
                            val parsedDate = runCatching { LocalDate.parse(dueDate, DateTimeFormatter.ISO_DATE) }.getOrNull()
                            if (parsedDate != null) {
                                parsedDate.atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            } else {
                                System.currentTimeMillis() + 86400000L // Default 24h from now
                            }
                        } else null

                        onSave(name, description.ifBlank { null }, selectedColor, dueDate.ifBlank { null }, reminderMillis)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(if (project == null) "Create" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
