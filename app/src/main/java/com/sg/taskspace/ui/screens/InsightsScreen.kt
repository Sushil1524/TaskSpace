package com.sg.taskspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.PrimaryIndigo
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val insightsData by viewModel.getInsightsData(currentMonth).collectAsState(initial = null)
    val isCurrentMonth = currentMonth == YearMonth.now()

    // Category colors
    val categoryColors = listOf(
        PrimaryIndigo, SecondaryEmerald, PriorityHigh, PriorityMedium, PriorityLow,
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Month Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrentMonth) {
                            Text("Current Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        enabled = !isCurrentMonth
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = if (!isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            if (insightsData == null) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val data = insightsData!!

                // ── Hero completion card ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(PrimaryIndigo.copy(alpha = 0.85f), SecondaryEmerald.copy(alpha = 0.75f))),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Monthly Completion", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${data.completionRateThisMonth}%",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${data.completedTasksThisMonth} of ${data.totalTasksThisMonth} tasks done",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { data.completionRateThisMonth / 100f },
                                    modifier = Modifier.width(160.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f)
                                )
                            }
                            // Circular progress donut
                            Box(
                                modifier = Modifier.size(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { data.completionRateThisMonth / 100f },
                                    modifier = Modifier.size(72.dp),
                                    strokeWidth = 7.dp,
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Text(
                                    "${data.completionRateThisMonth}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // ── Highlights row ──
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    InsightHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = "🔥 Streak",
                        value = "${data.currentStreak}d",
                        subtitle = "current",
                        iconColor = Color(0xFFEF4444),
                        bgColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                    )
                    InsightHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = "⭐ Best Day",
                        value = data.bestDayOfWeek.take(3),
                        subtitle = "this month",
                        iconColor = Color(0xFFF59E0B),
                        bgColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
                    )
                    InsightHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = "🎯 Focus",
                        value = data.needsFocus,
                        subtitle = "needs work",
                        iconColor = PriorityHigh,
                        bgColor = PriorityHigh.copy(alpha = 0.1f)
                    )
                }

                // ── Priority breakdown ──
                InsightSectionHeader("Priority Breakdown", Icons.Default.PriorityHigh)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val priorities = listOf(
                            Triple("High", PriorityHigh, data.categoryDistribution.entries.sumOf { if (it.key.contains("High", true)) it.value.first else 0 }),
                            Triple("Medium", PriorityMedium, data.categoryDistribution.entries.sumOf { if (it.key.contains("Medium", true)) it.value.first else 0 }),
                            Triple("Low", PriorityLow, data.categoryDistribution.entries.sumOf { if (it.key.contains("Low", true)) it.value.first else 0 })
                        )
                        val maxPriority = maxOf(priorities.maxOf { it.third }, 1)
                        priorities.forEach { (label, color, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp))
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / maxPriority },
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }

                // ── Category breakdown ──
                InsightSectionHeader("Category Breakdown", Icons.Default.Category)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (data.categoryDistribution.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No tasks this month", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    } else {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val maxTotal = maxOf(data.categoryDistribution.values.maxOf { it.first }, 1)
                            data.categoryDistribution.entries.sortedByDescending { it.value.first }.forEachIndexed { i, (category, stats) ->
                                val (total, completed) = stats
                                val rate = if (total > 0) completed.toFloat() / total else 0f
                                val color = categoryColors.getOrElse(i) { PrimaryIndigo }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                            Text(category.ifEmpty { "Uncategorized" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("$completed/$total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = color, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { rate },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Motivational summary ──
                val motivationMsg = when {
                    data.completionRateThisMonth >= 90 -> "🏆 Outstanding! You're crushing it this month!"
                    data.completionRateThisMonth >= 70 -> "🚀 Great momentum! Keep pushing forward."
                    data.completionRateThisMonth >= 50 -> "💪 Good progress. Half-way there!"
                    data.completionRateThisMonth > 0 -> "📈 Building momentum. Every task counts."
                    else -> "🌱 Start tracking tasks to see your insights here."
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Text(motivationMsg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InsightHighlightCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtitle: String,
    iconColor: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = if (value.length > 6) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = iconColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InsightSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

// Keep existing HighlightCard, StatItem, CategoryRow for backward compat
@Composable
fun HighlightCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, iconColor: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CategoryRow(category: String, count: Int, completed: Int) {
    val completionRate = if (count > 0) completed.toFloat() / count else 0f
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(category.ifEmpty { "Uncategorized" }, modifier = Modifier.weight(1f))
        Text("$completed/$count", fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { completionRate },
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
