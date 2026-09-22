package com.example.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechToTextManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var endOfSpeechSafetyRunnable: Runnable? = null

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        mainHandler.post {
            stopListeningInternal()

            onFinalResultCallback = onFinalResult
            onErrorCallback = onError
            _partialText.value = ""

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Standardize to 0..10f scale for visual reactors
                            _rmsDb.value = rmsdB.coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                            // Ultra-fast response: If we already have partial recognized speech,
                            // don't wait indefinitely for Android's slow onResults timeout.
                            val currentPartial = _partialText.value.trim()
                            if (currentPartial.isNotBlank()) {
                                endOfSpeechSafetyRunnable = Runnable {
                                    val cb = onFinalResultCallback
                                    if (cb != null) {
                                        onFinalResultCallback = null
                                        onErrorCallback = null
                                        _rmsDb.value = 0f
                                        cb.invoke(currentPartial)
                                        stopListeningInternal()
                                    }
                                }
                                mainHandler.postDelayed(endOfSpeechSafetyRunnable!!, 500L)
                            }
                        }

                        override fun onError(error: Int) {
                            endOfSpeechSafetyRunnable?.let { mainHandler.removeCallbacks(it) }
                            endOfSpeechSafetyRunnable = null
                            _isListening.value = false
                            _rmsDb.value = 0f
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                                SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, resetting..."
                                SpeechRecognizer.ERROR_SERVER -> "Server speech recognition error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out"
                                else -> "Recognition code: $error"
                            }
                            Log.w(TAG, "SpeechRecognizer error: $message (code $error)")
                            onErrorCallback?.invoke(message)
                        }

                        override fun onResults(results: Bundle?) {
                            endOfSpeechSafetyRunnable?.let { mainHandler.removeCallbacks(it) }
                            endOfSpeechSafetyRunnable = null
                            _isListening.value = false
                            _rmsDb.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: _partialText.value.trim()
                            val cb = onFinalResultCallback
                            onFinalResultCallback = null
                            onErrorCallback = null
                            if (text.isNotBlank()) {
                                _partialText.value = text
                                cb?.invoke(text)
                            } else {
                                onErrorCallback?.invoke("No speech detected")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull().orEmpty()
                            if (text.isNotBlank()) {
                                _partialText.value = text
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // Cut silence detection delays from 2000ms down to 600ms for rapid turnaround
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 450L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                _isListening.value = false
                _rmsDb.value = 0f
                onError("Voice recognizer error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Jarvis...")
        }
    }

    fun updateRms(rms: Float) {
        _rmsDb.value = rms.coerceIn(0f, 10f)
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        endOfSpeechSafetyRunnable?.let { mainHandler.removeCallbacks(it) }
        endOfSpeechSafetyRunnable = null
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    companion object {
        private const val TAG = "SpeechToTextManager"
    }
}
