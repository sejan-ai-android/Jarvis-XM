package com.example.data.prefs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class KeyStatus {
    NOT_SET,
    UNTESTED,
    TESTING,
    VALID,
    INVALID
}

data class KeyValidationResult(
    val status: KeyStatus,
    val message: String
)

/**
 * Validates API keys live against provider endpoints or format rules.
 */
class KeyValidator(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {

    suspend fun validateKey(keyId: String, value: String): KeyValidationResult = withContext(Dispatchers.IO) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return@withContext KeyValidationResult(KeyStatus.NOT_SET, "Key is empty")
        }

        when (keyId) {
            SecureKeyStore.KEY_GEMINI -> validateGeminiKey(trimmed)
            SecureKeyStore.KEY_PORCUPINE -> validatePorcupineKey(trimmed)
            SecureKeyStore.KEY_OPENWEATHER -> validateOpenWeatherKey(trimmed)
            SecureKeyStore.KEY_SPOTIFY_CLIENT_ID -> validateGenericLength(trimmed, 20, "Spotify Client ID")
            SecureKeyStore.KEY_SPOTIFY_CLIENT_SECRET -> validateGenericLength(trimmed, 20, "Spotify Client Secret")
            SecureKeyStore.KEY_ELEVENLABS -> validateGenericLength(trimmed, 20, "ElevenLabs Key")
            SecureKeyStore.KEY_GOOGLE_OAUTH_CLIENT_ID -> validateOAuthId(trimmed)
            SecureKeyStore.KEY_SERPAPI -> validateGenericLength(trimmed, 16, "SerpAPI Key")
            SecureKeyStore.KEY_IFTTT_WEBHOOK -> validateGenericLength(trimmed, 10, "IFTTT Webhook Key")
            SecureKeyStore.KEY_CUSTOM_ENDPOINT -> validateUrl(trimmed)
            else -> KeyValidationResult(KeyStatus.VALID, "Saved")
        }
    }

    private fun validateGeminiKey(key: String): KeyValidationResult {
        if (!key.startsWith("AIza") && key.length < 25) {
            return KeyValidationResult(KeyStatus.INVALID, "Invalid Gemini key format (typically starts with 'AIza')")
        }
        return try {
            // Live validation request to Google Gemini API
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                KeyValidationResult(KeyStatus.VALID, "Connection verified with Google AI Studio")
            } else {
                KeyValidationResult(KeyStatus.INVALID, "HTTP ${response.code}: Verification failed")
            }
        } catch (e: Exception) {
            // If offline or network error, validate format
            if (key.startsWith("AIza") && key.length >= 30) {
                KeyValidationResult(KeyStatus.VALID, "Format recognized (Offline check)")
            } else {
                KeyValidationResult(KeyStatus.INVALID, "Network error during validation: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun validatePorcupineKey(key: String): KeyValidationResult {
        // Picovoice keys are base64-like strings ending with "==" or 50+ chars
        if (key.length >= 30) {
            return KeyValidationResult(KeyStatus.VALID, "Porcupine access key format valid")
        }
        return KeyValidationResult(KeyStatus.INVALID, "Porcupine access key must be at least 30 characters")
    }

    private fun validateOpenWeatherKey(key: String): KeyValidationResult {
        if (key.length != 32) {
            return KeyValidationResult(KeyStatus.INVALID, "OpenWeather key should be 32 hexadecimal characters")
        }
        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather?q=London&appid=$key"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 200) {
                KeyValidationResult(KeyStatus.VALID, "OpenWeather connection verified")
            } else if (response.code == 401) {
                KeyValidationResult(KeyStatus.INVALID, "Unauthorized: Key not active yet or invalid")
            } else {
                KeyValidationResult(KeyStatus.VALID, "Key format valid")
            }
        } catch (e: Exception) {
            KeyValidationResult(KeyStatus.VALID, "Key format valid (offline check)")
        }
    }

    private fun validateGenericLength(key: String, minLength: Int, name: String): KeyValidationResult {
        return if (key.length >= minLength) {
            KeyValidationResult(KeyStatus.VALID, "$name format looks valid")
        } else {
            KeyValidationResult(KeyStatus.INVALID, "$name is too short (min $minLength characters)")
        }
    }

    private fun validateOAuthId(key: String): KeyValidationResult {
        return if (key.contains(".apps.googleusercontent.com")) {
            KeyValidationResult(KeyStatus.VALID, "Google OAuth Client ID recognized")
        } else if (key.length > 20) {
            KeyValidationResult(KeyStatus.VALID, "OAuth Client ID format saved")
        } else {
            KeyValidationResult(KeyStatus.INVALID, "Expected .apps.googleusercontent.com client ID")
        }
    }

    private fun validateUrl(key: String): KeyValidationResult {
        return if (key.startsWith("http://") || key.startsWith("https://")) {
            KeyValidationResult(KeyStatus.VALID, "Endpoint URL format valid")
        } else {
            KeyValidationResult(KeyStatus.INVALID, "URL must begin with http:// or https://")
        }
    }
}
