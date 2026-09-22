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
    val isGeminiKeyMissing: Boolean = false,
    val selectedModel: String = "gemini-2.5-flash",
    val errorBanner: String? = null,
    val liveTranscript: String = ""
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
    }

    fun checkKeyStatus() {
        val hasKey = keyRepository.hasKey(SecureKeyStore.KEY_GEMINI)
        _uiState.value = _uiState.value.copy(
            isGeminiKeyMissing = !hasKey,
            errorBanner = if (!hasKey) "Gemini API key is not configured. Jarvis core is operating in offline standby." else null
        )
    }

    fun startVoiceInput(onError: (String) -> Unit = {}) {
        voiceManager.startListeningSession(
            onFinalTranscript = { transcript ->
                if (transcript.isNotBlank()) {
                    sendMessage(transcript)
                }
            },
            onError = { error ->
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
