package com.sg.taskspace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.PrimaryIndigo
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.ui.viewmodel.AchievementCategory
import com.sg.taskspace.ui.viewmodel.AchievementProgress
import com.sg.taskspace.ui.viewmodel.AchievementsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()

    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    var selectedStatus by remember { mutableStateOf("All") } // "All", "Unlocked", "In Progress"
    var isGridView by remember { mutableStateOf(true) }
    var detailAchievement by remember { mutableStateOf<AchievementProgress?>(null) }

    // Combine all achievements and filter
    val allAchievements = remember(uiState) {
        uiState.unlockedAchievements + uiState.inProgressAchievements
    }

    val filteredAchievements = remember(allAchievements, selectedCategory, selectedStatus) {
        allAchievements.filter { achievement ->
            val matchesCategory = selectedCategory == null || achievement.definition.category == selectedCategory
            val matchesStatus = when (selectedStatus) {
                "Unlocked" -> achievement.unlocked
                "In Progress" -> !achievement.unlocked
                else -> true
            }
            matchesCategory && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements & Badges", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.Menu else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category & Status filter header (sticky at top)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Category Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${allAchievements.size})") },
                        leadingIcon = { Text("🌟", fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    AchievementCategory.values().forEach { category ->
                        val count = allAchievements.count { it.definition.category == category }
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text("${category.displayName} ($count)") },
                            leadingIcon = { Text(category.iconEmoji, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status Filter & Layout Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Unlocked", "In Progress").forEach { status ->
                            val count = when (status) {
                                "Unlocked" -> uiState.unlockedAchievements.size
                                "In Progress" -> uiState.inProgressAchievements.size
                                else -> allAchievements.size
                            }
                            val isSelected = selectedStatus == status
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { selectedStatus = status }
                            ) {
                                Text(
                                    text = "$status ($count)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isGridView) "Grid View" else "List View",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Area (Grid or List)
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Header spans full width
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Gamification Level Banner
                            userPreferences?.let { prefs ->
                                GamificationLevelBanner(
                                    level = prefs.level,
                                    totalXp = prefs.totalXp,
                                    progress = prefs.xpProgressFraction,
                                    totalXpEarnedFromAchievements = uiState.summary.totalXpEarned
                                )
                            }

                            // Summary Progress Card
                            AchievementSummaryCard(
                                summary = uiState.summary
                            )

                            // Next Achievement Spotlight
                            uiState.nextAchievement?.let { nextTarget ->
                                if (selectedStatus != "Unlocked") {
                                    NextMilestoneSpotlightCard(
                                        nextAchievement = nextTarget,
                                        onClick = { detailAchievement = nextTarget }
                                    )
                                }
                            }

                            Text(
                                text = "All Badges (${filteredAchievements.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (filteredAchievements.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            EmptyAchievementsCard()
                        }
                    } else {
                        items(filteredAchievements, key = { it.definition.id }) { achievement ->
                            AchievementSquareCard(
                                achievement = achievement,
                                onClick = { detailAchievement = achievement }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            userPreferences?.let { prefs ->
                                GamificationLevelBanner(
                                    level = prefs.level,
                                    totalXp = prefs.totalXp,
                                    progress = prefs.xpProgressFraction,
                                    totalXpEarnedFromAchievements = uiState.summary.totalXpEarned
                                )
                            }

                            AchievementSummaryCard(
                                summary = uiState.summary
                            )

                            uiState.nextAchievement?.let { nextTarget ->
                                if (selectedStatus != "Unlocked") {
                                    NextMilestoneSpotlightCard(
                                        nextAchievement = nextTarget,
                                        onClick = { detailAchievement = nextTarget }
                                    )
                                }
                            }

                            Text(
                                text = "All Badges (${filteredAchievements.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (filteredAchievements.isEmpty()) {
                        item {
                            EmptyAchievementsCard()
                        }
                    } else {
                        items(filteredAchievements, key = { it.definition.id }) { achievement ->
                            AchievementListCard(
                                achievement = achievement,
                                onClick = { detailAchievement = achievement }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    detailAchievement?.let { achievement ->
        AchievementDetailDialog(
            achievement = achievement,
            onDismiss = { detailAchievement = null }
        )
    }
}

// ── Level & XP Banner ──
@Composable
private fun GamificationLevelBanner(
    level: Int,
    totalXp: Int,
    progress: Float,
    totalXpEarnedFromAchievements: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "⭐ Level $level",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Column {
                        Text("$totalXp Total XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("+$totalXpEarnedFromAchievements XP from Badges", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = "${(progress * 100).toInt()}% to Lvl ${level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

// ── Progression Summary Card ──
@Composable
private fun AchievementSummaryCard(
    summary: com.sg.taskspace.ui.viewmodel.ProgressionSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Mastery Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${summary.unlockedCount} of ${summary.totalCount} Badges Unlocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (summary.currentStreak > 0) {
                    Text(
                        text = "🔥 ${summary.currentStreak}-day active task streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE86A33),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Circular progress donut
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { summary.overallProgress },
                    modifier = Modifier.size(60.dp),
                    strokeWidth = 6.dp,
                    color = SecondaryEmerald,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = "${(summary.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Next Milestone Spotlight Card ──
@Composable
private fun NextMilestoneSpotlightCard(
    nextAchievement: AchievementProgress,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(nextAchievement.definition.iconEmoji, fontSize = 18.sp)
                    Text(
                        text = "Next Target: ${nextAchievement.definition.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "+${nextAchievement.definition.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = nextAchievement.definition.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${nextAchievement.current} / ${nextAchievement.definition.target} (${nextAchievement.remaining} left)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap for details →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { nextAchievement.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// ── Square Achievement Card (Grid) ──
@Composable
private fun AchievementSquareCard(
    achievement: AchievementProgress,
    onClick: () -> Unit
) {
    val category = achievement.definition.category
    val accentColor = getCategoryColor(category)
    val isUnlocked = achievement.unlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isUnlocked) Modifier.border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Icon + XP / Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isUnlocked) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = achievement.definition.iconEmoji,
                            fontSize = 20.sp
                        )
                    }
                }

                Surface(
                    color = if (isUnlocked) SecondaryEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isUnlocked) "✓ Done" else "+${achievement.definition.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Title & Category tag
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = achievement.definition.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontSize = 10.sp
                )
            }

            // Description (Readable with 2-3 lines max)
            Text(
                text = achievement.definition.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            // Progress bar & label
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUnlocked) "Completed" else "${achievement.current}/${achievement.definition.target}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUnlocked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isUnlocked) {
                        Text(
                            text = "${(achievement.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (isUnlocked) 1f else achievement.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// ── Detailed List Achievement Card ──
@Composable
private fun AchievementListCard(
    achievement: AchievementProgress,
    onClick: () -> Unit
) {
    val category = achievement.definition.category
    val accentColor = getCategoryColor(category)
    val isUnlocked = achievement.unlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isUnlocked) Modifier.border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) accentColor.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (isUnlocked) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = achievement.definition.iconEmoji,
                        fontSize = 22.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.definition.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = if (isUnlocked) SecondaryEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isUnlocked) "✓ Unlocked" else "+${achievement.definition.xpReward} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = achievement.definition.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${category.displayName} • ${if (isUnlocked) "Completed" else "${achievement.current}/${achievement.definition.target}"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!isUnlocked) {
                        Text(
                            text = "${achievement.remaining} remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (isUnlocked) 1f else achievement.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// ── Interactive Detail Dialog ──
@Composable
private fun AchievementDetailDialog(
    achievement: AchievementProgress,
    onDismiss: () -> Unit
) {
    val category = achievement.definition.category
    val accentColor = getCategoryColor(category)
    val isUnlocked = achievement.unlocked

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Badge Avatar
                Surface(
                    color = if (isUnlocked) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
                        .then(
                            if (isUnlocked) Modifier.border(2.dp, accentColor, CircleShape) else Modifier
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = achievement.definition.iconEmoji,
                            fontSize = 36.sp
                        )
                    }
                }

                // Title & Category
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = achievement.definition.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+${achievement.definition.xpReward} XP Reward",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = achievement.definition.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Progress Info Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUnlocked) "Status: Unlocked!" else "Status: In Progress",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) SecondaryEmerald else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${achievement.current} / ${achievement.definition.target}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { if (isUnlocked) 1f else achievement.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        if (!isUnlocked) {
                            Text(
                                text = "${achievement.remaining} more needed to unlock this badge",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun EmptyAchievementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔍", fontSize = 28.sp)
            Text(
                text = "No badges match this filter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Try choosing another category or switching to 'All'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun getCategoryColor(category: AchievementCategory): Color {
    return when (category) {
        AchievementCategory.Tasks -> PrimaryIndigo
        AchievementCategory.Streaks -> Color(0xFFE86A33)
        AchievementCategory.BuildHabits -> SecondaryEmerald
        AchievementCategory.QuitHabits -> PriorityHigh
        AchievementCategory.Goals -> Color(0xFF0284C7)
        AchievementCategory.Mastery -> Color(0xFF9333EA)
    }
}
