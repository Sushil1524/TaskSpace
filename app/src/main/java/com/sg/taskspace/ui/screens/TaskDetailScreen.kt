package com.sg.taskspace.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityHighContainer
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityLowContainer
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.PriorityMediumContainer
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val taskFlow = remember(taskId) { viewModel.getTaskById(taskId) }
    val task by taskFlow.collectAsState(initial = null)

    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editedNotes by remember { mutableStateOf("") }
    var editedPriority by remember { mutableStateOf("Medium") }

    LaunchedEffect(task) {
        task?.let {
            editedTitle = it.title
            editedNotes = it.notes ?: ""
            editedPriority = it.priority
        }
    }

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val priorityColor = when (task?.priority) {
        "High" -> PriorityHigh
        "Medium" -> PriorityMedium
        else -> PriorityLow
    }
    val priorityContainerColor = when (task?.priority) {
        "High" -> PriorityHighContainer
        "Medium" -> PriorityMediumContainer
        else -> PriorityLowContainer
    }

    // Completion animation
    val checkScale by animateFloatAsState(
        targetValue = if (task?.isCompleted == true) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "checkScale"
    )
    val checkBg by animateColorAsState(
        targetValue = if (task?.isCompleted == true) priorityColor else Color.Transparent,
        animationSpec = tween(220),
        label = "checkBg"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Task" else "Task Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (isEditing) isEditing = false else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (task != null) {
                        if (isEditing) {
                            IconButton(onClick = {
                                viewModel.updateTaskDetails(
                                    task!!.copy(
                                        title = editedTitle,
                                        notes = editedNotes,
                                        priority = editedPriority
                                    )
                                )
                                isEditing = false
                                Toast.makeText(context, "Task updated", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                viewModel.deleteTask(task!!)
                                onNavigateBack()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (isEditing) {
            // ── Edit Mode ──
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 4
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        val pColor = when (p) { "High" -> PriorityHigh; "Medium" -> PriorityMedium; else -> PriorityLow }
                        val pContainer = when (p) { "High" -> PriorityHighContainer; "Medium" -> PriorityMediumContainer; else -> PriorityLowContainer }
                        FilterChip(
                            selected = editedPriority == p,
                            onClick = { editedPriority = p },
                            label = { Text(p, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pContainer,
                                selectedLabelColor = pColor
                            )
                        )
                    }
                }
            }
        } else {
            // ── View Mode ──
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header Card with gradient accent ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = priorityContainerColor.copy(alpha = 0.25f)
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Completion toggle + Title
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Animated checkbox
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .scale(checkScale)
                                    .clip(CircleShape)
                                    .border(2.dp, priorityColor, CircleShape)
                                    .background(checkBg)
                                    .clickable {
                                        val next = !(task!!.isCompleted)
                                        viewModel.updateTaskDetails(task!!.copy(isCompleted = next))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (task!!.isCompleted) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (task!!.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (task!!.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 30.sp
                                )
                            }
                        }

                        // Priority badge
                        Surface(
                            color = priorityContainerColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = priorityColor, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${task!!.priority} Priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = priorityColor
                                )
                            }
                        }
                    }
                }

                // ── Info Rows ──
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailInfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Scheduled For",
                        value = try {
                            LocalDate.parse(task!!.createdForDate)
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                        } catch (e: Exception) { task!!.createdForDate }
                    )

                    task!!.time?.let { time ->
                        DetailInfoRow(icon = Icons.Default.Schedule, label = "Time", value = time)
                    }

                    DetailInfoRow(icon = Icons.Default.Category, label = "Category", value = task!!.category)

                    if (task!!.estimatedMinutes > 0) {
                        val dur = if (task!!.estimatedMinutes < 60) "${task!!.estimatedMinutes}m"
                                  else "${task!!.estimatedMinutes / 60}h ${if (task!!.estimatedMinutes % 60 > 0) "${task!!.estimatedMinutes % 60}m" else ""}".trim()
                        DetailInfoRow(icon = Icons.Default.Timer, label = "Estimated Duration", value = dur)
                    }

                    if (task!!.repeat != "None") {
                        DetailInfoRow(icon = Icons.Default.Repeat, label = "Repeat", value = task!!.repeat)
                    }

                    if (task!!.isCompleted) {
                        DetailInfoRow(
                            icon = Icons.Default.CheckCircle,
                            label = "Status",
                            value = "Completed ✓",
                            valueColor = PriorityLow
                        )
                    }
                }

                // ── Tags ──
                if (task!!.tags.isNotBlank()) {
                    val tags = task!!.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { tag ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

                // ── Notes Section ──
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (!task!!.notes.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Task Notes", task!!.notes!!)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Notes copied", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (!task!!.notes.isNullOrBlank()) {
                        val notesText = task!!.notes!!
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp)
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
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable { isEditing = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text("Tap to add notes...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
            )
        }
    }
}
