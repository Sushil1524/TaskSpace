package com.sg.taskspace.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
        val FONT_FAMILY = stringPreferencesKey("font_family") // "Default", "Sans-Serif", "Serif", "Monospace"
        // Data & Security
        val IS_SECURE_BACKUP_ENABLED = booleanPreferencesKey("is_secure_backup_enabled")
        // Notifications
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
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
            val isSecureBackupEnabled = preferences[PreferencesKeys.IS_SECURE_BACKUP_ENABLED] ?: true
            val notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false
            UserPreferences(
                userName = userName,
                isOnboardingCompleted = isOnboardingCompleted,
                themeMode = themeMode,
                fontFamily = fontFamily,
                isSecureBackupEnabled = isSecureBackupEnabled,
                notificationsEnabled = notificationsEnabled
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

    suspend fun updateSecureBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SECURE_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
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
        isSecureBackupEnabled: Boolean = true,
        notificationsEnabled: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = userName
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = isOnboardingCompleted
            preferences[PreferencesKeys.THEME_MODE] = themeMode
            preferences[PreferencesKeys.FONT_FAMILY] = fontFamily
            preferences[PreferencesKeys.IS_SECURE_BACKUP_ENABLED] = isSecureBackupEnabled
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = notificationsEnabled
        }
    }
}

data class UserPreferences(
    @SerializedName("userName") val userName: String,
    @SerializedName("isOnboardingCompleted") val isOnboardingCompleted: Boolean,
    @SerializedName("themeMode") val themeMode: String = "AMOLED Dark",
    @SerializedName("fontFamily") val fontFamily: String = "Default",
    @SerializedName("isSecureBackupEnabled") val isSecureBackupEnabled: Boolean = true,
    @SerializedName("notificationsEnabled") val notificationsEnabled: Boolean = false
)
