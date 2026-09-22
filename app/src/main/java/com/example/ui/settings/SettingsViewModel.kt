package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.prefs.ApiKeyConfig
import com.example.data.prefs.KeyRepository
import com.example.data.prefs.KeyValidationResult
import com.example.data.prefs.UserPreferences
import com.example.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val keyRepository: KeyRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val keysState: StateFlow<List<ApiKeyConfig>> = keyRepository.keysState

    val preferencesState: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun saveKey(keyId: String, value: String) {
        keyRepository.saveKey(keyId, value)
    }

    fun testKey(keyId: String, valueToTest: String? = null, onResult: (KeyValidationResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = keyRepository.testKey(keyId, valueToTest)
            onResult(result)
        }
    }

    fun clearAllKeys() {
        keyRepository.clearAllKeys()
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
        }
    }

    fun updateAssistantTone(tone: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAssistantTone(tone)
        }
    }

    fun updateSelectedLanguage(language: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSelectedLanguage(language)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun updateAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoSpeakResponses(enabled)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setSpeechRate(rate)
        }
    }

    fun toggleBackgroundVoice(context: android.content.Context, active: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBackgroundVoiceActive(active)
            if (active) {
                com.example.voice.service.JarvisVoiceService.startService(context)
            } else {
                com.example.voice.service.JarvisVoiceService.stopService(context)
            }
        }
    }

    fun toggleAlwaysListening(context: android.content.Context, active: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAlwaysListening(active)
            if (active) {
                com.example.voice.service.JarvisVoiceService.startService(context)
            }
        }
    }

    class Factory(
        private val keyRepository: KeyRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(keyRepository, userPreferencesRepository) as T
        }
    }
}
