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
            journalRepository.deleteJournalEntry(entry)
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
