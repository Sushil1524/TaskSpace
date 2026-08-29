package com.sg.taskspace.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityHighContainer
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityLowContainer
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.PriorityMediumContainer
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.util.AudioHapticHelper
import com.sg.taskspace.util.TagStyleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (() -> Unit)? = null,
    projectName: String? = null,
    swipeRightAction: String = "Complete",
    swipeLeftAction: String = "Delete",
    soundEffectsEnabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val priorityColor = when (task.priority) {
        "High" -> PriorityHigh
        "Medium" -> PriorityMedium
        else -> PriorityLow
    }
    val priorityContainerColor = when (task.priority) {
        "High" -> PriorityHighContainer
        "Medium" -> PriorityMediumContainer
        else -> PriorityLowContainer
    }

    val tagsList = remember(task.tags) {
        if (task.tags.isBlank()) emptyList()
        else task.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    // ── Completion Radial Ripple Animation ──
    val rippleProgress = remember { Animatable(0f) }
    var isRippling by remember { mutableStateOf(false) }
    LaunchedEffect(task.isCompleted) {
        if (task.isCompleted) {
            isRippling = true
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
            )
            isRippling = false
        } else {
            isRippling = false
            rippleProgress.snapTo(0f)
        }
    }

    // ── Animated checkbox visual feedback ──
    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "checkScale"
    )
    val checkBg by animateColorAsState(
        targetValue = if (task.isCompleted) priorityColor else Color.Transparent,
        animationSpec = tween(200),
        label = "checkBg"
    )
    val checkBorder by animateColorAsState(
        targetValue = if (task.isCompleted) priorityColor else priorityColor.copy(alpha = 0.6f),
        animationSpec = tween(200),
        label = "checkBorder"
    )

    var hasHandledSwipe by remember(task.id, task.isCompleted) { mutableStateOf(false) }

    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (!hasHandledSwipe && dismissValue != SwipeToDismissBoxValue.Settled) {
                hasHandledSwipe = true
                val action = if (dismissValue == SwipeToDismissBoxValue.StartToEnd) swipeRightAction else swipeLeftAction
                when (action) {
                    "Complete" -> {
                        val nextState = !task.isCompleted
                        if (nextState) {
                            AudioHapticHelper.playTaskCompleteSoundAndHaptic(
                                context = context,
                                hapticFeedback = haptic,
                                soundEnabled = soundEffectsEnabled,
                                hapticEnabled = soundEffectsEnabled
                            )
                        }
                        onCheckedChange(nextState)
                    }
                    "Delete" -> onDelete()
                    "Edit" -> onClick()
                    "Snooze" -> onSnooze?.invoke()
                }
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = swipeRightAction != "None",
        enableDismissFromEndToStart = swipeLeftAction != "None",
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> swipeRightAction
                SwipeToDismissBoxValue.EndToStart -> swipeLeftAction
                else -> "None"
            }

            val (bgColor, icon, label) = when (action) {
                "Complete" -> Triple(SecondaryEmerald, Icons.Default.Check, "Complete")
                "Delete" -> Triple(PriorityHigh, Icons.Default.Delete, "Delete")
                "Edit" -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.Edit, "Edit")
                "Snooze" -> Triple(PriorityMedium, Icons.Default.WbSunny, "Snooze")
                else -> Triple(Color.Transparent, null, "")
            }

            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

            if (icon != null && action != "None") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(icon, contentDescription = label, tint = Color.White)
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Icon(icon, contentDescription = label, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .drawWithContent {
                    drawContent()
                    if (isRippling) {
                        val progress = rippleProgress.value
                        val maxRadius = size.width * 1.25f
                        val radius = maxRadius * progress
                        val alpha = ((1f - progress) * 0.65f).coerceIn(0f, 0.65f)
                        val origin = androidx.compose.ui.geometry.Offset(
                            x = 28.dp.toPx(),
                            y = size.height / 2f
                        )
                        clipRect {
                            drawCircle(
                                color = SecondaryEmerald.copy(alpha = alpha),
                                radius = radius,
                                center = origin
                            )
                        }
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Priority accent bar on left edge
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(
                                color = if (!task.isCompleted) priorityColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                    )
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                            .alpha(if (task.isCompleted) 0.6f else 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated Circular Checkbox — roomier bounds & Surface BorderStroke prevent left cutoff
                        Box(
                            modifier = Modifier
                                .scale(checkScale)
                                .size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        val nextState = !task.isCompleted
                                        if (nextState) {
                                            AudioHapticHelper.playTaskCompleteSoundAndHaptic(
                                                context = context,
                                                hapticFeedback = haptic,
                                                soundEnabled = soundEffectsEnabled,
                                                hapticEnabled = soundEffectsEnabled
                                            )
                                        }
                                        onCheckedChange(nextState)
                                    },
                                shape = CircleShape,
                                color = checkBg,
                                border = androidx.compose.foundation.BorderStroke(2.dp, checkBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (task.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }


                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!task.isCompleted) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Priority chip
                                    Surface(
                                        color = priorityContainerColor.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = task.priority,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            color = priorityColor,
                                            fontSize = 10.sp
                                        )
                                    }

                                    // Category chip
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = task.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }

                                    // Project Chip
                                    if (!projectName.isNullOrBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderOpen,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = projectName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    // Smart Tag chips
                                    for (tag in tagsList.take(2)) {
                                        val tagStyle = TagStyleHelper.getStyleForTag(tag)
                                        Surface(
                                            color = tagStyle.color.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = tagStyle.icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = tagStyle.color
                                                )
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = tagStyle.color,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    // Duration badge
                                    if (task.estimatedMinutes > 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = formatMinutes(task.estimatedMinutes),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatMinutes(minutes: Int): String {
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${if (minutes % 60 > 0) "${minutes % 60}m" else ""}".trim()
}
