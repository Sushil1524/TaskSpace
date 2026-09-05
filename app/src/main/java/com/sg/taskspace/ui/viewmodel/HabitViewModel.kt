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

import com.sg.taskspace.data.UserPreferences
import com.sg.taskspace.data.UserPreferencesRepository

class HabitViewModel(
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val habits: StateFlow<List<Habit>> = habitRepository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<HabitLog>> = habitRepository.allHabitLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences("User", false))

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
    val habitStats: StateFlow<Map<String, HabitStats>> = combine(
        habits,
        userPreferencesRepository.userPreferencesFlow
    ) { habitList: List<Habit>, prefs: UserPreferences ->
        habitList to prefs
    }
        .flatMapLatest { pair: Pair<List<Habit>, UserPreferences> ->
            val habitList = pair.first
            val prefs = pair.second
            if (habitList.isEmpty()) {
                flowOf(emptyMap<String, HabitStats>())
            } else {
                val statsFlows: List<Flow<Pair<String, HabitStats>>> = habitList.map { habit ->
                    habitRepository.getAllLogsForHabit(habit.id)
                        .map { logs ->
                            calculateStatsForHabit(habit, logs, prefs)
                        }
                }
                combine(statsFlows) { pairs -> pairs.toMap() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun calculateStatsForHabit(habit: Habit, logs: List<HabitLog>, prefs: UserPreferences): Pair<String, HabitStats> {
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
        val completedDateSet = logs.filter { log ->
            if (habit.type == "NUMERIC") {
                log.value >= habit.targetValue
            } else {
                log.isCompleted
            }
        }
            .map { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }
            .toSet()

        var currentStreak = 0
        var checkDate = today
        if (!completedDateSet.contains(today) && !(prefs.freezeHabitStreaks && prefs.isDateInVacation(today))) {
            checkDate = today.minusDays(1)
        }

        while (true) {
            if (completedDateSet.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else if (prefs.freezeHabitStreaks && prefs.isDateInVacation(checkDate)) {
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
            if (checkDate.isBefore(createdDate.minusDays(365))) break
        }

        var maxStreak = currentStreak
        var tempStreak = 0
        var iterDate = createdDate
        while (!iterDate.isAfter(today)) {
            if (completedDateSet.contains(iterDate)) {
                tempStreak++
                maxStreak = maxOf(maxStreak, tempStreak)
            } else if (prefs.freezeHabitStreaks && prefs.isDateInVacation(iterDate)) {
                // frozen vacation day does not break the streak
            } else {
                tempStreak = 0
            }
            iterDate = iterDate.plusDays(1)
        }

        return habit.id to HabitStats(
            currentStreak = currentStreak,
            maxStreak = maxStreak,
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
        val existingLog = logs.find { it.habitId == habit.id && it.date == dateString }

        viewModelScope.launch {
            if (existingLog != null) {
                val newStatus = !existingLog.isCompleted
                habitRepository.insertHabitLog(
                    existingLog.copy(
                        isCompleted = newStatus,
                        value = if (newStatus) habit.targetValue else 0f
                    )
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
        val existingLog = logs.find { it.habitId == habit.id && it.date == dateString }
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
        val existingLog = logs.find { it.habitId == habit.id && it.date == dateString }

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

    fun setVacationMode(
        enabled: Boolean,
        startDate: String = "",
        endDate: String = "",
        reason: String = "Vacation",
        freezeHabits: Boolean = true,
        pauseReminders: Boolean = true
    ) {
        viewModelScope.launch {
            userPreferencesRepository.updateVacationMode(
                enabled = enabled,
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                freezeHabits = freezeHabits,
                pauseReminders = pauseReminders
            )
        }
    }

    fun clearVacationMode() {
        viewModelScope.launch {
            userPreferencesRepository.clearVacationMode()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                val userPrefsRepo = UserPreferencesRepository(application)
                return HabitViewModel(HabitRepository(database.habitDao()), userPrefsRepo) as T
            }
        }
    }
}
