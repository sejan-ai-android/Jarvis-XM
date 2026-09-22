package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure on-device key-value store wrapping EncryptedSharedPreferences.
 * Stores runtime API keys securely without persisting them in plain text or BuildConfig.
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val TAG = "SecureKeyStore"
        private const val PREFS_FILE = "jarvis_secure_vault"

        // Canonical Key Identifiers
        const val KEY_GEMINI = "gemini_api_key"
        const val KEY_PORCUPINE = "porcupine_access_key"
        const val KEY_OPENWEATHER = "openweather_api_key"
        const val KEY_SPOTIFY_CLIENT_ID = "spotify_client_id"
        const val KEY_SPOTIFY_CLIENT_SECRET = "spotify_client_secret"
        const val KEY_ELEVENLABS = "elevenlabs_api_key"
        const val KEY_GOOGLE_OAUTH_CLIENT_ID = "google_oauth_client_id"
        const val KEY_SERPAPI = "serpapi_key"
        const val KEY_IFTTT_WEBHOOK = "ifttt_webhook_key"
        const val KEY_CUSTOM_ENDPOINT = "custom_endpoint"

        val ALL_KEY_IDS = listOf(
            KEY_GEMINI,
            KEY_PORCUPINE,
            KEY_OPENWEATHER,
            KEY_SPOTIFY_CLIENT_ID,
            KEY_SPOTIFY_CLIENT_SECRET,
            KEY_ELEVENLABS,
            KEY_GOOGLE_OAUTH_CLIENT_ID,
            KEY_SERPAPI,
            KEY_IFTTT_WEBHOOK,
            KEY_CUSTOM_ENDPOINT
        )
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences initialization failed, falling back to private prefs", e)
            context.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getKey(key: String): String {
        return prefs.getString(key, "") ?: ""
    }

    fun setKey(key: String, value: String) {
        prefs.edit().putString(key, value.trim()).apply()
    }

    fun removeKey(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun hasKey(key: String): Boolean {
        return getKey(key).isNotBlank()
    }

    fun clearAllKeys() {
        prefs.edit().clear().apply()
    }
}
