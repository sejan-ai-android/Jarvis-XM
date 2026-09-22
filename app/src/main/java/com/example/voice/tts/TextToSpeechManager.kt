package com.example.voice.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingRms = MutableStateFlow(0f)
    val speakingRms: StateFlow<Float> = _speakingRms.asStateFlow()

    private var currentCallback: (() -> Unit)? = null
    private var rmsSimulationJob: Job? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Default US Language not supported on this device TTS engine")
            }
            tts?.setSpeechRate(1.05f) // Crisp Jarvis cadence
            tts?.setPitch(0.95f)      // Sophisticated Jarvis tone

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startRmsSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopRmsSimulation()
                    mainHandler.post {
                        val cb = currentCallback
                        currentCallback = null
                        cb?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopRmsSimulation()
                    mainHandler.post {
                        val cb = currentCallback
                        currentCallback = null
                        cb?.invoke()
                    }
                }
            })
            isInitialized = true
        } else {
            Log.e(TAG, "Failed to initialize Android TextToSpeech engine")
        }
    }

    private fun startRmsSimulation() {
        rmsSimulationJob?.cancel()
        rmsSimulationJob = scope.launch {
            while (isActive && _isSpeaking.value) {
                _speakingRms.value = Random.nextFloat() * 7f + 2.5f
                delay(80)
            }
            _speakingRms.value = 0f
        }
    }

    private fun stopRmsSimulation() {
        rmsSimulationJob?.cancel()
        _speakingRms.value = 0f
    }

    fun speak(text: String, speechRate: Float = 1.05f, onFinished: () -> Unit = {}) {
        if (!isInitialized || text.isBlank()) {
            mainHandler.post { onFinished() }
            return
        }

        stop()
        currentCallback = onFinished
        tts?.setSpeechRate(speechRate)

        val utteranceId = UUID.randomUUID().toString()
        // Strip markdown asterisks and backticks for clean speech
        val cleanedText = cleanMarkdownForSpeech(text)

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        stopRmsSimulation()
        val hadCallback = currentCallback
        currentCallback = null
        if (_isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
        }
        hadCallback?.let { cb ->
            mainHandler.post { cb() }
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    internal fun cleanMarkdownForSpeech(text: String): String {
        return text
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // bold
            .replace(Regex("\\*([^*]+)\\*"), "$1")       // italic
            .replace(Regex("`([^`]+)`"), "$1")           // code
            .replace(Regex("#+\\s"), "")                 // headers
            .replace(Regex("[-*]\\s"), "")               // bullets
            .trim()
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
