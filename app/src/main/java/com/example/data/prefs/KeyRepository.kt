package com.example.data.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ApiKeyConfig(
    val id: String,
    val title: String,
    val description: String,
    val isRequired: Boolean,
    val freeTierInfo: String,
    val providerUrl: String,
    val currentValue: String,
    val status: KeyStatus = KeyStatus.NOT_SET,
    val statusMessage: String = ""
)

/**
 * Domain-facing repository for managing user runtime keys securely.
 */
class KeyRepository(
    private val keyStore: SecureKeyStore,
    private val validator: KeyValidator = KeyValidator()
) {

    private val _keysState = MutableStateFlow<List<ApiKeyConfig>>(emptyList())
    val keysState: StateFlow<List<ApiKeyConfig>> = _keysState.asStateFlow()

    init {
        loadAllKeys()
    }

    fun loadAllKeys() {
        val configs = listOf(
            ApiKeyConfig(
                id = SecureKeyStore.KEY_GEMINI,
                title = "Gemini API Key",
                description = "Powers Jarvis intelligence, text conversations, and reasoning engine.",
                isRequired = true,
                freeTierInfo = "Free tier available (15 RPM / 1M TPM on Flash models)",
                providerUrl = "https://aistudio.google.com/",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_GEMINI),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_GEMINI)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_PORCUPINE,
                title = "Porcupine Access Key",
                description = "Enables hands-free 'Hey Jarvis' hotword detection via Picovoice.",
                isRequired = true,
                freeTierInfo = "Free Developer Tier (up to 3 active devices)",
                providerUrl = "https://console.picovoice.ai/",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_PORCUPINE),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_PORCUPINE)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_OPENWEATHER,
                title = "OpenWeather API Key",
                description = "Retrieves live weather data, radar info, and local atmospheric telemetry.",
                isRequired = true,
                freeTierInfo = "Free tier: 1,000 API calls/day",
                providerUrl = "https://home.openweathermap.org/api_keys",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_OPENWEATHER),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_OPENWEATHER)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_SPOTIFY_CLIENT_ID,
                title = "Spotify Client ID",
                description = "For music playback automation and smart playlist query controls.",
                isRequired = false,
                freeTierInfo = "Free Developer App access",
                providerUrl = "https://developer.spotify.com/dashboard",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_SPOTIFY_CLIENT_ID),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_SPOTIFY_CLIENT_ID)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_SPOTIFY_CLIENT_SECRET,
                title = "Spotify Client Secret",
                description = "Paired with Client ID for OAuth token generation.",
                isRequired = false,
                freeTierInfo = "Free Developer App access",
                providerUrl = "https://developer.spotify.com/dashboard",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_SPOTIFY_CLIENT_SECRET),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_SPOTIFY_CLIENT_SECRET)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_ELEVENLABS,
                title = "ElevenLabs API Key",
                description = "Optional ultra-realistic AI voice synthesis for Jarvis responses.",
                isRequired = false,
                freeTierInfo = "Free tier: 10,000 characters/month",
                providerUrl = "https://elevenlabs.io/app/settings/api-keys",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_ELEVENLABS),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_ELEVENLABS)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_GOOGLE_OAUTH_CLIENT_ID,
                title = "Google OAuth Client ID",
                description = "Enables Google Calendar scheduling and Gmail reading/sending tools.",
                isRequired = false,
                freeTierInfo = "Free Cloud Console integration",
                providerUrl = "https://console.cloud.google.com/",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_GOOGLE_OAUTH_CLIENT_ID),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_GOOGLE_OAUTH_CLIENT_ID)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_SERPAPI,
                title = "SerpAPI Key",
                description = "Enables live web searching and current news grounding queries.",
                isRequired = false,
                freeTierInfo = "Free tier: 250 searches",
                providerUrl = "https://serpapi.com/manage-api-key",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_SERPAPI),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_SERPAPI)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_IFTTT_WEBHOOK,
                title = "IFTTT Webhook Key",
                description = "Triggers IoT routines, smart home automations, and external webhooks.",
                isRequired = false,
                freeTierInfo = "Free tier with unlimited webhook triggers",
                providerUrl = "https://ifttt.com/maker_webhooks/settings",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_IFTTT_WEBHOOK),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_IFTTT_WEBHOOK)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            ),
            ApiKeyConfig(
                id = SecureKeyStore.KEY_CUSTOM_ENDPOINT,
                title = "Custom Endpoint URL",
                description = "Self-hosted AI server or custom proxy endpoint for local LLM routing.",
                isRequired = false,
                freeTierInfo = "Self-hosted / Private server",
                providerUrl = "https://github.com",
                currentValue = keyStore.getKey(SecureKeyStore.KEY_CUSTOM_ENDPOINT),
                status = if (keyStore.hasKey(SecureKeyStore.KEY_CUSTOM_ENDPOINT)) KeyStatus.UNTESTED else KeyStatus.NOT_SET
            )
        )
        _keysState.value = configs
    }

    fun getKey(keyId: String): String = keyStore.getKey(keyId)

    fun hasKey(keyId: String): Boolean = keyStore.hasKey(keyId)

    fun saveKey(keyId: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            keyStore.removeKey(keyId)
        } else {
            keyStore.setKey(keyId, trimmed)
        }
        updateKeyItem(keyId) { current ->
            current.copy(
                currentValue = trimmed,
                status = if (trimmed.isEmpty()) KeyStatus.NOT_SET else KeyStatus.UNTESTED,
                statusMessage = if (trimmed.isEmpty()) "Key removed" else "Saved (Not tested yet)"
            )
        }
    }

    suspend fun testKey(keyId: String, valueToTest: String? = null): KeyValidationResult {
        val value = valueToTest ?: keyStore.getKey(keyId)

        updateKeyItem(keyId) { current ->
            current.copy(status = KeyStatus.TESTING, statusMessage = "Testing connection...")
        }

        val result = validator.validateKey(keyId, value)

        updateKeyItem(keyId) { current ->
            current.copy(
                status = result.status,
                statusMessage = result.message
            )
        }

        return result
    }

    fun clearAllKeys() {
        keyStore.clearAllKeys()
        loadAllKeys()
    }

    private fun updateKeyItem(keyId: String, transform: (ApiKeyConfig) -> ApiKeyConfig) {
        _keysState.value = _keysState.value.map { item ->
            if (item.id == keyId) transform(item) else item
        }
    }
}
