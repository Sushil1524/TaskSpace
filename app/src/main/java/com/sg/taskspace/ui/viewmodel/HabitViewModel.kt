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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HabitViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val habits: StateFlow<List<Habit>> = habitRepository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<HabitLog>> = habitRepository.allHabitLogs
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
        val createdAt: Long,
        val daysClean: Int = 0
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
                            calculateStatsForHabit(habit, logs)
                        }
                }
                combine(statsFlows) { pairs -> pairs.toMap() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun calculateStatsForHabit(habit: Habit, logs: List<HabitLog>): Pair<String, HabitStats> {
        val today = LocalDate.now()
        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        if (habit.type == "QUIT") {
            val relapseLogs = logs.filter { it.relapseNotes != null || !it.isCompleted }
                .map { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }
                .sortedDescending()

            val lastRelapse = relapseLogs.firstOrNull()
            val daysClean = if (lastRelapse != null) {
                ChronoUnit.DAYS.between(lastRelapse, today).toInt().coerceAtLeast(0)
            } else {
                ChronoUnit.DAYS.between(createdDate, today).toInt().coerceAtLeast(0)
            }

            return habit.id to HabitStats(
                currentStreak = daysClean,
                maxStreak = daysClean,
                createdAt = habit.createdAt,
                daysClean = daysClean
            )
        }

        // BUILD or NUMERIC
        val completedDates = logs.filter { log ->
            if (habit.type == "NUMERIC") {
                log.value >= habit.targetValue
            } else {
                log.isCompleted
            }
        }
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

        return habit.id to HabitStats(
            currentStreak = currentStreak,
            maxStreak = maxOf(maxStreak, currentStreak),
            createdAt = habit.createdAt
        )
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.insertHabit(habit)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.insertHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.softDeleteHabit(habit.id)
        }
    }

    fun restoreHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.restoreHabit(habitId)
        }
    }

    fun toggleHabitCompletion(habit: Habit, date: LocalDate, logs: List<HabitLog>) {
        val dateString = date.format(DateTimeFormatter.ISO_DATE)
        val existingLog = logs.find { it.habitId == habit.id }

        viewModelScope.launch {
            if (existingLog != null) {
                val newStatus = !existingLog.isCompleted
                val newValue = if (newStatus) habit.targetValue else 0f
                habitRepository.insertHabitLog(
                    existingLog.copy(isCompleted = newStatus, value = newValue)
                )
            } else {
                habitRepository.insertHabitLog(
                    HabitLog(
                        habitId = habit.id,
                        date = dateString,
                        isCompleted = true,
                        value = habit.targetValue
                    )
                )
            }
        }
    }

    fun updateNumericProgress(habit: Habit, date: LocalDate, delta: Float, logs: List<HabitLog>) {
        val dateString = date.format(DateTimeFormatter.ISO_DATE)
        val existingLog = logs.find { it.habitId == habit.id }
        val currentValue = existingLog?.value ?: 0f
        val newValue = (currentValue + delta).coerceAtLeast(0f)
        val isCompleted = newValue >= habit.targetValue

        viewModelScope.launch {
            if (existingLog != null) {
                habitRepository.insertHabitLog(
                    existingLog.copy(value = newValue, isCompleted = isCompleted)
                )
            } else {
                habitRepository.insertHabitLog(
                    HabitLog(
                        habitId = habit.id,
                        date = dateString,
                        value = newValue,
                        isCompleted = isCompleted
                    )
                )
            }
        }
    }

    fun logRelapse(habit: Habit, date: LocalDate, relapseNotes: String?, logs: List<HabitLog>) {
        val dateString = date.format(DateTimeFormatter.ISO_DATE)
        val existingLog = logs.find { it.habitId == habit.id }

        viewModelScope.launch {
            if (existingLog != null) {
                habitRepository.insertHabitLog(
                    existingLog.copy(isCompleted = false, value = 0f, relapseNotes = relapseNotes)
                )
            } else {
                habitRepository.insertHabitLog(
                    HabitLog(
                        habitId = habit.id,
                        date = dateString,
                        isCompleted = false,
                        value = 0f,
                        relapseNotes = relapseNotes
                    )
                )
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
