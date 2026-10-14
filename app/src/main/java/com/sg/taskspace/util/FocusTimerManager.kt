package com.sg.taskspace.util

import android.content.Context
import com.sg.taskspace.TaskSpaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class FocusTimerState(
    val taskId: String? = null,
    val taskTitle: String = "",
    val isRunning: Boolean = false,
    val startTimeEpochMillis: Long = 0L,
    val accumulatedSeconds: Long = 0L
)

object FocusTimerManager {
    private const val PREFS_NAME = "taskspace_focus_timer_prefs"
    private const val KEY_TASK_ID = "focus_task_id"
    private const val KEY_TASK_TITLE = "focus_task_title"
    private const val KEY_IS_RUNNING = "focus_is_running"
    private const val KEY_START_TIME = "focus_start_time"
    private const val KEY_ACCUMULATED_SEC = "focus_accumulated_sec"

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _timerState = MutableStateFlow(FocusTimerState())
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    init {
        loadPersistedState()
        startTicker()
    }

    private fun getPrefs() = TaskSpaceApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadPersistedState() {
        val prefs = getPrefs()
        val taskId = prefs.getString(KEY_TASK_ID, null)
        val taskTitle = prefs.getString(KEY_TASK_TITLE, "") ?: ""
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val startTime = prefs.getLong(KEY_START_TIME, 0L)
        val accumulated = prefs.getLong(KEY_ACCUMULATED_SEC, 0L)

        val state = FocusTimerState(
            taskId = taskId,
            taskTitle = taskTitle,
            isRunning = isRunning,
            startTimeEpochMillis = startTime,
            accumulatedSeconds = accumulated
        )
        _timerState.value = state
        _elapsedSeconds.value = computeElapsedSeconds(state)
    }

    private fun persistState(state: FocusTimerState) {
        getPrefs().edit()
            .putString(KEY_TASK_ID, state.taskId)
            .putString(KEY_TASK_TITLE, state.taskTitle)
            .putBoolean(KEY_IS_RUNNING, state.isRunning)
            .putLong(KEY_START_TIME, state.startTimeEpochMillis)
            .putLong(KEY_ACCUMULATED_SEC, state.accumulatedSeconds)
            .apply()
    }

    private fun startTicker() {
        scope.launch {
            while (isActive) {
                val current = _timerState.value
                if (current.taskId != null) {
                    _elapsedSeconds.value = computeElapsedSeconds(current)
                } else {
                    _elapsedSeconds.value = 0L
                }
                delay(1000L)
            }
        }
    }

    fun computeElapsedSeconds(state: FocusTimerState): Long {
        if (state.taskId == null) return 0L
        return if (state.isRunning && state.startTimeEpochMillis > 0L) {
            val liveDiff = (System.currentTimeMillis() - state.startTimeEpochMillis) / 1000L
            maxOf(0L, state.accumulatedSeconds + liveDiff)
        } else {
            maxOf(0L, state.accumulatedSeconds)
        }
    }

    fun startTimer(taskId: String, taskTitle: String = "") {
        val newState = FocusTimerState(
            taskId = taskId,
            taskTitle = taskTitle,
            isRunning = true,
            startTimeEpochMillis = System.currentTimeMillis(),
            accumulatedSeconds = 0L
        )
        _timerState.value = newState
        _elapsedSeconds.value = 0L
        persistState(newState)
    }

    fun pauseTimer() {
        val current = _timerState.value
        if (current.taskId == null || !current.isRunning) return

        val currentElapsed = computeElapsedSeconds(current)
        val newState = current.copy(
            isRunning = false,
            startTimeEpochMillis = 0L,
            accumulatedSeconds = currentElapsed
        )
        _timerState.value = newState
        _elapsedSeconds.value = currentElapsed
        persistState(newState)
    }

    fun resumeTimer() {
        val current = _timerState.value
        if (current.taskId == null || current.isRunning) return

        val newState = current.copy(
            isRunning = true,
            startTimeEpochMillis = System.currentTimeMillis()
        )
        _timerState.value = newState
        persistState(newState)
    }

    /**
     * Stops the timer and returns Pair(taskId, minutesToAdd).
     * Minutes are rounded to nearest minute (minimum 1 min if >= 30 seconds).
     */
    fun stopTimer(): Pair<String, Int>? {
        val current = _timerState.value
        val taskId = current.taskId ?: return null
        val totalSec = computeElapsedSeconds(current)
        val minutesToAdd = when {
            totalSec < 5L -> 0
            totalSec < 60L -> 1
            else -> Math.round(totalSec.toDouble() / 60.0).toInt().coerceAtLeast(1)
        }

        val emptyState = FocusTimerState()
        _timerState.value = emptyState
        _elapsedSeconds.value = 0L
        persistState(emptyState)
        return Pair(taskId, minutesToAdd)
    }

    fun discardTimer() {
        val emptyState = FocusTimerState()
        _timerState.value = emptyState
        _elapsedSeconds.value = 0L
        persistState(emptyState)
    }

    fun formatSeconds(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSec = seconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSec)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSec)
        }
    }
}
