package com.sg.taskspace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sg.taskspace.data.BackupDiffResult
import com.sg.taskspace.data.CategoryRestoreConfig
import com.sg.taskspace.data.EntityDiff
import com.sg.taskspace.data.ItemDiffDetails
import com.sg.taskspace.data.ItemDiffType
import com.sg.taskspace.data.RestoreSelectionConfig
import com.sg.taskspace.data.RestoreStrategy

// Semantic diff badge indicator colors
private val DiffAddedColor = Color(0xFF22C55E)       // green-500
private val DiffModifiedColor = Color(0xFFF59E0B)    // amber-500
private val DiffDeletedColor = Color(0xFFEF4444)     // red-500

@Composable
fun BackupDiffDialog(
    diff: BackupDiffResult,
    onConfirmWithSelection: (RestoreSelectionConfig) -> Unit,
    onDismiss: () -> Unit,
    isRestoring: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Per-category strategies
    var taskStrategy by remember { mutableStateOf(RestoreStrategy.MERGE_BACKUP_WINS) }
    var habitStrategy by remember { mutableStateOf(RestoreStrategy.MERGE_BACKUP_WINS) }
    var journalStrategy by remember { mutableStateOf(RestoreStrategy.MERGE_BACKUP_WINS) }
    var goalStrategy by remember { mutableStateOf(RestoreStrategy.MERGE_BACKUP_WINS) }
    var projectStrategy by remember { mutableStateOf(RestoreStrategy.MERGE_BACKUP_WINS) }
    var restorePreferences by remember { mutableStateOf(true) }

    // Per-category excluded item IDs (item-level toggles)
    var excludedTaskIds by remember { mutableStateOf(setOf<String>()) }
    var excludedHabitIds by remember { mutableStateOf(setOf<String>()) }
    var excludedJournalIds by remember { mutableStateOf(setOf<String>()) }
    var excludedGoalIds by remember { mutableStateOf(setOf<String>()) }
    var excludedProjectIds by remember { mutableStateOf(setOf<String>()) }

    Dialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(380)) { it / 2 }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Drag handle ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Header ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Selective Restore & Diff",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (diff.hasAnyChanges)
                                                "${diff.totalChanges} change${if (diff.totalChanges != 1) "s" else ""} detected · Select what to keep"
                                            else "Your data is already up to date",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = { if (!isRestoring) onDismiss() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Presets Bar ──
                            Text(
                                text = "Quick Strategy Presets:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = taskStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                            habitStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                            journalStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                            goalStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                            projectStrategy == RestoreStrategy.MERGE_BACKUP_WINS,
                                    onClick = {
                                        taskStrategy = RestoreStrategy.MERGE_BACKUP_WINS
                                        habitStrategy = RestoreStrategy.MERGE_BACKUP_WINS
                                        journalStrategy = RestoreStrategy.MERGE_BACKUP_WINS
                                        goalStrategy = RestoreStrategy.MERGE_BACKUP_WINS
                                        projectStrategy = RestoreStrategy.MERGE_BACKUP_WINS
                                    },
                                    label = { Text("Merge All (Safe)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = taskStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                                habitStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                                journalStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                                goalStrategy == RestoreStrategy.MERGE_BACKUP_WINS &&
                                                projectStrategy == RestoreStrategy.MERGE_BACKUP_WINS,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                FilterChip(
                                    selected = taskStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                            habitStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                            journalStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                            goalStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                            projectStrategy == RestoreStrategy.REPLACE_LOCAL,
                                    onClick = {
                                        taskStrategy = RestoreStrategy.REPLACE_LOCAL
                                        habitStrategy = RestoreStrategy.REPLACE_LOCAL
                                        journalStrategy = RestoreStrategy.REPLACE_LOCAL
                                        goalStrategy = RestoreStrategy.REPLACE_LOCAL
                                        projectStrategy = RestoreStrategy.REPLACE_LOCAL
                                    },
                                    label = { Text("Replace All") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = taskStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                                habitStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                                journalStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                                goalStrategy == RestoreStrategy.REPLACE_LOCAL &&
                                                projectStrategy == RestoreStrategy.REPLACE_LOCAL,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                FilterChip(
                                    selected = taskStrategy == RestoreStrategy.KEEP_LOCAL &&
                                            habitStrategy == RestoreStrategy.KEEP_LOCAL &&
                                            journalStrategy == RestoreStrategy.KEEP_LOCAL &&
                                            goalStrategy == RestoreStrategy.KEEP_LOCAL &&
                                            projectStrategy == RestoreStrategy.KEEP_LOCAL,
                                    onClick = {
                                        taskStrategy = RestoreStrategy.KEEP_LOCAL
                                        habitStrategy = RestoreStrategy.KEEP_LOCAL
                                        journalStrategy = RestoreStrategy.KEEP_LOCAL
                                        goalStrategy = RestoreStrategy.KEEP_LOCAL
                                        projectStrategy = RestoreStrategy.KEEP_LOCAL
                                    },
                                    label = { Text("Keep Local All") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = taskStrategy == RestoreStrategy.KEEP_LOCAL &&
                                                habitStrategy == RestoreStrategy.KEEP_LOCAL &&
                                                journalStrategy == RestoreStrategy.KEEP_LOCAL &&
                                                goalStrategy == RestoreStrategy.KEEP_LOCAL &&
                                                projectStrategy == RestoreStrategy.KEEP_LOCAL,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Legend ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DiffLegendChip(color = DiffAddedColor, label = "Added in Backup")
                                DiffLegendChip(color = DiffModifiedColor, label = "Modified")
                                DiffLegendChip(color = DiffDeletedColor, label = "Missing in Backup")
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ── Interactive Category Rows ──
                            CategoryDiffCard(
                                icon = Icons.Default.CheckCircle,
                                iconTint = MaterialTheme.colorScheme.primary,
                                label = "Tasks",
                                diff = diff.tasks,
                                currentStrategy = taskStrategy,
                                onStrategySelected = { taskStrategy = it },
                                excludedIds = excludedTaskIds,
                                onToggleItem = { id ->
                                    excludedTaskIds = if (id in excludedTaskIds) excludedTaskIds - id else excludedTaskIds + id
                                }
                            )

                            CategoryDiffCard(
                                icon = Icons.Default.Loop,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                label = "Habits",
                                diff = diff.habits,
                                currentStrategy = habitStrategy,
                                onStrategySelected = { habitStrategy = it },
                                excludedIds = excludedHabitIds,
                                onToggleItem = { id ->
                                    excludedHabitIds = if (id in excludedHabitIds) excludedHabitIds - id else excludedHabitIds + id
                                }
                            )

                            CategoryDiffCard(
                                icon = Icons.Default.Book,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                label = "Journal Entries",
                                diff = diff.journalEntries,
                                currentStrategy = journalStrategy,
                                onStrategySelected = { journalStrategy = it },
                                excludedIds = excludedJournalIds,
                                onToggleItem = { id ->
                                    excludedJournalIds = if (id in excludedJournalIds) excludedJournalIds - id else excludedJournalIds + id
                                }
                            )

                            CategoryDiffCard(
                                icon = Icons.Default.Flag,
                                iconTint = Color(0xFFF43F5E), // Rose
                                label = "Goals",
                                diff = diff.goals,
                                currentStrategy = goalStrategy,
                                onStrategySelected = { goalStrategy = it },
                                excludedIds = excludedGoalIds,
                                onToggleItem = { id ->
                                    excludedGoalIds = if (id in excludedGoalIds) excludedGoalIds - id else excludedGoalIds + id
                                }
                            )

                            CategoryDiffCard(
                                icon = Icons.Default.FolderOpen,
                                iconTint = Color(0xFF38BDF8), // Sky Blue
                                label = "Projects",
                                diff = diff.projects,
                                currentStrategy = projectStrategy,
                                onStrategySelected = { projectStrategy = it },
                                excludedIds = excludedProjectIds,
                                onToggleItem = { id ->
                                    excludedProjectIds = if (id in excludedProjectIds) excludedProjectIds - id else excludedProjectIds + id
                                }
                            )

                            // ── User Preferences Switch ──
                            if (diff.parsedBackup.userPreferences != null) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Restore App Settings & Preferences",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Theme, fonts, swipe actions & app lock PIN",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = restorePreferences,
                                            onCheckedChange = { restorePreferences = it }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // ── Bottom Action bar ──
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    enabled = !isRestoring,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        "Cancel",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val selectionConfig = RestoreSelectionConfig(
                                            taskConfig = CategoryRestoreConfig(taskStrategy, excludedTaskIds),
                                            habitConfig = CategoryRestoreConfig(habitStrategy, excludedHabitIds),
                                            journalConfig = CategoryRestoreConfig(journalStrategy, excludedJournalIds),
                                            goalConfig = CategoryRestoreConfig(goalStrategy, excludedGoalIds),
                                            projectConfig = CategoryRestoreConfig(projectStrategy, excludedProjectIds),
                                            restorePreferences = restorePreferences
                                        )
                                        onConfirmWithSelection(selectionConfig)
                                    },
                                    enabled = !isRestoring,
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (isRestoring) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apply Selection", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Backwards compatibility overload
@Composable
fun BackupDiffDialog(
    diff: BackupDiffResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isRestoring: Boolean = false
) {
    BackupDiffDialog(
        diff = diff,
        onConfirmWithSelection = { _ -> onConfirm() },
        onDismiss = onDismiss,
        isRestoring = isRestoring
    )
}

@Composable
private fun DiffLegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CategoryDiffCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    diff: EntityDiff,
    currentStrategy: RestoreStrategy,
    onStrategySelected: (RestoreStrategy) -> Unit,
    excludedIds: Set<String>,
    onToggleItem: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showStrategyMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (diff.items.isNotEmpty()) expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Badges
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (diff.added > 0) {
                            DiffBadge(count = diff.added, color = DiffAddedColor, prefix = "+")
                        }
                        if (diff.modified > 0) {
                            DiffBadge(count = diff.modified, color = DiffModifiedColor, prefix = "~")
                        }
                        if (diff.deleted > 0) {
                            DiffBadge(count = diff.deleted, color = DiffDeletedColor, prefix = "-")
                        }
                        if (!diff.hasChanges) {
                            Text(
                                text = "No changes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Strategy Picker Dropdown Anchor
                Box {
                    AssistChip(
                        onClick = { showStrategyMenu = true },
                        label = {
                            Text(
                                text = when (currentStrategy) {
                                    RestoreStrategy.MERGE_BACKUP_WINS -> "Merge (Backup Wins)"
                                    RestoreStrategy.MERGE_LOCAL_WINS -> "Merge (Keep Local)"
                                    RestoreStrategy.REPLACE_LOCAL -> "Replace Category"
                                    RestoreStrategy.KEEP_LOCAL -> "Keep Local (Skip)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (currentStrategy) {
                                RestoreStrategy.MERGE_BACKUP_WINS, RestoreStrategy.MERGE_LOCAL_WINS -> MaterialTheme.colorScheme.surface
                                RestoreStrategy.REPLACE_LOCAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                RestoreStrategy.KEEP_LOCAL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            },
                            labelColor = when (currentStrategy) {
                                RestoreStrategy.REPLACE_LOCAL -> MaterialTheme.colorScheme.onErrorContainer
                                RestoreStrategy.KEEP_LOCAL -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )

                    DropdownMenu(
                        expanded = showStrategyMenu,
                        onDismissRequest = { showStrategyMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        RestoreStrategy.entries.forEach { strategy ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = strategy.label,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = strategy.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onStrategySelected(strategy)
                                    showStrategyMenu = false
                                }
                            )
                        }
                    }
                }

                if (diff.items.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand item list",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable Itemized Checklist
            AnimatedVisibility(
                visible = expanded && diff.items.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Item-level selection (${diff.items.size} items):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    diff.items.forEach { item ->
                        val isChecked = item.id !in excludedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(item.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleItem(item.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )

                            Text(
                                text = item.title,
                                fontSize = 13.sp,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )

                            // Item Diff Type Badge
                            val (badgeColor, prefix) = when (item.diffType) {
                                ItemDiffType.ADDED -> Pair(DiffAddedColor, "+Added")
                                ItemDiffType.MODIFIED -> Pair(DiffModifiedColor, "~Modified")
                                ItemDiffType.DELETED_IN_BACKUP -> Pair(DiffDeletedColor, "-Deleted in backup")
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = prefix,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffBadge(count: Int, color: Color, prefix: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$prefix$count",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 12.sp
        )
    }
}
