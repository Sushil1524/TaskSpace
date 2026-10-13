package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Task
import com.sg.taskspace.data.TaskRepository
import com.sg.taskspace.util.AppLockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: com.sg.taskspace.data.UserPreferencesRepository,
    private val dataTransferManager: com.sg.taskspace.data.DataTransferManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            dataTransferManager.performAutoBackupIfNeeded()
        }
        checkAndApplyAppLockOnForeground()
    }

    fun exportData(uri: android.net.Uri, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = dataTransferManager.exportData(uri)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun importData(uri: android.net.Uri, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = dataTransferManager.importData(uri)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    /** Phase 1: Read, decrypt, and diff the backup — no writes. */
    fun previewImport(
        uri: android.net.Uri,
        onResult: (Result<com.sg.taskspace.data.BackupDiffResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = dataTransferManager.previewImport(uri)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    /** Phase 2: Commit the already-parsed backup to the database. */
    fun importDataFromParsed(
        backupData: com.sg.taskspace.data.BackupData,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = dataTransferManager.importDataFromParsed(backupData)
            withContext(Dispatchers.Main) {
                onResult(result.isSuccess)
            }
        }
    }

    /** Granular Phase 2: Commit backup with per-category & itemized selection. */
    fun importDataWithSelection(
        backupData: com.sg.taskspace.data.BackupData,
        selectionConfig: com.sg.taskspace.data.RestoreSelectionConfig,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = dataTransferManager.importDataWithSelection(backupData, selectionConfig)
            withContext(Dispatchers.Main) {
                onResult(result.isSuccess)
            }
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

    fun saveThemeMode(themeMode: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateThemeMode(themeMode)
        }
    }

    fun saveFontFamily(fontFamily: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateFontFamily(fontFamily)
        }
    }

    fun saveSecureBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateSecureBackupEnabled(enabled)
        }
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateNotificationsEnabled(enabled)
            if (enabled) {
                com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
                com.sg.taskspace.util.NotificationHelper.scheduleDailyEveningReminder(TaskSpaceApplication.instance)
            } else {
                com.sg.taskspace.util.NotificationHelper.cancelDailyDashboardNotification(TaskSpaceApplication.instance)
            }
        }
    }

    fun savePersistentDailyDashboardEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updatePersistentDailyDashboardEnabled(enabled)
            if (enabled) {
                com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
            } else {
                com.sg.taskspace.util.NotificationHelper.cancelDailyDashboardNotification(TaskSpaceApplication.instance)
            }
        }
    }

    fun saveSwipeRightAction(action: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateSwipeRightAction(action)
        }
    }

    fun saveSwipeLeftAction(action: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateSwipeLeftAction(action)
        }
    }

    // App Lock State
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()
    private var lastBackgroundTimeMs: Long = 0L

    fun checkAndApplyAppLockOnForeground() {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.fetchInitialPreferences()
            if (prefs.isAppLockEnabled && prefs.appLockPinHash.isNotBlank()) {
                val bgTime = if (lastBackgroundTimeMs > 0L) lastBackgroundTimeMs else prefs.lastBackgroundTimeMillis
                val isExpired = AppLockHelper.isTimeoutExceeded(
                    lastBackgroundMs = bgTime,
                    timeoutOption = prefs.autoLockTimeout
                )
                if (isExpired) {
                    _isAppLocked.value = true
                }
            }
        }
    }

    fun recordBackgroundTimestamp() {
        val now = System.currentTimeMillis()
        lastBackgroundTimeMs = now
        viewModelScope.launch {
            userPreferencesRepository.updateLastBackgroundTime(now)
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
        val now = System.currentTimeMillis()
        lastBackgroundTimeMs = now
        viewModelScope.launch {
            userPreferencesRepository.updateLastBackgroundTime(now)
        }
    }

    fun saveAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAppLockEnabled(enabled)
            if (!enabled) {
                _isAppLocked.value = false
            }
        }
    }

    fun saveAppLockPin(pin: String) {
        viewModelScope.launch {
            val hash = AppLockHelper.hashPin(pin)
            userPreferencesRepository.updateAppLockPinHash(hash)
            userPreferencesRepository.updateAppLockEnabled(true)
        }
    }

    fun saveBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateBiometricEnabled(enabled)
        }
    }

    fun saveAutoLockTimeout(timeout: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutoLockTimeout(timeout)
        }
    }

    fun snoozeTaskToTomorrow(task: Task) {
        viewModelScope.launch {
            val tomorrowIso = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE)
            taskRepository.updateTask(task.copy(createdForDate = tomorrowIso, isCompleted = false))
        }
    }

    // Overdue Tasks Logic
    val overdueTasks: StateFlow<List<Task>> = taskRepository.getAllTasks()
        .map { allTasks ->
            val currentTodayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            allTasks.filter { !it.isCompleted && !it.isDeleted && it.repeat == "None" && it.createdForDate < currentTodayIso }
                .sortedWith(
                    compareByDescending<Task> { it.createdForDate }
                        .thenBy {
                            when (it.priority.lowercase()) {
                                "high" -> 1
                                "medium" -> 2
                                "low" -> 3
                                else -> 4
                            }
                        }
                )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun moveTaskToToday(task: Task) {
        viewModelScope.launch {
            val currentTodayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val updated = task.copy(createdForDate = currentTodayIso, isCompleted = false)
            taskRepository.updateTask(updated)
            scheduleTaskReminderIfNeeded(updated)
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    fun moveAllOverdueTasksToToday(tasks: List<Task>) {
        viewModelScope.launch {
            val currentTodayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            tasks.forEach { task ->
                val updated = task.copy(createdForDate = currentTodayIso, isCompleted = false)
                taskRepository.updateTask(updated)
                scheduleTaskReminderIfNeeded(updated)
            }
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    fun restoreTaskDate(task: Task, originalDate: String) {
        viewModelScope.launch {
            val updated = task.copy(createdForDate = originalDate)
            taskRepository.updateTask(updated)
            scheduleTaskReminderIfNeeded(updated)
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    fun restoreTasksDates(entries: List<Pair<Task, String>>) {
        viewModelScope.launch {
            entries.forEach { (task, origDate) ->
                val updated = task.copy(createdForDate = origDate)
                taskRepository.updateTask(updated)
                scheduleTaskReminderIfNeeded(updated)
            }
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    // ── Focus Timer Integration ─────────────────────────────────────────────
    val focusTimerState: StateFlow<com.sg.taskspace.util.FocusTimerState> = com.sg.taskspace.util.FocusTimerManager.timerState
    val focusTimerElapsedSeconds: StateFlow<Long> = com.sg.taskspace.util.FocusTimerManager.elapsedSeconds

    fun startFocusTimer(task: Task) {
        com.sg.taskspace.util.FocusTimerManager.startTimer(task.id, task.title)
    }

    fun pauseFocusTimer() {
        com.sg.taskspace.util.FocusTimerManager.pauseTimer()
    }

    fun resumeFocusTimer() {
        com.sg.taskspace.util.FocusTimerManager.resumeTimer()
    }

    fun stopFocusTimer(task: Task) {
        viewModelScope.launch {
            val result = com.sg.taskspace.util.FocusTimerManager.stopTimer()
            if (result != null && result.second > 0) {
                val newTracked = task.trackedMinutes + result.second
                val updated = task.copy(trackedMinutes = newTracked)
                taskRepository.updateTask(updated)
            }
        }
    }

    fun discardFocusTimer() {
        com.sg.taskspace.util.FocusTimerManager.discardTimer()
    }

    fun updateTaskTrackedMinutes(task: Task, newMinutes: Int) {
        viewModelScope.launch {
            val updated = task.copy(trackedMinutes = maxOf(0, newMinutes))
            taskRepository.updateTask(updated)
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

    private val _selectedDate = MutableStateFlow(currentDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    data class DayStats(val date: LocalDate, val totalTasks: Int, val completedTasks: Int, val tasks: List<Task> = emptyList())

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
                    completedTasks = dailyTasks.count { it.isCompleted },
                    tasks = dailyTasks
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
       
        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDate(dateIso, ""), 
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
                    completedTasks = dailyTasks.count { it.isCompleted },
                    tasks = dailyTasks
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
        val needsFocus: String,
        val totalEstimatedMinutes: Int = 0,
        val completedEstimatedMinutes: Int = 0,
        val avgDailyWorkloadMinutes: Int = 0,
        val projectDistribution: Map<String, Pair<Int, Int>> = emptyMap() // ProjectId -> (Total, Completed)
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
                var streak = 0
                var checkDate = LocalDate.now()
                // Helper to check if a day has completed tasks
                fun hasCompletedTasks(date: LocalDate): Boolean {
                    val tasks = getTasksForDay(date, streakCandidateTasks, repeatingTasks)
                    return tasks.any { it.isCompleted }
                }

                if (hasCompletedTasks(checkDate)) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                } else {
                     checkDate = checkDate.minusDays(1)
                }

                while (hasCompletedTasks(checkDate)) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                    if (streak > 365) break // Safety break
                }

                // 5. Needs Focus
                val needsFocus = allMonthTasks.groupBy { it.category }
                    .filter { it.value.isNotEmpty() }
                    .minByOrNull { entry ->
                        val total = entry.value.size
                        val completed = entry.value.count { it.isCompleted }
                        if (total > 0) completed.toFloat() / total else 1f // If 0 tasks, ignore (shouldn't happen due to filter)
                    }?.key?.ifEmpty { "Uncategorized" } ?: "None"

                // 6. Time Estimates & Workload Analytics
                val totalEstMin = allMonthTasks.sumOf { it.estimatedMinutes }
                val completedEstMin = allMonthTasks.filter { it.isCompleted }.sumOf { it.estimatedMinutes }
                val daysInMonth = endOfMonth.dayOfMonth
                val avgDailyWorkload = if (daysInMonth > 0) totalEstMin / daysInMonth else 0

                // 7. Project Distribution
                val projectDist = allMonthTasks.filter { !it.projectId.isNullOrBlank() }
                    .groupBy { it.projectId!! }
                    .mapValues { entry -> Pair(entry.value.size, entry.value.count { it.isCompleted }) }

                InsightsData(
                    totalTasksThisMonth = totalMonth,
                    completedTasksThisMonth = completedMonth,
                    completionRateThisMonth = rate,
                    bestDayOfWeek = bestDay,
                    currentStreak = streak,
                    categoryDistribution = categories,
                    needsFocus = needsFocus,
                    totalEstimatedMinutes = totalEstMin,
                    completedEstimatedMinutes = completedEstMin,
                    avgDailyWorkloadMinutes = avgDailyWorkload,
                    projectDistribution = projectDist
                )
            } catch (e: Exception) {
                e.printStackTrace()
                InsightsData(0, 0, 0, "Error", 0, emptyMap(), "Error", 0, 0, 0, emptyMap())
            }
        }
    }

    val allTasks: StateFlow<List<Task>> = taskRepository.getAllTasks()
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUniqueTags: Flow<List<String>> = allTasks.map { tasks ->
        tasks.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private fun scheduleTaskReminderIfNeeded(task: Task) {
        if (task.time.isNullOrBlank() || task.isCompleted || task.isDeleted) {
            com.sg.taskspace.util.NotificationHelper.cancelReminder(TaskSpaceApplication.instance, task.id)
            return
        }
        try {
            val taskDate = LocalDate.parse(task.createdForDate, DateTimeFormatter.ISO_DATE)
            val taskTime = com.sg.taskspace.util.DateTimeUtils.parseLocalTime(task.time) ?: return
            val triggerMillis = java.time.LocalDateTime.of(taskDate, taskTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (triggerMillis > System.currentTimeMillis()) {
                com.sg.taskspace.util.NotificationHelper.scheduleReminder(
                    context = TaskSpaceApplication.instance,
                    id = task.id,
                    title = "Task Reminder: ${task.title}",
                    notes = task.notes ?: "Scheduled for ${task.time}",
                    triggerAtMillis = triggerMillis
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addTask(
        title: String,
        notes: String,
        priority: String,
        category: String,
        repeat: String = "None",
        date: LocalDate? = null,
        estimatedMinutes: Int = 0,
        projectId: String? = null,
        tags: String = "",
        time: String? = null
    ) {
        viewModelScope.launch {
            val targetDate = date ?: _selectedDate.value
            val task = Task(
                title = title,
                notes = notes,
                priority = priority,
                category = category,
                createdForDate = targetDate.format(DateTimeFormatter.ISO_DATE),
                repeat = repeat,
                estimatedMinutes = estimatedMinutes,
                projectId = projectId,
                tags = tags,
                time = time
            )
            taskRepository.insertTask(task)
            scheduleTaskReminderIfNeeded(task)
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }
    
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val willBeCompleted = !task.isCompleted
            val xp = if (task.priority == "High") 20 else 10
            if (willBeCompleted) {
                userPreferencesRepository.addXp(xp)
            } else {
                userPreferencesRepository.addXp(-xp)
            }
            if (task.repeat != "None") {
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
                taskRepository.deleteTask(task)
                com.sg.taskspace.util.NotificationHelper.cancelReminder(TaskSpaceApplication.instance, task.id)
            } else {
                val updated = task.copy(isCompleted = !task.isCompleted)
                taskRepository.updateTask(updated)
                scheduleTaskReminderIfNeeded(updated)
            }
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    fun saveColorPalette(colorPalette: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateColorPalette(colorPalette)
        }
    }

    fun saveSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateSoundEffectsEnabled(enabled)
        }
    }

    fun saveAutoBackupFrequency(frequency: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutoBackupFrequency(frequency)
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.softDeleteTask(task.id)
            com.sg.taskspace.util.NotificationHelper.cancelReminder(TaskSpaceApplication.instance, task.id)
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.restoreTask(taskId)
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
        }
    }

    fun updateTaskDetails(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            scheduleTaskReminderIfNeeded(task)
            com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(TaskSpaceApplication.instance)
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

    fun getTaskById(taskId: String): Flow<Task?> {
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
