package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Task
import com.sg.taskspace.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: com.sg.taskspace.data.UserPreferencesRepository,
    private val dataTransferManager: com.sg.taskspace.data.DataTransferManager
) : ViewModel() {

    fun exportData(uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = dataTransferManager.exportData(uri)
            onResult(result.isSuccess)
        }
    }

    fun importData(uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = dataTransferManager.importData(uri)
            // If success, we might need to reload data or just let Flow update.
            // Flow should update automatically if Room observes changes.
            onResult(result.isSuccess)
        }
    }

    val userPreferences: StateFlow<com.sg.taskspace.data.UserPreferences?> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserName(name)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
        }
    }

    // Date & Time Logic
    private val currentDate = LocalDate.now()
    val formattedDate: String = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val weekNumber: String = "Week ${currentDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    // Tasks for Today
    private val todayIso = currentDate.format(DateTimeFormatter.ISO_DATE)
    private val dayOfWeek = currentDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }



    // Motivation Logic
    // Motivation Logic (Moved to UI)

    // Weekly Progress (Placeholder for now, or simple implementation)
    // We need a list of days (Mon-Sun) and their completion status
    // For Phase 2, let's just show the days and make them clickable (to filter? or just visual?)
    // User said: "shows mon thru sun and they are clickable and it shows total task and completed task with the list of that particular day"
    // This implies clicking a day changes the "Today's Task" list to that day.
    
    private val _selectedDate = MutableStateFlow(currentDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Update homeTasks to observe selectedDate
    // currentDisplayTasks moved to below to use new logic



    // Categories
    // Categories (Defined in UI/Bottom Sheet)

    data class DayStats(val date: LocalDate, val totalTasks: Int, val completedTasks: Int)

    // Reflection Text
    private val _reflectionText = MutableStateFlow("")
    val reflectionText: StateFlow<String> = _reflectionText.asStateFlow()

    fun updateReflectionText(text: String) {
        _reflectionText.value = text
    }

    // Helper to filter tasks for a specific day, handling repeating task exceptions
    private fun getTasksForDay(
        date: LocalDate,
        rangeTasks: List<Task>,
        repeatingTasks: List<Task>
    ): List<Task> {
        val dateIso = date.format(DateTimeFormatter.ISO_DATE)
        val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        // 1. Get all specific tasks for this day (including completed instances of repeating tasks)
        // CRITICAL FIX: Exclude repeating templates from here to avoid duplicates if they were created today.
        val specificTasks = rangeTasks.filter { it.createdForDate == dateIso && it.repeat == "None" }

        // 2. Get relevant repeating tasks
        val relevantRepeating = repeatingTasks.filter {
            (it.repeat == "Daily" || (it.repeat == "Weekly" && it.repeatDayOfWeek == dayName)) &&
            it.createdForDate <= dateIso // Only show if current date is on or after creation date
        }

        // 3. Filter out repeating tasks that have a completion instance (child) in specificTasks
        val effectiveRepeating = relevantRepeating.filter { repeating ->
            specificTasks.none { specific -> specific.parentId == repeating.id }
        }

        // 4. Combine
        return specificTasks + effectiveRepeating
    }

    // Weekly Stats Logic
    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyStats: StateFlow<List<DayStats>> = _selectedDate.flatMapLatest { date ->
        // Week starts on Sunday
        // Sunday = 7. 7 % 7 = 0. minusDays(0) -> Sunday.
        // Monday = 1. 1 % 7 = 1. minusDays(1) -> Sunday.
        val daysToSubtract = date.dayOfWeek.value % 7
        val startOfWeek = date.minusDays(daysToSubtract.toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        val startIso = startOfWeek.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfWeek.format(DateTimeFormatter.ISO_DATE)

        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDateRange(startIso, endIso),
            taskRepository.getRepeatingTasks()
        ) { rangeTasks, repeatingTasks ->
            val stats = mutableListOf<DayStats>()
            for (i in 0 until 7) {
                val currentDate = startOfWeek.plusDays(i.toLong())
                val dailyTasks = getTasksForDay(currentDate, rangeTasks, repeatingTasks)
                
                stats.add(DayStats(
                    date = currentDate,
                    totalTasks = dailyTasks.size,
                    completedTasks = dailyTasks.count { it.isCompleted }
                ))
            }
            stats
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Weekly Tasks Logic (Updated to include repeating tasks)
    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val daysToSubtract = date.dayOfWeek.value % 7
        val startOfWeek = date.minusDays(daysToSubtract.toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        val startIso = startOfWeek.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfWeek.format(DateTimeFormatter.ISO_DATE)
        
        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDateRange(startIso, endIso),
            taskRepository.getRepeatingTasks()
        ) { rangeTasks, repeatingTasks ->
             (rangeTasks + repeatingTasks).distinctBy { it.id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Update currentDisplayTasks to use the new logic
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDisplayTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val dateIso = date.format(DateTimeFormatter.ISO_DATE)
        
        // We need to fetch range tasks for just this day to be efficient, 
        // but our Repo only has getTasksForDateRange or getTasksForDate.
        // getTasksForDate in Dao includes repeating logic in SQL, which is now insufficient because of the parentId check.
        // So we should fetch "specific tasks" for today and "repeating tasks" and merge them in Kotlin.
        
        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDate(dateIso, ""), // We can use this but ignore its repeating logic? No, it returns mixed.
            // Let's use getTasksForDateRange for just today.
            taskRepository.getTasksForDateRange(dateIso, dateIso),
            taskRepository.getRepeatingTasks()
        ) { _, rangeTasks, repeatingTasks -> // Ignore the first one
            getTasksForDay(date, rangeTasks, repeatingTasks)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // History Stats Logic
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getWeekStats(startOfWeek: LocalDate): Flow<List<DayStats>> {
        val endOfWeek = startOfWeek.plusDays(6)
        val startIso = startOfWeek.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfWeek.format(DateTimeFormatter.ISO_DATE)

        return kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDateRange(startIso, endIso),
            taskRepository.getRepeatingTasks()
        ) { rangeTasks, repeatingTasks ->
            val stats = mutableListOf<DayStats>()
            for (i in 0 until 7) {
                val currentDate = startOfWeek.plusDays(i.toLong())
                val dailyTasks = getTasksForDay(currentDate, rangeTasks, repeatingTasks)
                
                stats.add(DayStats(
                    date = currentDate,
                    totalTasks = dailyTasks.size,
                    completedTasks = dailyTasks.count { it.isCompleted }
                ))
            }
            stats
        }
    }

    // Insights Logic
    data class InsightsData(
        val totalTasksThisMonth: Int,
        val completedTasksThisMonth: Int,
        val completionRateThisMonth: Int,
        val bestDayOfWeek: String,
        val currentStreak: Int,
        val categoryDistribution: Map<String, Pair<Int, Int>>, // Total, Completed
        val needsFocus: String
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getInsightsData(yearMonth: java.time.YearMonth): Flow<InsightsData> {
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()
        val startIso = startOfMonth.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfMonth.format(DateTimeFormatter.ISO_DATE)

        // For streak, we need to look back. Let's look back 365 days max for now.
        val streakStart = LocalDate.now().minusDays(365)
        val streakStartIso = streakStart.format(DateTimeFormatter.ISO_DATE)
        val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        return kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDateRange(startIso, endIso), // Month tasks
            taskRepository.getTasksForDateRange(streakStartIso, todayIso), // Streak candidates (approx)
            taskRepository.getRepeatingTasks()
        ) { monthTasks, streakCandidateTasks, repeatingTasks ->
            try {
                // 1. Monthly Stats
                // We need to expand repeating tasks for the month
                val allMonthTasks = mutableListOf<Task>()
                for (i in 0 until endOfMonth.dayOfMonth) {
                    val date = startOfMonth.plusDays(i.toLong())
                    allMonthTasks.addAll(getTasksForDay(date, monthTasks, repeatingTasks))
                }
                
                val totalMonth = allMonthTasks.size
                val completedMonth = allMonthTasks.count { it.isCompleted }
                val rate = if (totalMonth > 0) (completedMonth.toFloat() / totalMonth * 100).toInt() else 0
                
                // 2. Best Day of Week (based on completion count in this month)
                val bestDay = allMonthTasks.filter { it.isCompleted }
                    .groupBy { 
                        try {
                            LocalDate.parse(it.createdForDate, DateTimeFormatter.ISO_DATE).dayOfWeek 
                        } catch (_: Exception) {
                            null
                        }
                    }
                    .filterKeys { it != null }
                    .maxByOrNull { it.value.size }?.key?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "N/A"

                // 3. Category Distribution (This Month)
                val categories = allMonthTasks.groupBy { it.category }.mapValues { 
                    Pair(it.value.size, it.value.count { task -> task.isCompleted })
                }

                // 4. Current Streak
                // Check backwards from yesterday/today
                var streak = 0
                var checkDate = LocalDate.now()
                // If today has completed tasks, start counting from today. If not, check yesterday.
                // Actually, streak usually implies consecutive days *ending* today or yesterday.
                
                // Helper to check if a day has completed tasks
                fun hasCompletedTasks(date: LocalDate): Boolean {
                    val tasks = getTasksForDay(date, streakCandidateTasks, repeatingTasks)
                    return tasks.any { it.isCompleted }
                }

                if (hasCompletedTasks(checkDate)) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                } else {
                     // If today has no completed tasks yet, streak might still be alive from yesterday
                     checkDate = checkDate.minusDays(1)
                }

                while (hasCompletedTasks(checkDate)) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                    if (streak > 365) break // Safety break
                }

                // 5. Needs Focus
                // Category with lowest completion rate (min 1 task)
                val needsFocus = allMonthTasks.groupBy { it.category }
                    .filter { it.value.isNotEmpty() }
                    .minByOrNull { entry ->
                        val total = entry.value.size
                        val completed = entry.value.count { it.isCompleted }
                        if (total > 0) completed.toFloat() / total else 1f // If 0 tasks, ignore (shouldn't happen due to filter)
                    }?.key?.ifEmpty { "Uncategorized" } ?: "None"

                InsightsData(
                    totalTasksThisMonth = totalMonth,
                    completedTasksThisMonth = completedMonth,
                    completionRateThisMonth = rate,
                    bestDayOfWeek = bestDay,
                    currentStreak = streak,
                    categoryDistribution = categories,
                    needsFocus = needsFocus
                )
            } catch (e: Exception) {
                e.printStackTrace()
                InsightsData(0, 0, 0, "Error", 0, emptyMap(), "Error")
            }
        }
    }

    fun addTask(title: String, notes: String, priority: String, category: String, repeat: String = "None", date: LocalDate? = null) {
        viewModelScope.launch {
            val targetDate = date ?: _selectedDate.value
            val task = Task(
                title = title,
                notes = notes,
                priority = priority,
                category = category,
                createdForDate = targetDate.format(DateTimeFormatter.ISO_DATE),
                repeat = repeat
            )
            taskRepository.insertTask(task)
        }
    }
    
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (task.repeat != "None") {
                // It's a repeating task (Template). User wants to complete it for TODAY.
                // Create a new instance for today that is completed.
                val todayIso = _selectedDate.value.format(DateTimeFormatter.ISO_DATE)
                val completedInstance = task.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    createdForDate = todayIso,
                    isCompleted = true,
                    repeat = "None",
                    parentId = task.id
                )
                taskRepository.insertTask(completedInstance)
            } else if (task.parentId != null && task.isCompleted) {
                // It's a completed instance of a repeating task. User is un-checking it.
                // We should delete this instance so the original repeating task shows up again.
                taskRepository.deleteTask(task)
            } else {
                // Normal task or disconnected instance
                taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
            }
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun updateTaskDetails(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        // We need to add getTaskById to Repository and Dao first, or just filter from all tasks if list is small.
        // Better to add to Dao/Repo for correctness.
        // For now, let's assume we add it to Repo.
        return taskRepository.getTaskById(taskId)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaskSpaceApplication)
                TaskViewModel(
                    application.container.taskRepository,
                    application.container.userPreferencesRepository,
                    application.container.dataTransferManager
                )
            }
        }
    }
}
