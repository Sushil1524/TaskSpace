package com.sg.taskspace.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sg.taskspace.data.JournalEntry
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

    val today = LocalDate.now()
    val moods = listOf("Great", "Good", "Neutral", "Bad", "Awful")
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d yyyy") }
    val recentEntries = allEntries.filter { journalEntry ->
        runCatching { LocalDate.parse(journalEntry.date) <= today }.getOrDefault(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Journal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveEntry(selectedDate, content, mood) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            item {
                Text(
                    text = if (entry == null) {
                        "No entry saved for this day yet. You can still reflect and save one now."
                    } else {
                        "Revisit your thoughts for this day and update them if you want."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    moods.forEachIndexed { index, currentMood ->
                        SegmentedButton(
                            selected = mood == currentMood,
                            onClick = { mood = currentMood },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = moods.size)
                        ) {
                            Text(currentMood, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    label = { Text("Write your thoughts here...") },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Button(
                    onClick = { viewModel.saveEntry(selectedDate, content, mood) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Entry")
                }
            }

            item {
                Text(
                    text = "Recent Reflections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (recentEntries.isEmpty()) {
                item {
                    Text(
                        text = "No journal history yet. Your saved entries will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentEntries, key = { it.id }) { journalEntry ->
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onToday,
                enabled = selectedDate != today,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Jump To Today")
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
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
                Text(
                    text = entry.mood,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

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
