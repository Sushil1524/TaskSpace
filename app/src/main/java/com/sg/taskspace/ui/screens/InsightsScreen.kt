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
import androidx.compose.ui.platform.LocalContext
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.HabitLog
import com.sg.taskspace.data.Task
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.PrimaryIndigo
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.ui.viewmodel.AchievementCategory
import com.sg.taskspace.ui.viewmodel.AchievementsViewModel
import com.sg.taskspace.ui.viewmodel.HabitViewModel
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.util.ReportExporter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: TaskViewModel,
    habitViewModel: HabitViewModel? = null,
    projectViewModel: com.sg.taskspace.ui.viewmodel.ProjectViewModel? = null,
    journalViewModel: com.sg.taskspace.ui.viewmodel.JournalViewModel? = null,
    achievementsViewModel: AchievementsViewModel? = null,
    onNavigateToJournal: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val insightsData by viewModel.getInsightsData(currentMonth).collectAsState(initial = null)
    val isCurrentMonth = currentMonth == YearMonth.now()

    val allTasks by viewModel.allTasks.collectAsState()
    val habitsState = habitViewModel?.habits?.collectAsState()
    val habits: List<Habit> = habitsState?.value ?: emptyList()

    val allLogsState = habitViewModel?.allLogs?.collectAsState()
    val allHabitLogs: List<HabitLog> = allLogsState?.value ?: emptyList()

    val habitStatsState = habitViewModel?.habitStats?.collectAsState()
    val habitStats: Map<String, HabitViewModel.HabitStats> = habitStatsState?.value ?: emptyMap()

    val projectsState = projectViewModel?.projects?.collectAsState()
    val projects = projectsState?.value ?: emptyList()

    val journalEntriesState = journalViewModel?.allEntries?.collectAsState()
    val allJournalEntries = journalEntriesState?.value ?: emptyList()
    val journalStats = remember(currentMonth, allJournalEntries) {
        journalViewModel?.computeJournalStats(currentMonth, allJournalEntries)
    }

    val achievementsUiState by achievementsViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }

    var showExportDialog by remember { mutableStateOf(false) }

    // Category colors
    val categoryColors = listOf(
        PrimaryIndigo, SecondaryEmerald, PriorityHigh, PriorityMedium, PriorityLow,
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Report")
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
                        label = "🔥 Task Streak",
                        value = "${data.currentStreak}d",
                        subtitle = "current",
                        iconColor = Color(0xFFEF4444),
                        bgColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                    )
                    InsightHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = "⭐ Best Task Day",
                        value = data.bestDayOfWeek.take(3),
                        subtitle = "this month",
                        iconColor = Color(0xFFF59E0B),
                        bgColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
                    )
                    InsightHighlightCard(
                        modifier = Modifier.weight(1f),
                        label = "🎯 Focus Area",
                        value = data.needsFocus,
                        subtitle = "needs work",
                        iconColor = PriorityHigh,
                        bgColor = PriorityHigh.copy(alpha = 0.1f)
                    )
                }

                // ── Today's Reflection Status / Smart Suggestion Banner ──
                if (journalStats != null) {
                    if (!journalStats.hasJournaledToday) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.EditNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Today's Reflection Needed",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "You haven't logged your mood or journal today. Take 2 minutes to reflect!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onNavigateToJournal,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Write", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        val todayEntry = journalStats.todayEntry
                        val todayEmoji = when (todayEntry?.mood?.lowercase()) {
                            "great" -> "🤩"
                            "good", "happy" -> "😊"
                            "neutral", "calm" -> "😐"
                            "bad", "sad" -> "😔"
                            "awful", "anxious", "angry" -> "😫"
                            else -> "✨"
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SecondaryEmerald.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = SecondaryEmerald.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(todayEmoji, fontSize = 20.sp)
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Today's Reflection Logged ($todayEmoji ${todayEntry?.mood ?: ""})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val previewText = todayEntry?.content?.replace("\n", " ")?.take(65) ?: ""
                                        Text(
                                            text = if (previewText.isNotBlank()) "\"$previewText...\"" else "Reflection saved.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = onNavigateToJournal,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // ── Habit Analytics Section ──
                if (habits.isNotEmpty()) {
                    InsightSectionHeader("Habit Consistency & Strength", Icons.Default.SelfImprovement)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Habits Breakdown By Type
                            val buildCount = habits.count { h -> h.type == "BUILD" }
                            val quitCount = habits.count { h -> h.type == "QUIT" }
                            val numericCount = habits.count { h -> h.type == "NUMERIC" }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TypeBadge(label = "Build", count = buildCount, color = SecondaryEmerald)
                                TypeBadge(label = "Quit", count = quitCount, color = PriorityHigh)
                                TypeBadge(label = "Target", count = numericCount, color = PrimaryIndigo)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Habit Strength Breakdown (Rolling 30-Day Consistency)
                            Text(
                                text = "Habit Strength (30-Day Consistency)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            habits.forEach { habit ->
                                val stat = habitStats[habit.id]
                                val strength = stat?.habitStrength ?: 0
                                val streak = stat?.currentStreak ?: 0
                                val strengthColor = when {
                                    strength >= 70 -> SecondaryEmerald
                                    strength >= 40 -> Color(0xFFD97706)
                                    else -> PriorityHigh
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (streak > 0) {
                                                Text(
                                                    text = "${streak}d streak",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                color = strengthColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "$strength% Strength",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = strengthColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { strength / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = strengthColor,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Day of Week Distribution for Habits
                            Text(
                                text = "Habit Completion Frequency by Day",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            val dayCounts = remember(allHabitLogs) {
                                val map = mutableMapOf<String, Int>()
                                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { map[it] = 0 }
                                allHabitLogs.filter { log -> log.isCompleted }.forEach { log ->
                                    try {
                                        val dayName = LocalDate.parse(log.date, DateTimeFormatter.ISO_DATE).dayOfWeek.name.take(3)
                                        map[dayName] = (map[dayName] ?: 0) + 1
                                    } catch (e: Exception) { }
                                }
                                map
                            }

                            val maxDayCount = maxOf(dayCounts.values.maxOrNull() ?: 1, 1)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                dayCounts.forEach { (day, count) ->
                                    val barRatio = count.toFloat() / maxDayCount
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("$count", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .fillMaxHeight(barRatio.coerceIn(0.1f, 1f))
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SecondaryEmerald)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(day, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Top Habit Streak Highlight
                            val topHabitPair = habitStats.entries.maxByOrNull { entry -> entry.value.currentStreak }
                            val topHabit = habits.find { h -> h.id == topHabitPair?.key }
                            if (topHabit != null && topHabitPair != null && topHabitPair.value.currentStreak > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Top Active Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(topHabit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = "${topHabitPair.value.currentStreak} Days",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Journal & Mood Analytics Section ──
                InsightSectionHeader("Journal & Mood Insights", Icons.Default.Book)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (journalStats == null || journalStats.totalEntriesThisMonth == 0) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No journal reflections for this month yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = onNavigateToJournal,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Journal")
                                    }
                                }
                            }
                        } else {
                            // Journal Highlights Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val moodRatingText = if (journalStats.avgMoodScore > 0f) {
                                    String.format(java.util.Locale.US, "%.1f", journalStats.avgMoodScore)
                                } else "-"
                                InsightHighlightCard(
                                    modifier = Modifier.weight(1f),
                                    label = "😊 Avg Mood",
                                    value = moodRatingText,
                                    subtitle = "${journalStats.dominantMoodEmoji} ${journalStats.dominantMood}",
                                    iconColor = Color(0xFF10B981),
                                    bgColor = Color(0xFF10B981).copy(alpha = 0.1f)
                                )
                                InsightHighlightCard(
                                    modifier = Modifier.weight(1f),
                                    label = "🔥 Streak",
                                    value = "${journalStats.currentStreak}d",
                                    subtitle = "consecutive",
                                    iconColor = Color(0xFFF59E0B),
                                    bgColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
                                )
                                InsightHighlightCard(
                                    modifier = Modifier.weight(1f),
                                    label = "✍️ Words",
                                    value = "${journalStats.totalWordsThisMonth}",
                                    subtitle = "~${journalStats.avgWordsPerEntry} w/entry",
                                    iconColor = PrimaryIndigo,
                                    bgColor = PrimaryIndigo.copy(alpha = 0.1f)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Mood Distribution Breakdown
                            Text(
                                text = "Mood Distribution (${journalStats.totalEntriesThisMonth} Reflections)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            val moodItems = listOf(
                                Triple("Great", "🤩", SecondaryEmerald),
                                Triple("Good", "😊", PrimaryIndigo),
                                Triple("Neutral", "😐", Color(0xFFF59E0B)),
                                Triple("Bad", "😔", Color(0xFFF97316)),
                                Triple("Awful", "😫", PriorityHigh)
                            )
                            val maxMoodCount = maxOf(journalStats.moodDistribution.values.maxOrNull() ?: 1, 1)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                moodItems.forEach { (moodName, emoji, color) ->
                                    val count = journalStats.moodDistribution[moodName] ?: 0
                                    val ratio = count.toFloat() / maxMoodCount
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("$emoji $moodName", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(90.dp))
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = color,
                                            trackColor = MaterialTheme.colorScheme.surface
                                        )
                                        Text(
                                            "$count",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(24.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            // Smart Mindfulness Correlation Tip
                            val mindfulnessTip = when {
                                data.completedTasksThisMonth >= 15 && journalStats.avgMoodScore >= 3.8f ->
                                    "🌟 Excellent balance! High task throughput paired with positive mood."
                                data.completedTasksThisMonth >= 15 && journalStats.avgMoodScore > 0f && journalStats.avgMoodScore < 3.0f ->
                                    "💡 Heavy workload detected with lower mood. Consider scheduling relaxation time."
                                journalStats.currentStreak >= 5 ->
                                    "🧘 Great streak! Regular journaling builds clarity and long-term self-awareness."
                                else ->
                                    "📝 Journaling consistently helps spot patterns between your daily workload and energy levels."
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                    Text(mindfulnessTip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
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

                // ── Focus Time & Workload Analytics ──
                InsightSectionHeader("Focus Time & Workload Analytics", Icons.Default.Timer)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Top Grid: 3 Hero Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = SecondaryEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text("Focus Tracked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatEstimatedMinutes(data.totalTrackedMinutes),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (data.totalTrackedMinutes > 0) SecondaryEmerald else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text("Planned Work", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatEstimatedMinutes(data.totalEstimatedMinutes),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryIndigo
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = PriorityMedium,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text("Daily Focus Avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatEstimatedMinutes(data.avgDailyFocusMinutes)}/day",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PriorityMedium
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )

                        // 1. Focus vs. Plan Ratio Comparison
                        val focusRatioPct = if (data.totalEstimatedMinutes > 0) {
                            ((data.totalTrackedMinutes.toFloat() / data.totalEstimatedMinutes.toFloat()) * 100).toInt()
                        } else 0
                        val progressFraction = (data.focusEfficiencyRatio).coerceIn(0f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Focus vs. Planned Ratio",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (data.totalEstimatedMinutes > 0) "$focusRatioPct% of estimated" else "No planned workload set",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (focusRatioPct >= 100) SecondaryEmerald else MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { if (data.totalEstimatedMinutes > 0) progressFraction else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (focusRatioPct > 100) PriorityMedium else SecondaryEmerald,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }

                        // 2. Secondary stats row: Completed Tasks breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Focus on Done Tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatEstimatedMinutes(data.completedTrackedMinutes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryEmerald
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Planned on Done Tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatEstimatedMinutes(data.completedEstimatedMinutes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo
                                    )
                                }
                            }
                        }

                        // 3. Category Focus Distribution
                        if (data.focusByCategory.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Focus Duration by Category",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                data.focusByCategory.entries.take(5).forEachIndexed { index, (cat, trackedMin) ->
                                    val catFraction = if (data.totalTrackedMinutes > 0) trackedMin.toFloat() / data.totalTrackedMinutes else 0f
                                    val color = categoryColors[index % categoryColors.size]
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                                Text(cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            }
                                            Text(
                                                text = "${formatEstimatedMinutes(trackedMin)} (${(catFraction * 100).toInt()}%)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = color
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { catFraction },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            color = color,
                                            trackColor = MaterialTheme.colorScheme.surface
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Top Focused Tasks Deep-Dive
                        if (data.topFocusedTasks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Top Focused Tasks",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                data.topFocusedTasks.forEach { task ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = if (task.isCompleted) SecondaryEmerald else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = task.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    if (task.category.isNotBlank()) {
                                                        Text(
                                                            text = task.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = formatEstimatedMinutes(task.trackedMinutes),
                                                        style = androidx.compose.ui.text.TextStyle(
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                            fontFeatureSettings = "tnum",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                    if (task.estimatedMinutes > 0) {
                                                        Text(
                                                            text = "/ ${formatEstimatedMinutes(task.estimatedMinutes)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
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

                // ── Projects & Milestone Breakdown ──
                if (projects.isNotEmpty() || data.projectDistribution.isNotEmpty()) {
                    InsightSectionHeader("Projects & Milestone Progress", Icons.Default.Folder)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (projects.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("No active projects assigned this month", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                projects.forEach { project ->
                                    val (totalTasks, completedTasks) = data.projectDistribution[project.id] ?: Pair(0, 0)
                                    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
                                    val projColor = runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }.getOrDefault(PrimaryIndigo)

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(projColor))
                                                Text(project.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            }
                                            Text("$completedTasks/$totalTasks Done", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = projColor,
                                            trackColor = MaterialTheme.colorScheme.surface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Achievements & Gamification Progress ──
                achievementsUiState?.let { achState ->
                    InsightSectionHeader("Achievements & Badges", Icons.Default.EmojiEvents)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Badges Unlocked",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${achState.summary.unlockedCount} of ${achState.summary.totalCount} completed (${(achState.summary.overallProgress * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    color = PrimaryIndigo.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🏆 ${achState.summary.totalXpEarned} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { achState.summary.overallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = SecondaryEmerald,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            // Category breakdown chips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val habitsBuild = achState.allAchievements.filter { it.definition.category == AchievementCategory.BuildHabits }
                                val habitsQuit = achState.allAchievements.filter { it.definition.category == AchievementCategory.QuitHabits }
                                val tasksAch = achState.allAchievements.filter { it.definition.category == AchievementCategory.Tasks }

                                val buildUnlocked = habitsBuild.count { it.unlocked }
                                val quitUnlocked = habitsQuit.count { it.unlocked }
                                val tasksUnlocked = tasksAch.count { it.unlocked }

                                InsightHighlightCard(
                                    modifier = Modifier.weight(1f),
                                    label = "🌱 Build",
                                    value = "$buildUnlocked/${habitsBuild.size}",
                                    subtitle = "habits",
                                    iconColor = SecondaryEmerald,
                                    bgColor = SecondaryEmerald.copy(alpha = 0.1f)
                                )

                                InsightHighlightCard(
                                    modifier = Modifier.weight(1f),
                                    label = "🛑 Quit",
                                    value = "$quitUnlocked/${habitsQuit.size}",
                                    subtitle = "unquittable",
                                    iconColor = PriorityHigh,
                                    bgColor = PriorityHigh.copy(alpha = 0.1f)
                                )

                                InsightHighlightCard(
                                    modifier = Modifier.weight(1f),
                                    label = "📋 Tasks",
                                    value = "$tasksUnlocked/${tasksAch.size}",
                                    subtitle = "mastery",
                                    iconColor = PrimaryIndigo,
                                    bgColor = PrimaryIndigo.copy(alpha = 0.1f)
                                )
                            }

                            // Next Milestone Hint
                            achState.nextAchievement?.let { next ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(next.definition.iconEmoji, fontSize = 20.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Next: ${next.definition.title}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${next.current}/${next.definition.target} (${next.remaining} remaining)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onNavigateToAchievements,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View All Badges & Milestones")
                            }
                        }
                    }
                }

                // ── Motivational summary ──
                val motivationMsg = when {
                    data.completionRateThisMonth >= 90 -> "Outstanding! You're crushing it this month!"
                    data.completionRateThisMonth >= 70 -> "Great momentum! Keep pushing forward."
                    data.completionRateThisMonth >= 50 -> "Good progress. Half-way there!"
                    data.completionRateThisMonth > 0 -> "Building momentum. Every task counts."
                    else -> "Start tracking tasks to see your insights here."
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

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Export Summary Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose your preferred export format to share or save your monthly productivity summary:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val report = ReportExporter.generateMarkdownReport(
                            month = currentMonth,
                            insightsData = insightsData,
                            tasks = allTasks,
                            habits = habits,
                            habitStats = habitStats,
                            projects = projects,
                            journalEntries = allJournalEntries,
                            journalStats = journalStats,
                            achievementsUiState = achievementsUiState
                        )
                        ReportExporter.shareReport(
                            context = context,
                            content = report,
                            title = "TaskSpace Report - ${currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))}",
                            mimeType = "text/markdown"
                        )
                        showExportDialog = false
                    }
                ) {
                    Text("Markdown (.md)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val csv = ReportExporter.generateCsvReport(
                            tasks = allTasks,
                            habits = habits,
                            habitStats = habitStats,
                            journalEntries = allJournalEntries,
                            achievements = achievementsUiState?.allAchievements ?: emptyList()
                        )
                        ReportExporter.shareReport(
                            context = context,
                            content = csv,
                            title = "TaskSpace Data - ${currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))}",
                            mimeType = "text/csv"
                        )
                        showExportDialog = false
                    }
                ) {
                    Text("CSV (.csv)")
                }
            }
        )
    }
}

@Composable
private fun TypeBadge(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
            Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = color)
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

private fun formatEstimatedMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
