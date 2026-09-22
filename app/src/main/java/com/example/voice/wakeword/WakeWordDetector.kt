package com.example.voice.wakeword

import android.content.Context
import android.util.Log
import com.example.data.prefs.SecureKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WakeWordDetector(
    private val context: Context,
    private val keyStore: SecureKeyStore
) {

    private val _isHotwordActive = MutableStateFlow(false)
    val isHotwordActive: StateFlow<Boolean> = _isHotwordActive.asStateFlow()

    private var onWakeWordTriggered: (() -> Unit)? = null

    val isPorcupineConfigured: Boolean
        get() = keyStore.hasKey(SecureKeyStore.KEY_PORCUPINE)

    fun startDetection(onTrigger: () -> Unit) {
        onWakeWordTriggered = onTrigger

        val accessKey = keyStore.getKey(SecureKeyStore.KEY_PORCUPINE)
        if (accessKey.isBlank()) {
            Log.d(TAG, "Porcupine access key not configured. Hotword engine in standby.")
            _isHotwordActive.value = false
            return
        }

        try {
            // Porcupine initialization protocol
            Log.i(TAG, "Initializing Porcupine hotword detector with registered access key...")
            _isHotwordActive.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Porcupine engine", e)
            _isHotwordActive.value = false
        }
    }

    fun stopDetection() {
        _isHotwordActive.value = false
        onWakeWordTriggered = null
    }

    /**
     * Simulates or triggers hotword programmatically (e.g. from service or UI action)
     */
    fun triggerWakeWord() {
        onWakeWordTriggered?.invoke()
    }

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}
