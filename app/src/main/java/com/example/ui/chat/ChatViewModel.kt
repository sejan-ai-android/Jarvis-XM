package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.gemini.GeminiRepository
import com.example.core.model.JarvisResult
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationRepository
import com.example.data.local.MessageEntity
import com.example.data.prefs.KeyRepository
import com.example.data.prefs.SecureKeyStore
import com.example.data.prefs.UserPreferences
import com.example.data.prefs.UserPreferencesRepository
import com.example.ui.components.OrbState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentConversation: ConversationEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val isThinking: Boolean = false,
    val isListening: Boolean = false,
    val orbState: OrbState = OrbState.IDLE,
    val rmsDb: Float = 0f,
    val isGeminiKeyMissing: Boolean = false,
    val selectedModel: String = "gemini-2.5-flash",
    val errorBanner: String? = null,
    val liveTranscript: String = "",
    val isAudioConversationActive: Boolean = false,
    val isContinuousLoopEnabled: Boolean = true,
    val lastSpokenResponse: String = ""
)

class ChatViewModel(
    private val conversationRepository: ConversationRepository,
    private val geminiRepository: GeminiRepository,
    private val keyRepository: KeyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val voiceManager: com.example.voice.JarvisVoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    init {
        checkKeyStatus()
        initializeDefaultConversation()

        // Sync Voice Orb State
        viewModelScope.launch {
            voiceManager.orbState.collect { orb ->
                _uiState.value = _uiState.value.copy(
                    orbState = orb,
                    isListening = orb == OrbState.LISTENING
                )
            }
        }

        // Sync Live Transcript
        viewModelScope.launch {
            voiceManager.voiceTranscript.collect { transcript ->
                _uiState.value = _uiState.value.copy(liveTranscript = transcript)
            }
        }

        // Sync RMS level for Arc Reactor reactivity
        viewModelScope.launch {
            voiceManager.rmsDb.collect { rms ->
                _uiState.value = _uiState.value.copy(rmsDb = rms)
            }
        }
    }

    fun checkKeyStatus() {
        val hasKey = keyRepository.hasKey(SecureKeyStore.KEY_GEMINI)
        _uiState.value = _uiState.value.copy(
            isGeminiKeyMissing = !hasKey,
            errorBanner = if (!hasKey) "Gemini API key is not configured. Jarvis core is operating in offline standby." else null
        )
    }

    fun startAudioConversation() {
        _uiState.value = _uiState.value.copy(
            isAudioConversationActive = true,
            errorBanner = null,
            liveTranscript = ""
        )
        voiceManager.setContinuousConversation(true)
        startAudioConversationListenLoop()
    }

    fun stopAudioConversation() {
        _uiState.value = _uiState.value.copy(
            isAudioConversationActive = false,
            liveTranscript = ""
        )
        voiceManager.setContinuousConversation(false)
        voiceManager.stop()
    }

    fun toggleAudioConversation() {
        if (_uiState.value.isAudioConversationActive) {
            stopAudioConversation()
        } else {
            startAudioConversation()
        }
    }

    fun toggleContinuousLoop() {
        val next = !_uiState.value.isContinuousLoopEnabled
        _uiState.value = _uiState.value.copy(isContinuousLoopEnabled = next)
        voiceManager.setContinuousConversation(next)
    }

    private fun startAudioConversationListenLoop() {
        if (!_uiState.value.isAudioConversationActive) return

        voiceManager.startListeningSession(
            onFinalTranscript = { transcript ->
                if (transcript.isNotBlank()) {
                    sendVoiceConversationMessage(transcript)
                } else if (_uiState.value.isAudioConversationActive && _uiState.value.isContinuousLoopEnabled) {
                    startAudioConversationListenLoop()
                }
            },
            onError = { error ->
                if (_uiState.value.isAudioConversationActive && _uiState.value.isContinuousLoopEnabled) {
                    if (error.contains("timeout", ignoreCase = true) || error.contains("No speech", ignoreCase = true)) {
                        // User was silent, keep listening in conversation mode
                        startAudioConversationListenLoop()
                    } else {
                        _uiState.value = _uiState.value.copy(errorBanner = error)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(errorBanner = error)
                }
            }
        )
    }

    private fun sendVoiceConversationMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val convId = _uiState.value.currentConversation?.id ?: "default_session"

        viewModelScope.launch {
            conversationRepository.insertMessage(
                conversationId = convId,
                sender = "user",
                content = trimmed
            )

            val hasKey = keyRepository.hasKey(SecureKeyStore.KEY_GEMINI)
            if (!hasKey) {
                val fallbackMsg = "Sir, my neural reasoning core is offline. Please configure your Gemini API key in Settings."
                conversationRepository.insertMessage(
                    conversationId = convId,
                    sender = "assistant",
                    content = fallbackMsg
                )
                _uiState.value = _uiState.value.copy(
                    isGeminiKeyMissing = true,
                    lastSpokenResponse = fallbackMsg,
                    errorBanner = "Gemini API key is required for voice responses."
                )
                voiceManager.speakAssistantResponse(fallbackMsg, preferences.value.speechRate) {
                    if (_uiState.value.isAudioConversationActive && _uiState.value.isContinuousLoopEnabled) {
                        startAudioConversationListenLoop()
                    }
                }
                return@launch
            }

            _uiState.value = _uiState.value.copy(isThinking = true, errorBanner = null)
            voiceManager.setThinking()

            val history = conversationRepository.getRecentMessages(convId, 20).reversed()
            val result = geminiRepository.generateAssistantResponse(
                history = history,
                userMessage = trimmed,
                preferences = preferences.value,
                modelName = _uiState.value.selectedModel
            )

            when (result) {
                is JarvisResult.Success -> {
                    conversationRepository.insertMessage(
                        conversationId = convId,
                        sender = "assistant",
                        content = result.data
                    )
                    _uiState.value = _uiState.value.copy(
                        isThinking = false,
                        lastSpokenResponse = result.data
                    )

                    // Speak response and automatically continue conversation loop if active
                    voiceManager.speakAssistantResponse(result.data, preferences.value.speechRate) {
                        if (_uiState.value.isAudioConversationActive && _uiState.value.isContinuousLoopEnabled) {
                            startAudioConversationListenLoop()
                        }
                    }
                }
                is JarvisResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isThinking = false,
                        isGeminiKeyMissing = result.isKeyMissing,
                        errorBanner = result.message
                    )
                    voiceManager.setIdle()
                    val errorAlert = "⚠️ Protocol alert: ${result.message}"
                    conversationRepository.insertMessage(
                        conversationId = convId,
                        sender = "assistant",
                        content = errorAlert
                    )
                }
                is JarvisResult.Loading -> {}
            }
        }
    }

    fun startVoiceInput(onError: (String) -> Unit = {}) {
        voiceManager.startListeningSession(
            onFinalTranscript = { transcript ->
                if (transcript.isNotBlank()) {
                    sendMessage(transcript)
                }
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(errorBanner = error)
                onError(error)
            }
        )
    }

    fun stopVoiceInput() {
        voiceManager.stop()
    }

    private fun initializeDefaultConversation() {
        viewModelScope.launch {
            val conv = conversationRepository.getOrCreateDefaultConversation()
            _uiState.value = _uiState.value.copy(currentConversation = conv)

            conversationRepository.getMessagesForConversation(conv.id).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun setSelectedModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(errorBanner = null)
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val convId = _uiState.value.currentConversation?.id ?: "default_session"

        viewModelScope.launch {
            // 1. Persist user message in Room
            conversationRepository.insertMessage(
                conversationId = convId,
                sender = "user",
                content = trimmed
            )

            // 2. Verify key availability
            val hasKey = keyRepository.hasKey(SecureKeyStore.KEY_GEMINI)
            if (!hasKey) {
                _uiState.value = _uiState.value.copy(
                    isGeminiKeyMissing = true,
                    errorBanner = "This feature requires a Gemini API key. Tap to configure it in Settings."
                )
                val msg = "I am ready to assist you, Sir, but my neural reasoning core is offline. Please tap the banner above or open Settings to enter your Gemini API key."
                conversationRepository.insertMessage(
                    conversationId = convId,
                    sender = "assistant",
                    content = msg
                )
                if (preferences.value.autoSpeakResponses) {
                    voiceManager.speakAssistantResponse(msg, preferences.value.speechRate)
                }
                return@launch
            }

            // 3. Set UI state to Thinking
            _uiState.value = _uiState.value.copy(
                isThinking = true,
                errorBanner = null
            )
            voiceManager.setThinking()

            // 4. Fetch context and invoke Gemini
            val history = conversationRepository.getRecentMessages(convId, 20).reversed()
            val result = geminiRepository.generateAssistantResponse(
                history = history,
                userMessage = trimmed,
                preferences = preferences.value,
                modelName = _uiState.value.selectedModel
            )

            when (result) {
                is JarvisResult.Success -> {
                    conversationRepository.insertMessage(
                        conversationId = convId,
                        sender = "assistant",
                        content = result.data
                    )
                    _uiState.value = _uiState.value.copy(isThinking = false)

                    // Speak response aloud if enabled in preferences
                    if (preferences.value.autoSpeakResponses) {
                        voiceManager.speakAssistantResponse(result.data, preferences.value.speechRate)
                    } else {
                        voiceManager.setIdle()
                    }
                }
                is JarvisResult.Error -> {
                    val isKeyError = result.isKeyMissing
                    _uiState.value = _uiState.value.copy(
                        isThinking = false,
                        isGeminiKeyMissing = isKeyError,
                        errorBanner = result.message
                    )
                    voiceManager.setIdle()
                    conversationRepository.insertMessage(
                        conversationId = convId,
                        sender = "assistant",
                        content = "⚠️ Protocol alert: ${result.message}"
                    )
                }
                is JarvisResult.Loading -> {}
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationRepository.clearAll()
            initializeDefaultConversation()
        }
    }

    class Factory(
        private val conversationRepository: ConversationRepository,
        private val geminiRepository: GeminiRepository,
        private val keyRepository: KeyRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val voiceManager: com.example.voice.JarvisVoiceManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(
                conversationRepository,
                geminiRepository,
                keyRepository,
                userPreferencesRepository,
                voiceManager
            ) as T
        }
    }
}
