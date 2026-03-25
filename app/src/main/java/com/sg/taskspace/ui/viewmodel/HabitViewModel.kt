package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sg.taskspace.data.AppDatabase
import com.sg.taskspace.data.Habit
import com.sg.taskspace.data.HabitLog
import com.sg.taskspace.data.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val habits: StateFlow<List<Habit>> = habitRepository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val logsForSelectedDate: StateFlow<List<HabitLog>> = _selectedDate
        .flatMapLatest { date ->
            habitRepository.getHabitLogsForDate(date.format(DateTimeFormatter.ISO_DATE))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addHabit(name: String, frequency: String) {
        viewModelScope.launch {
            habitRepository.insertHabit(Habit(name = name, frequency = frequency))
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    fun toggleHabitCompletion(habit: Habit, date: LocalDate, logs: List<HabitLog>) {
        val dateString = date.format(DateTimeFormatter.ISO_DATE)
        val existingLog = logs.find { it.habitId == habit.id }

        viewModelScope.launch {
            if (existingLog != null) {
                habitRepository.insertHabitLog(existingLog.copy(isCompleted = !existingLog.isCompleted))
            } else {
                habitRepository.insertHabitLog(HabitLog(habitId = habit.id, date = dateString, isCompleted = true))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                return HabitViewModel(HabitRepository(database.habitDao())) as T
            }
        }
    }
}
