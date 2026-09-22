package com.example.voice.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Default US Language not supported on this device TTS engine")
            }
            tts?.setSpeechRate(1.05f) // Slight crisp Jarvis cadence
            tts?.setPitch(0.95f)      // Slightly deeper, sophisticated Jarvis tone

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isInitialized = true
        } else {
            Log.e(TAG, "Failed to initialize Android TextToSpeech engine")
        }
    }

    fun speak(text: String, speechRate: Float = 1.05f, onFinished: () -> Unit = {}) {
        if (!isInitialized || text.isBlank()) {
            onFinished()
            return
        }

        stop()
        tts?.setSpeechRate(speechRate)

        val utteranceId = UUID.randomUUID().toString()
        // Strip markdown asterisks and backticks for clean speech
        val cleanedText = cleanMarkdownForSpeech(text)

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (_isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
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
