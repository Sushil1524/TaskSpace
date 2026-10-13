package com.sg.taskspace.ui.components

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityHighContainer
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityLowContainer
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.PriorityMediumContainer
import com.sg.taskspace.util.TagStyleHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    task: Task,
    projects: List<Project> = emptyList(),
    viewModel: com.sg.taskspace.ui.viewmodel.TaskViewModel? = null,
    onDismissRequest: () -> Unit,
    onUpdateTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    // Editable state
    var editedTitle by remember { mutableStateOf(task.title) }
    var editedNotes by remember { mutableStateOf(task.notes ?: "") }
    var editedPriority by remember { mutableStateOf(task.priority) }
    var editedCategory by remember { mutableStateOf(task.category) }
    var editedProjectId by remember { mutableStateOf(task.projectId) }
    var editedTags by remember { mutableStateOf(task.tags) }
    var editedRepeat by remember { mutableStateOf(task.repeat) }
    var editedEstimatedMinutes by remember { mutableStateOf(task.estimatedMinutes) }
    var editedTrackedMinutes by remember { mutableStateOf(task.trackedMinutes) }
    var editedTime by remember { mutableStateOf(task.time) }
    var showEditTrackedDialogInEditMode by remember { mutableStateOf(false) }

    val focusTimerState by (viewModel?.focusTimerState?.collectAsState() ?: com.sg.taskspace.util.FocusTimerManager.timerState.collectAsState())
    val focusTimerElapsedSeconds by (viewModel?.focusTimerElapsedSeconds?.collectAsState() ?: com.sg.taskspace.util.FocusTimerManager.elapsedSeconds.collectAsState())
    val isTimerActive = focusTimerState.taskId == task.id
    val isTimerRunning = isTimerActive && focusTimerState.isRunning
    val activeOnOther = focusTimerState.taskId != null && focusTimerState.taskId != task.id

    var editedDate by remember {
        mutableStateOf(
            try {
                LocalDate.parse(task.createdForDate)
            } catch (e: Exception) {
                LocalDate.now()
            }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val priorityColor = when (if (isEditing) editedPriority else task.priority) {
        "High" -> PriorityHigh
        "Medium" -> PriorityMedium
        else -> PriorityLow
    }
    val priorityContainerColor = when (if (isEditing) editedPriority else task.priority) {
        "High" -> PriorityHighContainer
        "Medium" -> PriorityMediumContainer
        else -> PriorityLowContainer
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        editedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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

    val tagsList = remember(task.tags) {
        if (task.tags.isBlank()) emptyList()
        else task.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    val projectName = remember(task.projectId, projects) {
        projects.find { it.id == task.projectId }?.name
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // ── Top Header Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(priorityContainerColor.copy(alpha = 0.35f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = priorityContainerColor,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Task,
                                    contentDescription = null,
                                    tint = priorityColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isEditing) "Edit Task" else "Task Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEditing) "Modify parameters below" else task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                // ── Scrollable Body Content ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (isEditing) {
                        // ══════════════════════════════════════════
                        // ── EDIT MODE (Spacious & Clean Layout) ──
                        // ══════════════════════════════════════════

                        // Task Title
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("Task Title", fontWeight = FontWeight.SemiBold) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )

                        // Priority Selector
                        SectionHeader("Priority")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("High", "Medium", "Low").forEach { p ->
                                val pColor = when (p) { "High" -> PriorityHigh; "Medium" -> PriorityMedium; else -> PriorityLow }
                                val pContainer = when (p) { "High" -> PriorityHighContainer; "Medium" -> PriorityMediumContainer; else -> PriorityLowContainer }
                                FilterChip(
                                    selected = editedPriority == p,
                                    onClick = { editedPriority = p },
                                    label = { Text(p, fontWeight = FontWeight.Bold) },
                                    leadingIcon = if (editedPriority == p) {
                                        { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = pContainer,
                                        selectedLabelColor = pColor
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Category Selector
                        SectionHeader("Category")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("General", "Personal", "Work", "Study", "Finance", "Health", "Urgent").forEach { c ->
                                FilterChip(
                                    selected = editedCategory == c,
                                    onClick = { editedCategory = c },
                                    label = { Text(c) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Project Selector (if projects exist)
                        if (projects.isNotEmpty()) {
                            SectionHeader("Project Assignment")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                FilterChip(
                                    selected = editedProjectId == null,
                                    onClick = { editedProjectId = null },
                                    label = { Text("None") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                projects.forEach { proj ->
                                    FilterChip(
                                        selected = editedProjectId == proj.id,
                                        onClick = { editedProjectId = proj.id },
                                        label = { Text(proj.name) },
                                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // Date & Time Selectors Side-by-Side
                        SectionHeader("Schedule")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    editedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val isPm = hour >= 12
                                            val h12 = if (hour % 12 == 0) 12 else hour % 12
                                            editedTime = String.format("%02d:%02d %s", h12, minute, if (isPm) "PM" else "AM")
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    editedTime ?: "Set Time",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Time Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val timePresets = listOf(
                                "09:00 AM" to "Morning 9 AM",
                                "02:00 PM" to "Afternoon 2 PM",
                                "06:00 PM" to "Evening 6 PM"
                            )
                            timePresets.forEach { (timeVal, label) ->
                                FilterChip(
                                    selected = editedTime == timeVal,
                                    onClick = { editedTime = if (editedTime == timeVal) null else timeVal },
                                    label = { Text(label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            if (editedTime != null) {
                                FilterChip(
                                    selected = false,
                                    onClick = { editedTime = null },
                                    label = { Text("Clear Time", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Repeat Schedule & Duration Side-by-Side Options
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SectionHeader("Repeat")
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("None", "Daily", "Weekly").forEach { r ->
                                        FilterChip(
                                            selected = editedRepeat == r,
                                            onClick = { editedRepeat = r },
                                            label = { Text(r, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Estimated Duration
                        SectionHeader("Estimated Duration")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(0 to "None", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h").forEach { (mins, label) ->
                                FilterChip(
                                    selected = editedEstimatedMinutes == mins,
                                    onClick = { editedEstimatedMinutes = mins },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Tracked Focus Time
                        SectionHeader("Tracked Time")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEditTrackedDialogInEditMode = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editedTrackedMinutes > 0) "${formatMinutes(editedTrackedMinutes)} tracked" else "0m tracked")
                            }
                        }

                        // Tags
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = editedTags,
                                onValueChange = { editedTags = it },
                                label = { Text("Tags (comma separated, e.g. #work, #urgent)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            val commonTags = listOf("#urgent", "#work", "#personal", "#study", "#health", "#finance")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                commonTags.forEach { tag ->
                                    val hasTag = editedTags.contains(tag, ignoreCase = true)
                                    FilterChip(
                                        selected = hasTag,
                                        onClick = {
                                            if (hasTag) {
                                                val clean = editedTags.split(",").map { it.trim() }.filter { !it.equals(tag, ignoreCase = true) && it.isNotBlank() }
                                                editedTags = clean.joinToString(", ")
                                            } else {
                                                editedTags = if (editedTags.isBlank()) tag else "$editedTags, $tag"
                                            }
                                        },
                                        label = { Text(tag, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Notes Field
                        OutlinedTextField(
                            value = editedNotes,
                            onValueChange = { editedNotes = it },
                            label = { Text("Notes & URLs") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp)
                        )

                    } else {
                        // ══════════════════════════════════════════
                        // ── VIEW MODE (Rich Cards & Info Rows) ──
                        // ══════════════════════════════════════════

                        // Title Banner
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 28.sp
                        )

                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            // Priority Badge
                            Surface(
                                color = priorityContainerColor.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = priorityColor, modifier = Modifier.size(12.dp))
                                    Text(task.priority, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = priorityColor)
                                }
                            }

                            // Category Chip
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Project Badge
                            if (!projectName.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(projectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // ── Focus Timer Section ──
                        FocusTimerCard(
                            task = task,
                            isTimerRunningForThisTask = isTimerRunning,
                            isTimerActiveForThisTask = isTimerActive,
                            activeOnOtherTask = activeOnOther,
                            elapsedSeconds = focusTimerElapsedSeconds,
                            onStartTimer = {
                                if (viewModel != null) viewModel.startFocusTimer(task)
                                else com.sg.taskspace.util.FocusTimerManager.startTimer(task.id, task.title)
                            },
                            onPauseTimer = {
                                if (viewModel != null) viewModel.pauseFocusTimer()
                                else com.sg.taskspace.util.FocusTimerManager.pauseTimer()
                            },
                            onResumeTimer = {
                                if (viewModel != null) viewModel.resumeFocusTimer()
                                else com.sg.taskspace.util.FocusTimerManager.resumeTimer()
                            },
                            onStopTimer = {
                                if (viewModel != null) {
                                    viewModel.stopFocusTimer(task)
                                } else {
                                    val res = com.sg.taskspace.util.FocusTimerManager.stopTimer()
                                    if (res != null && res.second > 0) {
                                        onUpdateTask(task.copy(trackedMinutes = task.trackedMinutes + res.second))
                                    }
                                }
                            },
                            onDiscardTimer = {
                                if (viewModel != null) viewModel.discardFocusTimer()
                                else com.sg.taskspace.util.FocusTimerManager.discardTimer()
                            },
                            onEditTrackedMinutes = { mins ->
                                if (viewModel != null) viewModel.updateTaskTrackedMinutes(task, mins)
                                else onUpdateTask(task.copy(trackedMinutes = mins))
                            }
                        )

                        // Metadata Grid
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoItem(
                                icon = Icons.Default.CalendarMonth,
                                label = "Scheduled Date",
                                value = try {
                                    LocalDate.parse(task.createdForDate)
                                        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                                } catch (e: Exception) { task.createdForDate }
                            )

                            if (!task.time.isNullOrBlank()) {
                                InfoItem(icon = Icons.Default.AccessTime, label = "Scheduled Time", value = task.time)
                            }

                            if (task.estimatedMinutes > 0) {
                                val dur = if (task.estimatedMinutes < 60) "${task.estimatedMinutes} mins"
                                else "${task.estimatedMinutes / 60}h ${if (task.estimatedMinutes % 60 > 0) "${task.estimatedMinutes % 60}m" else ""}".trim()
                                InfoItem(icon = Icons.Default.Schedule, label = "Estimated Duration", value = dur)
                            }

                            if (task.repeat != "None") {
                                InfoItem(icon = Icons.Default.Repeat, label = "Recurrence", value = task.repeat)
                            }
                        }

                        // Tags List
                        if (tagsList.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    tagsList.forEach { tag ->
                                        val tagStyle = TagStyleHelper.getStyleForTag(tag)
                                        Surface(
                                            color = tagStyle.color.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(tagStyle.icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = tagStyle.color)
                                                Text(tag, style = MaterialTheme.typography.labelSmall, color = tagStyle.color, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Notes Card
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text("Notes & Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                if (!task.notes.isNullOrBlank()) {
                                    TextButton(
                                        onClick = {
                                            val clip = ClipData.newPlainText("Task Notes", task.notes)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, "Notes copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            if (!task.notes.isNullOrBlank()) {
                                val notesText = task.notes
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
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
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    )
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No notes added for this task.",
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Bottom Action Bar ──
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing) {
                        TextButton(
                            onClick = { isEditing = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onUpdateTask(
                                    task.copy(
                                        title = editedTitle,
                                        notes = editedNotes,
                                        priority = editedPriority,
                                        category = editedCategory,
                                        projectId = editedProjectId,
                                        tags = editedTags,
                                        createdForDate = editedDate.format(DateTimeFormatter.ISO_DATE),
                                        repeat = editedRepeat,
                                        estimatedMinutes = editedEstimatedMinutes,
                                        trackedMinutes = editedTrackedMinutes,
                                        time = editedTime
                                    )
                                )
                                isEditing = false
                                Toast.makeText(context, "Task updated!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Changes")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onDeleteTask(task)
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }

                        Button(
                            onClick = { isEditing = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Task")
                        }
                    }
                }
            }
        }
    }

    if (showEditTrackedDialogInEditMode) {
        EditTrackedTimeDialog(
            currentMinutes = editedTrackedMinutes,
            onDismiss = { showEditTrackedDialogInEditMode = false },
            onConfirm = { mins ->
                editedTrackedMinutes = mins
                showEditTrackedDialogInEditMode = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
            }
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
