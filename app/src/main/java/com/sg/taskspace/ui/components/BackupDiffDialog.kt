package com.sg.taskspace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sg.taskspace.data.BackupDiffResult
import com.sg.taskspace.data.EntityDiff

// Color palette for diff badges
private val DiffAddedColor = Color(0xFF22C55E)       // green-500
private val DiffAddedBg = Color(0xFF14532D)          // green-900
private val DiffModifiedColor = Color(0xFFF59E0B)    // amber-500
private val DiffModifiedBg = Color(0xFF78350F)       // amber-900
private val DiffDeletedColor = Color(0xFFEF4444)     // red-500
private val DiffDeletedBg = Color(0xFF7F1D1D)        // red-900
private val DiffNeutralBg = Color(0xFF1E293B)        // slate-800

@Composable
fun BackupDiffDialog(
    diff: BackupDiffResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isRestoring: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

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
                        .fillMaxHeight(0.88f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Drag handle ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))

                            // ── Header ──
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Backup Preview",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (diff.hasAnyChanges)
                                            "${diff.totalChanges} change${if (diff.totalChanges != 1) "s" else ""} detected"
                                        else "Your data is already up to date",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // ── Warning banner ──
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF7F1D1D).copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = DiffDeletedColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Restoring will REPLACE all current data. This cannot be undone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DiffDeletedColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // ── Legend ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DiffLegendChip(color = DiffAddedColor, label = "Added")
                                DiffLegendChip(color = DiffModifiedColor, label = "Modified")
                                DiffLegendChip(color = DiffDeletedColor, label = "Deleted")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Diff rows ──
                            DiffCategoryRow(
                                icon = Icons.Default.CheckCircle,
                                iconTint = Color(0xFF818CF8),
                                label = "Tasks",
                                diff = diff.tasks
                            )
                            DiffCategoryRow(
                                icon = Icons.Default.Loop,
                                iconTint = Color(0xFF34D399),
                                label = "Habits",
                                diff = diff.habits
                            )
                            DiffCategoryRow(
                                icon = Icons.Default.Book,
                                iconTint = Color(0xFFFBBF24),
                                label = "Journal Entries",
                                diff = diff.journalEntries
                            )
                            DiffCategoryRow(
                                icon = Icons.Default.Flag,
                                iconTint = Color(0xFFF87171),
                                label = "Goals",
                                diff = diff.goals
                            )
                            DiffCategoryRow(
                                icon = Icons.Default.FolderOpen,
                                iconTint = Color(0xFF60A5FA),
                                label = "Projects",
                                diff = diff.projects
                            )

                            // ── No changes state ──
                            if (!diff.hasAnyChanges) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = DiffAddedBg.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = DiffAddedColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "The backup matches your current data exactly. No changes will occur.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DiffAddedColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // ── Action buttons ──
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0F172A),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Cancel
                                OutlinedButton(
                                    onClick = onDismiss,
                                    enabled = !isRestoring,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        "Cancel",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Restore Now
                                Button(
                                    onClick = onConfirm,
                                    enabled = !isRestoring,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DiffDeletedColor,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isRestoring) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.RestoreFromTrash,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore Now", fontWeight = FontWeight.Bold)
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
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DiffCategoryRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    diff: EntityDiff
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = DiffNeutralBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
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

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Diff badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (diff.added > 0) {
                    DiffBadge(count = diff.added, color = DiffAddedColor, bg = DiffAddedBg, prefix = "+")
                }
                if (diff.modified > 0) {
                    DiffBadge(count = diff.modified, color = DiffModifiedColor, bg = DiffModifiedBg, prefix = "~")
                }
                if (diff.deleted > 0) {
                    DiffBadge(count = diff.deleted, color = DiffDeletedColor, bg = DiffDeletedBg, prefix = "-")
                }
                if (!diff.hasChanges) {
                    Text(
                        text = "No changes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffBadge(count: Int, color: Color, bg: Color, prefix: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg.copy(alpha = 0.6f)
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
