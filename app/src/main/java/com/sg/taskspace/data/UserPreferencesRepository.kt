package com.sg.taskspace.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import com.google.gson.annotations.SerializedName

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        // Appearance
        val THEME_MODE = stringPreferencesKey("theme_mode") // "AMOLED Dark", "Dark", "Light", "System"
        val FONT_FAMILY = stringPreferencesKey("font_family") // "Default", "Rounded", "Serif", "Monospace"
        val COLOR_PALETTE = stringPreferencesKey("color_palette") // "Indigo", "Emerald", "Rose", "Amber", "Violet", "Teal"
        // Data & Security
        val IS_SECURE_BACKUP_ENABLED = booleanPreferencesKey("is_secure_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency") // "Daily", "Weekly", "Off"
        val LAST_AUTO_BACKUP_TIME = longPreferencesKey("last_auto_backup_time")
        // Notifications & Feedback
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PERSISTENT_DAILY_DASHBOARD_ENABLED = booleanPreferencesKey("persistent_daily_dashboard_enabled")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        // Gestures & Swipe Actions
        val SWIPE_RIGHT_ACTION = stringPreferencesKey("swipe_right_action") // "Complete", "Snooze", "Edit", "Delete", "None"
        val SWIPE_LEFT_ACTION = stringPreferencesKey("swipe_left_action")  // "Delete", "Complete", "Snooze", "Edit", "None"
        // App Lock & Security
        val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout") // "Immediately", "1 minute", "5 minutes", "15 minutes", "30 minutes"
        val LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")
        // Gamification
        val TOTAL_XP = intPreferencesKey("total_xp")
        // Vacation Mode
        val IS_VACATION_MODE_ENABLED = booleanPreferencesKey("is_vacation_mode_enabled")
        val VACATION_START_DATE = stringPreferencesKey("vacation_start_date")
        val VACATION_END_DATE = stringPreferencesKey("vacation_end_date")
        val VACATION_REASON = stringPreferencesKey("vacation_reason")
        val FREEZE_HABIT_STREAKS = booleanPreferencesKey("freeze_habit_streaks")
        val PAUSE_TASK_REMINDERS = booleanPreferencesKey("pause_task_reminders")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val userName = preferences[PreferencesKeys.USER_NAME] ?: "User"
            val isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
            val themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "AMOLED Dark"
            val fontFamily = preferences[PreferencesKeys.FONT_FAMILY] ?: "Default"
            val colorPalette = preferences[PreferencesKeys.COLOR_PALETTE] ?: "Indigo"
            val isSecureBackupEnabled = preferences[PreferencesKeys.IS_SECURE_BACKUP_ENABLED] ?: true
            val autoBackupFrequency = preferences[PreferencesKeys.AUTO_BACKUP_FREQUENCY] ?: "Daily"
            val lastAutoBackupTimeMillis = preferences[PreferencesKeys.LAST_AUTO_BACKUP_TIME] ?: 0L
            val notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false
            val persistentDailyDashboardEnabled = preferences[PreferencesKeys.PERSISTENT_DAILY_DASHBOARD_ENABLED] ?: false
            val soundEffectsEnabled = preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] ?: true
            val swipeRightAction = preferences[PreferencesKeys.SWIPE_RIGHT_ACTION] ?: "Complete"
            val swipeLeftAction = preferences[PreferencesKeys.SWIPE_LEFT_ACTION] ?: "Delete"
            val isAppLockEnabled = preferences[PreferencesKeys.IS_APP_LOCK_ENABLED] ?: false
            val appLockPinHash = preferences[PreferencesKeys.APP_LOCK_PIN_HASH] ?: ""
            val isBiometricEnabled = preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] ?: true
            val autoLockTimeout = preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] ?: "Immediately"
            val lastBackgroundTimeMillis = preferences[PreferencesKeys.LAST_BACKGROUND_TIME] ?: 0L
            val totalXp = preferences[PreferencesKeys.TOTAL_XP] ?: 0
            val isVacationModeEnabled = preferences[PreferencesKeys.IS_VACATION_MODE_ENABLED] ?: false
            val vacationStartDate = preferences[PreferencesKeys.VACATION_START_DATE] ?: ""
            val vacationEndDate = preferences[PreferencesKeys.VACATION_END_DATE] ?: ""
            val vacationReason = preferences[PreferencesKeys.VACATION_REASON] ?: "Vacation"
            val freezeHabitStreaks = preferences[PreferencesKeys.FREEZE_HABIT_STREAKS] ?: true
            val pauseTaskReminders = preferences[PreferencesKeys.PAUSE_TASK_REMINDERS] ?: true

            UserPreferences(
                userName = userName,
                isOnboardingCompleted = isOnboardingCompleted,
                themeMode = themeMode,
                fontFamily = fontFamily,
                colorPalette = colorPalette,
                isSecureBackupEnabled = isSecureBackupEnabled,
                autoBackupFrequency = autoBackupFrequency,
                lastAutoBackupTimeMillis = lastAutoBackupTimeMillis,
                notificationsEnabled = notificationsEnabled,
                persistentDailyDashboardEnabled = persistentDailyDashboardEnabled,
                soundEffectsEnabled = soundEffectsEnabled,
                swipeRightAction = swipeRightAction,
                swipeLeftAction = swipeLeftAction,
                isAppLockEnabled = isAppLockEnabled,
                appLockPinHash = appLockPinHash,
                isBiometricEnabled = isBiometricEnabled,
                autoLockTimeout = autoLockTimeout,
                lastBackgroundTimeMillis = lastBackgroundTimeMillis,
                totalXp = totalXp,
                isVacationModeEnabled = isVacationModeEnabled,
                vacationStartDate = vacationStartDate,
                vacationEndDate = vacationEndDate,
                vacationReason = vacationReason,
                freezeHabitStreaks = freezeHabitStreaks,
                pauseTaskReminders = pauseTaskReminders
            )
        }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateFontFamily(fontFamily: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_FAMILY] = fontFamily
        }
    }

    suspend fun updateColorPalette(colorPalette: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLOR_PALETTE] = colorPalette
        }
    }

    suspend fun updateSecureBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SECURE_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun updateAutoBackupFrequency(frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun updateLastAutoBackupTime(timeMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_AUTO_BACKUP_TIME] = timeMillis
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updatePersistentDailyDashboardEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PERSISTENT_DAILY_DASHBOARD_ENABLED] = enabled
        }
    }

    suspend fun updateSoundEffectsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] = enabled
        }
    }

    suspend fun updateSwipeRightAction(action: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SWIPE_RIGHT_ACTION] = action
        }
    }

    suspend fun updateSwipeLeftAction(action: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SWIPE_LEFT_ACTION] = action
        }
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun updateAppLockPinHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN_HASH] = hash
        }
    }

    suspend fun updateBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun updateAutoLockTimeout(timeout: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] = timeout
        }
    }

    suspend fun updateLastBackgroundTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKGROUND_TIME] = timestamp
        }
    }

    suspend fun addXp(amount: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TOTAL_XP] ?: 0
            preferences[PreferencesKeys.TOTAL_XP] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun updateVacationMode(
        enabled: Boolean,
        startDate: String = "",
        endDate: String = "",
        reason: String = "Vacation",
        freezeHabits: Boolean = true,
        pauseReminders: Boolean = true
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VACATION_MODE_ENABLED] = enabled
            preferences[PreferencesKeys.VACATION_START_DATE] = startDate
            preferences[PreferencesKeys.VACATION_END_DATE] = endDate
            preferences[PreferencesKeys.VACATION_REASON] = reason
            preferences[PreferencesKeys.FREEZE_HABIT_STREAKS] = freezeHabits
            preferences[PreferencesKeys.PAUSE_TASK_REMINDERS] = pauseReminders
        }
    }

    suspend fun clearVacationMode() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VACATION_MODE_ENABLED] = false
            preferences[PreferencesKeys.VACATION_START_DATE] = ""
            preferences[PreferencesKeys.VACATION_END_DATE] = ""
            preferences[PreferencesKeys.VACATION_REASON] = ""
        }
    }

    suspend fun fetchInitialPreferences(): UserPreferences {
        return userPreferencesFlow.first()
    }

    suspend fun restorePreferences(
        userName: String,
        isOnboardingCompleted: Boolean,
        themeMode: String = "AMOLED Dark",
        fontFamily: String = "Default",
        colorPalette: String = "Indigo",
        isSecureBackupEnabled: Boolean = true,
        autoBackupFrequency: String = "Daily",
        notificationsEnabled: Boolean = false,
        persistentDailyDashboardEnabled: Boolean = false,
        soundEffectsEnabled: Boolean = true,
        swipeRightAction: String = "Complete",
        swipeLeftAction: String = "Delete",
        isAppLockEnabled: Boolean = false,
        appLockPinHash: String = "",
        isBiometricEnabled: Boolean = true,
        autoLockTimeout: String = "Immediately",
        totalXp: Int = 0
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = userName
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = isOnboardingCompleted
            preferences[PreferencesKeys.THEME_MODE] = themeMode
            preferences[PreferencesKeys.FONT_FAMILY] = fontFamily
            preferences[PreferencesKeys.COLOR_PALETTE] = colorPalette
            preferences[PreferencesKeys.IS_SECURE_BACKUP_ENABLED] = isSecureBackupEnabled
            preferences[PreferencesKeys.AUTO_BACKUP_FREQUENCY] = autoBackupFrequency
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = notificationsEnabled
            preferences[PreferencesKeys.PERSISTENT_DAILY_DASHBOARD_ENABLED] = persistentDailyDashboardEnabled
            preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] = soundEffectsEnabled
            preferences[PreferencesKeys.SWIPE_RIGHT_ACTION] = swipeRightAction
            preferences[PreferencesKeys.SWIPE_LEFT_ACTION] = swipeLeftAction
            preferences[PreferencesKeys.IS_APP_LOCK_ENABLED] = isAppLockEnabled
            preferences[PreferencesKeys.APP_LOCK_PIN_HASH] = appLockPinHash
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] = isBiometricEnabled
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] = autoLockTimeout
            preferences[PreferencesKeys.TOTAL_XP] = totalXp
        }
    }
}

