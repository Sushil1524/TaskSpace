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
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        // Gamification
        val TOTAL_XP = intPreferencesKey("total_xp")
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
            val soundEffectsEnabled = preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] ?: true
            val totalXp = preferences[PreferencesKeys.TOTAL_XP] ?: 0

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
                soundEffectsEnabled = soundEffectsEnabled,
                totalXp = totalXp
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

    suspend fun updateSoundEffectsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] = enabled
        }
    }

    suspend fun addXp(amount: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TOTAL_XP] ?: 0
            preferences[PreferencesKeys.TOTAL_XP] = (current + amount).coerceAtLeast(0)
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
        soundEffectsEnabled: Boolean = true,
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
            preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] = soundEffectsEnabled
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
    @SerializedName("soundEffectsEnabled") val soundEffectsEnabled: Boolean = true,
    @SerializedName("totalXp") val totalXp: Int = 0
) {
    // Gamification level calculation: Level = (XP / 100) + 1
    val level: Int get() = (totalXp / 100) + 1
    val currentLevelXp: Int get() = totalXp % 100
    val xpProgressFraction: Float get() = currentLevelXp.toFloat() / 100f
}
