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

    data class HabitStats(
        val currentStreak: Int,
        val maxStreak: Int,
        val createdAt: Long
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val habitStats: StateFlow<Map<String, HabitStats>> = habits
        .flatMapLatest { habitList ->
            if (habitList.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val statsFlows = habitList.map { habit ->
                    habitRepository.getAllLogsForHabit(habit.id)
                        .map { logs ->
                            val completedDates = logs.filter { it.isCompleted }
                                .map { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }
                                .sortedDescending()
                                .distinct()

                            var currentStreak = 0
                            var maxStreak = 0
                            var tempStreak = 0

                            val ascendingDates = completedDates.reversed()
                            if (ascendingDates.isNotEmpty()) {
                                tempStreak = 1
                                maxStreak = 1
                                for (i in 1 until ascendingDates.size) {
                                    if (ascendingDates[i - 1].plusDays(1) == ascendingDates[i]) {
                                        tempStreak++
                                        maxStreak = maxOf(maxStreak, tempStreak)
                                    } else {
                                        tempStreak = 1
                                    }
                                }
                            }

                            val today = LocalDate.now()
                            val yesterday = today.minusDays(1)
                            if (completedDates.contains(today) || completedDates.contains(yesterday)) {
                                currentStreak = 1
                                var expectedPrevDate = if (completedDates.contains(today)) today.minusDays(1) else yesterday.minusDays(1)
                                
                                val startIndex = if (completedDates.contains(today)) 1 else completedDates.indexOf(yesterday) + 1

                                for (i in startIndex until completedDates.size) {
                                    if (completedDates[i] == expectedPrevDate) {
                                        currentStreak++
                                        expectedPrevDate = expectedPrevDate.minusDays(1)
                                    } else {
                                        break
                                    }
                                }
                            }

                            habit.id to HabitStats(currentStreak, maxStreak, habit.createdAt)
                        }
                }
                combine(statsFlows) { pairs -> pairs.toMap() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