data class UserPreferences(
    @SerializedName("userName") val userName: String,
    @SerializedName("isOnboardingCompleted") val isOnboardingCompleted: Boolean,
    @SerializedName("themeMode") val themeMode: String = "AMOLED Dark",
    @SerializedName("fontFamily") val fontFamily: String = "Default",
    @SerializedName("colorPalette") val colorPalette: String = "Indigo",
    @SerializedName("isSecureBackupEnabled") val isSecureBackupEnabled: Boolean = true,
    @SerializedName("autoBackupFrequency") val autoBackupFrequency: String = "Daily",
    @SerializedName("lastAutoBackupTimeMillis") val lastAutoBackupTimeMillis: Long = 0L,
    @SerializedName("notificationsEnabled") val notificationsEnabled: Boolean = false,
    @SerializedName("persistentDailyDashboardEnabled") val persistentDailyDashboardEnabled: Boolean = false,
    @SerializedName("soundEffectsEnabled") val soundEffectsEnabled: Boolean = true,
    @SerializedName("swipeRightAction") val swipeRightAction: String = "Complete",
    @SerializedName("swipeLeftAction") val swipeLeftAction: String = "Delete",
    @SerializedName("isAppLockEnabled") val isAppLockEnabled: Boolean = false,
    @SerializedName("appLockPinHash") val appLockPinHash: String = "",
    @SerializedName("isBiometricEnabled") val isBiometricEnabled: Boolean = true,
    @SerializedName("autoLockTimeout") val autoLockTimeout: String = "Immediately",
    @SerializedName("lastBackgroundTimeMillis") val lastBackgroundTimeMillis: Long = 0L,
    @SerializedName("totalXp") val totalXp: Int = 0,
    @SerializedName("isVacationModeEnabled") val isVacationModeEnabled: Boolean = false,
    @SerializedName("vacationStartDate") val vacationStartDate: String = "",
    @SerializedName("vacationEndDate") val vacationEndDate: String = "",
    @SerializedName("vacationReason") val vacationReason: String = "Vacation",
    @SerializedName("freezeHabitStreaks") val freezeHabitStreaks: Boolean = true,
    @SerializedName("pauseTaskReminders") val pauseTaskReminders: Boolean = true
) {
    // Gamification level calculation: Level = (XP / 100) + 1
    val level: Int get() = (totalXp / 100) + 1
    val currentLevelXp: Int get() = totalXp % 100
    val xpProgressFraction: Float get() = currentLevelXp.toFloat() / 100f

    fun isDateInVacation(dateStr: String): Boolean {
        if (!isVacationModeEnabled) return false
        if (vacationStartDate.isBlank() || vacationEndDate.isBlank()) return true
        return try {
            val date = java.time.LocalDate.parse(dateStr)
            val start = java.time.LocalDate.parse(vacationStartDate)
            val end = java.time.LocalDate.parse(vacationEndDate)
            !date.isBefore(start) && !date.isAfter(end)
        } catch (e: Exception) {
            true
        }
    }

    fun isDateInVacation(date: java.time.LocalDate): Boolean {
        if (!isVacationModeEnabled) return false
        if (vacationStartDate.isBlank() || vacationEndDate.isBlank()) return true
        return try {
            val start = java.time.LocalDate.parse(vacationStartDate)
            val end = java.time.LocalDate.parse(vacationEndDate)
            !date.isBefore(start) && !date.isAfter(end)
        } catch (e: Exception) {
            true
        }
    }
}
