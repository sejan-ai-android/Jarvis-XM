package com.example.voice

import android.content.Context
import com.example.data.prefs.SecureKeyStore
import com.example.ui.components.OrbState
import com.example.voice.stt.SpeechToTextManager
import com.example.voice.tts.TextToSpeechManager
import com.example.voice.wakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisVoiceManager(
    private val context: Context,
    private val keyStore: SecureKeyStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    val sttManager = SpeechToTextManager(context)
    val ttsManager = TextToSpeechManager(context)
    val wakeWordDetector = WakeWordDetector(context, keyStore)

    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript: StateFlow<String> = _voiceTranscript.asStateFlow()

    private val _combinedRms = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _combinedRms.asStateFlow()

    private val _isContinuousConversationActive = MutableStateFlow(false)
    val isContinuousConversationActive: StateFlow<Boolean> = _isContinuousConversationActive.asStateFlow()

    val isListening: StateFlow<Boolean> = sttManager.isListening

    init {
        // Observe TTS state
        coroutineScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _orbState.value = OrbState.SPEAKING
                } else if (_orbState.value == OrbState.SPEAKING) {
                    _orbState.value = OrbState.IDLE
                }
            }
        }

        // Observe STT partial results
        coroutineScope.launch {
            sttManager.partialText.collect { partial ->
                if (partial.isNotBlank()) {
                    _voiceTranscript.value = partial
                }
            }
        }

        // Combine STT & TTS RMS levels for fluid Arc Reactor visuals
        coroutineScope.launch {
            sttManager.rmsDb.collect { sttRms ->
                if (sttManager.isListening.value) {
                    _combinedRms.value = sttRms
                }
            }
        }

        coroutineScope.launch {
            ttsManager.speakingRms.collect { ttsRms ->
                if (ttsManager.isSpeaking.value) {
                    _combinedRms.value = ttsRms
                } else if (!sttManager.isListening.value) {
                    _combinedRms.value = 0f
                }
            }
        }
    }

    fun setContinuousConversation(active: Boolean) {
        _isContinuousConversationActive.value = active
    }

    fun startListeningSession(
        onFinalTranscript: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        // If currently speaking, interrupt TTS
        ttsManager.stop()

        _orbState.value = OrbState.LISTENING
        _voiceTranscript.value = ""

        sttManager.startListening(
            onFinalResult = { transcript ->
                _voiceTranscript.value = transcript
                _orbState.value = OrbState.THINKING
                _combinedRms.value = 0f
                onFinalTranscript(transcript)
            },
            onError = { error ->
                _orbState.value = OrbState.IDLE
                _combinedRms.value = 0f
                onError(error)
            }
        )
    }

    fun speakAssistantResponse(text: String, speechRate: Float = 1.05f, onDone: () -> Unit = {}) {
        _orbState.value = OrbState.SPEAKING
        ttsManager.speak(
            text = text,
            speechRate = speechRate,
            onFinished = {
                _orbState.value = OrbState.IDLE
                _combinedRms.value = 0f
                onDone()
            }
        )
    }

    fun setThinking() {
        _orbState.value = OrbState.THINKING
    }

    fun setIdle() {
        _orbState.value = OrbState.IDLE
    }

    fun stop() {
        sttManager.stopListening()
        ttsManager.stop()
        _orbState.value = OrbState.IDLE
    }

    fun destroy() {
        stop()
        ttsManager.shutdown()
        wakeWordDetector.stopDetection()
    }
}
