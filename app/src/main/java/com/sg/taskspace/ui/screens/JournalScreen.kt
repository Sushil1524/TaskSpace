package com.sg.taskspace.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.data.JournalEntry
import com.sg.taskspace.ui.theme.PrimaryIndigo
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.ui.viewmodel.JournalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val entry by viewModel.entryForSelectedDate.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()

    var content by remember(entry) { mutableStateOf(entry?.content ?: "") }
    var mood by remember(entry) { mutableStateOf(entry?.mood ?: "Neutral") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf("All") }

    val today = LocalDate.now()
    val moods = listOf(
        "Great" to "🤩",
        "Good" to "😊",
        "Neutral" to "😐",
        "Bad" to "😔",
        "Awful" to "😫"
    )
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d yyyy") }

    // Word count calculation
    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    }

    val filteredEntries = allEntries.filter { journalEntry ->
        val matchesSearch = searchQuery.isBlank() ||
                journalEntry.content.contains(searchQuery, ignoreCase = true) ||
                journalEntry.date.contains(searchQuery, ignoreCase = true)
        val matchesMood = selectedMoodFilter == "All" || journalEntry.mood.equals(selectedMoodFilter, ignoreCase = true)
        matchesSearch && matchesMood
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Journal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveEntry(selectedDate, content, mood) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                DateNavigator(
                    selectedDate = selectedDate,
                    today = today,
                    dateFormatter = dateFormatter,
                    onPrevious = { viewModel.selectDate(selectedDate.minusDays(1)) },
                    onNext = {
                        if (selectedDate < today) {
                            viewModel.selectDate(selectedDate.plusDays(1))
                        }
                    },
                    onToday = { viewModel.selectDate(today) }
                )
            }

            // Mood Chips Picker
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "How are you feeling today?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moods.forEach { (currentMood, emoji) ->
                            val isSelected = mood == currentMood
                            FilterChip(
                                selected = isSelected,
                                onClick = { mood = currentMood },
                                label = { Text("$emoji $currentMood", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Text Input & Word Count Counter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        label = { Text("Write your thoughts & reflection...") },
                        placeholder = { Text("What made today memorable? What are you grateful for?") },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount ${if (wordCount == 1) "word" else "words"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.saveEntry(selectedDate, content, mood) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Entry")
                        }
                    }
                }
            }

            // Journal Search & Filter Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Reflections & Journal History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search past entries...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Mood Filter Row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filterOptions = listOf("All") + moods.map { it.first }
                        items(filterOptions.size) { i ->
                            val option = filterOptions[i]
                            FilterChip(
                                selected = selectedMoodFilter == option,
                                onClick = { selectedMoodFilter = option },
                                label = { Text(option) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            if (filteredEntries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedMoodFilter != "All") "No matching journal entries found." else "No journal history yet. Save your first reflection above!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.id }) { journalEntry ->
                    JournalHistoryCard(
                        entry = journalEntry,
                        isSelected = journalEntry.date == selectedDate.format(DateTimeFormatter.ISO_DATE),
                        onClick = {
                            viewModel.selectDate(LocalDate.parse(journalEntry.date, DateTimeFormatter.ISO_DATE))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateNavigator(
    selectedDate: LocalDate,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedDate == today) "Today" else "${today.toEpochDay() - selectedDate.toEpochDay()} day(s) ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onNext, enabled = selectedDate < today) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                }
            }

            if (selectedDate != today) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onToday,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Jump To Today")
                }
            }
        }
    }
}

@Composable
private fun JournalHistoryCard(
    entry: JournalEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formattedDate = remember(entry.date) {
        runCatching {
            LocalDate.parse(entry.date, DateTimeFormatter.ISO_DATE)
                .format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy"))
        }.getOrDefault(entry.date)
    }

    val moodEmoji = when (entry.mood) {
        "Great" -> "🤩"
        "Good" -> "😊"
        "Neutral" -> "😐"
        "Bad" -> "😔"
        "Awful" -> "😫"
        else -> "📝"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$moodEmoji ${entry.mood}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
