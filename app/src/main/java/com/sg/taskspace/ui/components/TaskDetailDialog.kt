package com.sg.taskspace.ui.components

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.Task
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
    var editedTime by remember { mutableStateOf(task.time) }

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

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Task" else "Task Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isEditing) {
                    // --- EDIT MODE ---
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("Task Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Notes
                        OutlinedTextField(
                            value = editedNotes,
                            onValueChange = { editedNotes = it },
                            label = { Text("Notes (Supports links & URLs)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Priority
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf("High", "Medium", "Low").forEach { p ->
                                    FilterChip(
                                        selected = editedPriority == p,
                                        onClick = { editedPriority = p },
                                        label = { Text(p) }
                                    )
                                }
                            }
                        }

                        // Category
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf("General", "Personal", "Work", "Study", "Finance", "Health", "Urgent").forEach { c ->
                                    FilterChip(
                                        selected = editedCategory == c,
                                        onClick = { editedCategory = c },
                                        label = { Text(c) }
                                    )
                                }
                            }
                        }

                        // Project Selector (if projects exist)
                        if (projects.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Assign Project", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    FilterChip(
                                        selected = editedProjectId == null,
                                        onClick = { editedProjectId = null },
                                        label = { Text("None") }
                                    )
                                    projects.forEach { proj ->
                                        FilterChip(
                                            selected = editedProjectId == proj.id,
                                            onClick = { editedProjectId = proj.id },
                                            label = { Text(proj.name) },
                                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                }
                            }
                        }

                        // Tags
                        OutlinedTextField(
                            value = editedTags,
                            onValueChange = { editedTags = it },
                            label = { Text("Tags (comma separated, e.g. #work, #urgent)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Date & Time Picker Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Date Picker Button
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(editedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), fontSize = 11.sp)
                            }

                            // Time Picker Button
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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(editedTime ?: "Set Time", fontSize = 11.sp)
                            }
                        }

                        // Repeat Schedule
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Repeat Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("None", "Daily", "Weekly").forEach { r ->
                                    FilterChip(
                                        selected = editedRepeat == r,
                                        onClick = { editedRepeat = r },
                                        label = { Text(r) }
                                    )
                                }
                            }
                        }

                        // Estimated Duration
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Estimated Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf(0 to "None", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 120 to "2h").forEach { (mins, label) ->
                                    FilterChip(
                                        selected = editedEstimatedMinutes == mins,
                                        onClick = { editedEstimatedMinutes = mins },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                                        time = editedTime
                                    )
                                )
                                isEditing = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Changes")
                        }
                    }

                } else {
                    // --- VIEW MODE ---
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Badges Row 1: Priority, Category, Project
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            AssistChip(
                                onClick = { },
                                label = { Text(task.priority) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when (task.priority) {
                                        "High" -> MaterialTheme.colorScheme.errorContainer
                                        "Medium" -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                )
                            )
                            AssistChip(
                                onClick = { },
                                label = { Text(task.category) }
                            )
                            if (!projectName.isNullOrBlank()) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text(projectName) },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        }

                        // Badges Row 2: Date, Time, Repeat, Duration, Tags
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            // Scheduled Date Chip
                            AssistChip(
                                onClick = { },
                                label = { Text(task.createdForDate) },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )

                            if (!task.time.isNullOrBlank()) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text(task.time) },
                                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            if (task.repeat != "None") {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Repeat: ${task.repeat}") },
                                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            if (task.estimatedMinutes > 0) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("${task.estimatedMinutes} mins") },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }

                        // Tags List
                        if (tagsList.isNotEmpty()) {
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Notes Section
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (!task.notes.isNullOrBlank()) {
                            val notesText = task.notes

                            // Copy Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        val clip = ClipData.newPlainText("Task Notes", notesText)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast.makeText(context, "Notes copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
                            Text("No notes added.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { onDeleteTask(task); onDismissRequest() },
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Task")
                        }
                    }
                }
            }
        }
    }
}
