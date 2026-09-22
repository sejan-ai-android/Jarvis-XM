package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_user_prefs")

data class UserPreferences(
    val userName: String = "Sir",
    val assistantTone: String = "Tactical Jarvis",
    val selectedLanguage: String = "English (US)",
    val themeMode: String = "Dark",
    val speechRate: Float = 1.0f,
    val hasCompletedOnboarding: Boolean = false,
    val autoSpeakResponses: Boolean = true,
    val backgroundVoiceActive: Boolean = false,
    val alwaysListening: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ASSISTANT_TONE = stringPreferencesKey("assistant_tone")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_responses")
        val BACKGROUND_VOICE = booleanPreferencesKey("background_voice_active")
        val ALWAYS_LISTENING = booleanPreferencesKey("always_listening_active")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            userName = preferences[Keys.USER_NAME] ?: "Sir",
            assistantTone = preferences[Keys.ASSISTANT_TONE] ?: "Tactical Jarvis",
            selectedLanguage = preferences[Keys.SELECTED_LANGUAGE] ?: "English (US)",
            themeMode = preferences[Keys.THEME_MODE] ?: "Dark",
            speechRate = preferences[Keys.SPEECH_RATE] ?: 1.0f,
            hasCompletedOnboarding = preferences[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            autoSpeakResponses = preferences[Keys.AUTO_SPEAK] ?: true,
            backgroundVoiceActive = preferences[Keys.BACKGROUND_VOICE] ?: false,
            alwaysListening = preferences[Keys.ALWAYS_LISTENING] ?: false
        )
    }

    suspend fun setAlwaysListening(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ALWAYS_LISTENING] = enabled
        }
    }

    suspend fun setAutoSpeakResponses(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTO_SPEAK] = enabled
        }
    }

    suspend fun setBackgroundVoiceActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BACKGROUND_VOICE] = active
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_NAME] = name
        }
    }

    suspend fun setAssistantTone(tone: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ASSISTANT_TONE] = tone
        }
    }

    suspend fun setSelectedLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SELECTED_LANGUAGE] = language
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SPEECH_RATE] = rate
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
