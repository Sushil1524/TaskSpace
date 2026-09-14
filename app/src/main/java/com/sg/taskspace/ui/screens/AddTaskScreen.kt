package com.sg.taskspace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.viewmodel.ProjectViewModel
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: TaskViewModel,
    projectViewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Form state
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var category by remember { mutableStateOf("General") }
    var repeat by remember { mutableStateOf("None") }
    var estimatedMinutes by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableStateOf("") }
    var showCustomTime by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    // Reminders & Scheduled Time
    var scheduledTime by remember { mutableStateOf<String?>(null) }
    var setReminder by remember { mutableStateOf(false) }
    var reminderOffsetMinutes by remember { mutableIntStateOf(15) } // Default 15 mins from now

    val projects by projectViewModel.projects.collectAsState()

    // Tags state with ability to add custom tags
    var availableTags by remember {
        mutableStateOf(
            listOf("#urgent", "#design", "#finance", "#health", "#study", "#work", "#personal", "#quick-win", "#research", "#meeting")
        )
    }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // Misclick prevention dialog
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = title.isNotBlank() || notes.isNotBlank() || selectedTags.isNotEmpty() || estimatedMinutes > 0

    // Handle back press
    BackHandler {
        if (isDirty) showDiscardDialog = true else onNavigateBack()
    }

    // Add Custom Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Custom Tag", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g. fitness, shopping") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = if (newTagInput.startsWith("#")) newTagInput.trim() else "#${newTagInput.trim()}"
                        if (formatted.length > 1) {
                            if (formatted !in availableTags) {
                                availableTags = availableTags + formatted
                            }
                            selectedTags = selectedTags + formatted
                        }
                        newTagInput = ""
                        showAddTagDialog = false
                    },
                    enabled = newTagInput.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Discard Confirmation Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Task?", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                Button(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Task", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isDirty) showDiscardDialog = true else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val finalMinutes = if (showCustomTime) customMinutes.toIntOrNull() ?: estimatedMinutes else estimatedMinutes
                                val effectiveTime = scheduledTime ?: if (setReminder) {
                                    java.time.LocalTime.now().plusMinutes(reminderOffsetMinutes.toLong())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US))
                                } else null

                                viewModel.addTask(
                                    title = title,
                                    notes = notes,
                                    priority = priority,
                                    category = category,
                                    repeat = repeat,
                                    estimatedMinutes = finalMinutes,
                                    tags = selectedTags.joinToString(","),
                                    projectId = selectedProjectId,
                                    time = effectiveTime
                                )
                                onNavigateBack()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(
                            "Save",
                            fontWeight = FontWeight.Bold,
                            color = if (title.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            // Project Selector
            if (projects.isNotEmpty()) {
                SectionLabel("Project")
                ExposedDropdownMenuBox(
                    expanded = projectDropdownExpanded,
                    onExpandedChange = { projectDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = projects.find { it.id == selectedProjectId }?.name ?: "No Project",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Project") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = projectDropdownExpanded,
                        onDismissRequest = { projectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Project") },
                            onClick = { selectedProjectId = null; projectDropdownExpanded = false }
                        )
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = { selectedProjectId = project.id; projectDropdownExpanded = false }
                            )
                        }
                    }
                }
            }

            // Priority
            SectionLabel("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChip("High", priority == "High", PriorityHigh) { priority = "High" }
                PriorityChip("Medium", priority == "Medium", PriorityMedium) { priority = "Medium" }
                PriorityChip("Low", priority == "Low", PriorityLow) { priority = "Low" }
            }

            // Category
            SectionLabel("Category")
            CategorySelector(
                selected = category,
                categories = listOf("General", "Study", "Work", "Personal", "Health", "Finance"),
                onSelect = { category = it }
            )

            // Time Estimation
            SectionLabel("Estimated Duration")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(0 to "None", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h", -1 to "Custom")
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets.size) { i ->
                        val (mins, label) = presets[i]
                        val isCustom = mins == -1
                        val isSelected = if (isCustom) showCustomTime else (estimatedMinutes == mins && !showCustomTime)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isCustom) {
                                    showCustomTime = true
                                    estimatedMinutes = 0
                                } else {
                                    showCustomTime = false
                                    estimatedMinutes = mins
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                AnimatedVisibility(visible = showCustomTime, enter = expandVertically(), exit = shrinkVertically()) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("min") }
                    )
                }
            }

            // Scheduled Start Time
            SectionLabel("Scheduled Time")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                val timeOptions = listOf(
                    "09:00 AM" to "Morning 9 AM",
                    "02:00 PM" to "Afternoon 2 PM",
                    "06:00 PM" to "Evening 6 PM"
                )
                timeOptions.forEach { (timeVal, label) ->
                    FilterChip(
                        selected = scheduledTime == timeVal,
                        onClick = { scheduledTime = if (scheduledTime == timeVal) null else timeVal },
                        label = { Text(label) }
                    )
                }

                // Custom Time Button
                OutlinedButton(
                    onClick = {
                        val calendar = java.util.Calendar.getInstance()
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val isPm = hour >= 12
                                val h12 = if (hour % 12 == 0) 12 else hour % 12
                                scheduledTime = String.format("%02d:%02d %s", h12, minute, if (isPm) "PM" else "AM")
                            },
                            calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE),
                            false
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(scheduledTime ?: "Custom", fontSize = 12.sp)
                }

                if (scheduledTime != null) {
                    FilterChip(
                        selected = false,
                        onClick = { scheduledTime = null },
                        label = { Text("Clear") }
                    )
                }
            }

            // Tags (With option to add custom tags)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Tags")
                TextButton(onClick = { showAddTagDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom Tag", style = MaterialTheme.typography.labelSmall)
                }
            }
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableTags.size) { i ->
                    val tag = availableTags[i]
                    val isSelected = tag in selectedTags
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                        },
                        label = { Text(tag) }
                    )
                }
            }

            // Reminder toggle & picker
            SectionLabel("Task Reminder")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Remind Me", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Get a notification reminder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = setReminder, onCheckedChange = { setReminder = it })
            }

            AnimatedVisibility(visible = setReminder, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remind me in:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val offsets = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hour", 120 to "2 hours", 1440 to "Tomorrow")
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(offsets.size) { i ->
                            val (mins, label) = offsets[i]
                            FilterChip(
                                selected = reminderOffsetMinutes == mins,
                                onClick = { reminderOffsetMinutes = mins },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Repeat
            SectionLabel("Repeat")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("None", "Daily", "Weekly").forEach { r ->
                    FilterChip(
                        selected = repeat == r,
                        onClick = { repeat = r },
                        label = { Text(r) }
                    )
                }
            }

            // Save Button (bottom CTA)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalMinutes = if (showCustomTime) customMinutes.toIntOrNull() ?: estimatedMinutes else estimatedMinutes
                        val effectiveTime = scheduledTime ?: if (setReminder) {
                            java.time.LocalTime.now().plusMinutes(reminderOffsetMinutes.toLong())
                                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US))
                        } else null

                        viewModel.addTask(
                            title = title,
                            notes = notes,
                            priority = priority,
                            category = category,
                            repeat = repeat,
                            estimatedMinutes = finalMinutes,
                            tags = selectedTags.joinToString(","),
                            projectId = selectedProjectId,
                            time = effectiveTime
                        )
                        onNavigateBack()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Task", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun PriorityChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        )
    )
}

@Composable
private fun CategorySelector(selected: String, categories: List<String>, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories.size) { i ->
            val cat = categories[i]
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat) }
            )
        }
    }
}
