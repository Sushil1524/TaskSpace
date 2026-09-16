package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sg.taskspace.data.AppDatabase
import com.sg.taskspace.data.JournalEntry
import com.sg.taskspace.data.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JournalViewModel(private val journalRepository: JournalRepository) : ViewModel() {
    
    val allEntries: StateFlow<List<JournalEntry>> = journalRepository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entryForSelectedDate: StateFlow<JournalEntry?> = _selectedDate
        .flatMapLatest { date ->
            journalRepository.getJournalEntryForDate(date.format(DateTimeFormatter.ISO_DATE))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun saveEntry(date: LocalDate, content: String, mood: String) {
        val dateString = date.format(DateTimeFormatter.ISO_DATE)
        viewModelScope.launch {
            val existing = journalRepository.getJournalEntryForDate(dateString).first()
            if (existing != null) {
                journalRepository.insertJournalEntry(existing.copy(content = content, mood = mood))
            } else {
                journalRepository.insertJournalEntry(JournalEntry(date = dateString, content = content, mood = mood))
            }
        }
    }
    
    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.softDeleteJournalEntry(entry.id)
        }
    }

    data class JournalStats(
        val totalEntriesThisMonth: Int = 0,
        val totalWordsThisMonth: Int = 0,
        val avgWordsPerEntry: Int = 0,
        val currentStreak: Int = 0,
        val avgMoodScore: Float = 0f,
        val dominantMood: String = "None",
        val dominantMoodEmoji: String = "📝",
        val moodDistribution: Map<String, Int> = emptyMap(),
        val hasJournaledToday: Boolean = false,
        val todayEntry: JournalEntry? = null
    )

    fun computeJournalStats(month: java.time.YearMonth, entries: List<JournalEntry>): JournalStats {
        val activeEntries = entries.filter { !it.isDeleted }
        val monthPrefix = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val monthEntries = activeEntries.filter { it.date.startsWith(monthPrefix) }

        val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val todayEntry = activeEntries.find { it.date == todayIso }
        val hasJournaledToday = todayEntry != null && todayEntry.content.isNotBlank()

        // Word counts
        val totalWords = monthEntries.sumOf { entry ->
            if (entry.content.isBlank()) 0 else entry.content.trim().split("\\s+".toRegex()).size
        }
        val avgWords = if (monthEntries.isNotEmpty()) totalWords / monthEntries.size else 0

        // Mood scoring
        val standardMoods = listOf("Great", "Good", "Neutral", "Bad", "Awful")
        val moodDistribution = standardMoods.associateWith { moodName ->
            monthEntries.count { it.mood.equals(moodName, ignoreCase = true) }
        }.toMutableMap()

        // Handle any custom or legacy mood strings
        monthEntries.forEach { entry ->
            val matchingKey = standardMoods.find { it.equals(entry.mood, ignoreCase = true) }
            if (matchingKey == null && entry.mood.isNotBlank()) {
                moodDistribution[entry.mood] = (moodDistribution[entry.mood] ?: 0) + 1
            }
        }

        val totalMoodScore = monthEntries.sumOf { entry ->
            when (entry.mood.lowercase()) {
                "great" -> 5.0
                "good", "happy" -> 4.0
                "neutral", "calm" -> 3.0
                "bad", "sad" -> 2.0
                "awful", "anxious", "angry" -> 1.0
                else -> 3.0
            }
        }
        val avgMoodScore = if (monthEntries.isNotEmpty()) (totalMoodScore / monthEntries.size).toFloat() else 0f

        val dominantMoodEntry = moodDistribution.filter { it.value > 0 }.maxByOrNull { it.value }
        val dominantMood = dominantMoodEntry?.key ?: "None"
        val dominantMoodEmoji = when (dominantMood.lowercase()) {
            "great" -> "🤩"
            "good", "happy" -> "😊"
            "neutral", "calm" -> "😐"
            "bad", "sad" -> "😔"
            "awful", "anxious", "angry" -> "😫"
            else -> "📝"
        }

        // Streak calculation across all active entries
        val datesWithEntries = activeEntries
            .mapNotNull {
                try {
                    LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE)
                } catch (e: Exception) {
                    null
                }
            }
            .toSet()

        val today = LocalDate.now()
        var streak = 0
        var checkDate = if (datesWithEntries.contains(today)) today else today.minusDays(1)
        while (datesWithEntries.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return JournalStats(
            totalEntriesThisMonth = monthEntries.size,
            totalWordsThisMonth = totalWords,
            avgWordsPerEntry = avgWords,
            currentStreak = streak,
            avgMoodScore = avgMoodScore,
            dominantMood = dominantMood,
            dominantMoodEmoji = dominantMoodEmoji,
            moodDistribution = moodDistribution,
            hasJournaledToday = hasJournaledToday,
            todayEntry = todayEntry
        )
    }

    fun restoreEntry(entryId: String) {
        viewModelScope.launch {
            journalRepository.restoreJournalEntry(entryId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                return JournalViewModel(JournalRepository(database.journalDao())) as T
            }
        }
    }
}
