package com.sg.taskspace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sg.taskspace.data.JournalEntry
import com.sg.taskspace.ui.viewmodel.JournalViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val entry by viewModel.entryForSelectedDate.collectAsState()

    var content by remember(entry) { mutableStateOf(entry?.content ?: "") }
    var mood by remember(entry) { mutableStateOf(entry?.mood ?: "Neutral") }
    
    val moods = listOf("Great", "Good", "Neutral", "Bad", "Awful")

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "How was your day? (${selectedDate})", 
                style = MaterialTheme.typography.titleMedium
            )

            // Mood Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                moods.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mood == m,
                        onClick = { mood = m },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = moods.size)
                    ) {
                        Text(m, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Write your thoughts here...") },
                textStyle = MaterialTheme.typography.bodyLarge
            )
            
            Button(
                onClick = { viewModel.saveEntry(selectedDate, content, mood) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Entry")
            }
        }
    }
}
