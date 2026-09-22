package com.example.ai.gemini

import com.example.core.model.JarvisResult
import com.example.data.local.MessageEntity
import com.example.data.prefs.SecureKeyStore
import com.example.data.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GeminiRepository(
    private val keyStore: SecureKeyStore
) {

    private val apiService: GeminiApiService

    @Volatile
    private var activeWorkingModel: String? = null

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateAssistantResponse(
        history: List<MessageEntity>,
        userMessage: String,
        preferences: UserPreferences,
        modelName: String = "gemini-2.5-flash"
    ): JarvisResult<String> = withContext(Dispatchers.IO) {
        // Fetch key at call-time from SecureKeyStore
        val apiKey = keyStore.getKey(SecureKeyStore.KEY_GEMINI)
        if (apiKey.isBlank()) {
            return@withContext JarvisResult.Error(
                message = "Gemini API key is missing. Please configure it in Settings.",
                isKeyMissing = true
            )
        }

        try {
            val systemPrompt = buildSystemPrompt(preferences)
            val contents = mutableListOf<Content>()

            // Take last 8 messages for optimal prompt size and lowest latency
            val contextMessages = history.takeLast(8)
            for (msg in contextMessages) {
                val role = if (msg.sender == "user") "user" else "model"
                contents.add(Content(role = role, parts = listOf(Part(text = msg.content))))
            }

            // Append current user message
            contents.add(Content(role = roleUser, parts = listOf(Part(text = userMessage))))

            val request = GenerateContentRequest(
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                contents = contents,
                generationConfig = GenerationConfig(
                    temperature = 0.5,
                    maxOutputTokens = 600
                )
            )

            // Resolve modern model with fallback if 404 / NOT_FOUND / 503 high demand
            val normalizedModel = when (modelName) {
                "gemini-1.5-flash", "gemini-flash" -> "gemini-flash-latest"
                "gemini-1.5-pro", "gemini-pro" -> "gemini-3.1-pro-preview"
                "gemini-2.0-flash", "gemini-2.0-pro" -> "gemini-2.5-flash"
                else -> modelName
            }

            val candidatesToTry = listOfNotNull(
                activeWorkingModel,
                normalizedModel,
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-3.1-flash-lite-preview",
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview"
            ).distinct()

            var lastError = ""
            var lastCode = 0

            for (currentModel in candidatesToTry) {
                var response = apiService.generateContent(
                    model = currentModel,
                    apiKey = apiKey,
                    request = request
                )

                // If transient 503 (high demand) or 429, perform one brief retry with jitter
                if (!response.isSuccessful && (response.code() == 503 || response.code() == 429)) {
                    kotlinx.coroutines.delay(400)
                    response = apiService.generateContent(
                        model = currentModel,
                        apiKey = apiKey,
                        request = request
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val candidateText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!candidateText.isNullOrBlank()) {
                        activeWorkingModel = currentModel
                        return@withContext JarvisResult.Success(candidateText.trim())
                    } else {
                        return@withContext JarvisResult.Error("Jarvis received an empty transmission from the neural network.")
                    }
                } else {
                    lastCode = response.code()
                    val errorBody = response.errorBody()?.string().orEmpty()
                    lastError = errorBody

                    if (response.code() == 400 || response.code() == 403 || errorBody.contains("API_KEY_INVALID", ignoreCase = true)) {
                        return@withContext JarvisResult.Error(
                            message = "Invalid or expired Gemini API key. Please check your key in Settings.",
                            isKeyMissing = true
                        )
                    }

                    // If 404 (Not Found), 503 (High Demand / UNAVAILABLE), 429 (Rate Limit), or 500,
                    // smoothly failover to the next candidate model in the chain
                    val isTransientOrModelSpecific = response.code() == 404 ||
                            response.code() == 503 ||
                            response.code() == 429 ||
                            response.code() == 500 ||
                            response.code() == 504 ||
                            errorBody.contains("NOT_FOUND", ignoreCase = true) ||
                            errorBody.contains("UNAVAILABLE", ignoreCase = true) ||
                            errorBody.contains("high demand", ignoreCase = true) ||
                            errorBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true)

                    if (isTransientOrModelSpecific) {
                        continue
                    }

                    return@withContext JarvisResult.Error(formatErrorMessage(response.code(), errorBody))
                }
            }

            JarvisResult.Error(formatErrorMessage(lastCode, lastError))
        } catch (e: Exception) {
            JarvisResult.Error("Communications relay failure: ${e.localizedMessage ?: "Unknown connection error"}", e)
        }
    }

    private fun formatErrorMessage(code: Int, rawError: String): String {
        return when {
            code == 503 || rawError.contains("high demand", ignoreCase = true) || rawError.contains("UNAVAILABLE", ignoreCase = true) ->
                "Gemini neural models are currently experiencing heavy global traffic. Failover relays attempted. Please retry your request in a moment."
            code == 429 || rawError.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                "Gemini API rate limit reached. Please wait a few seconds before sending another transmission."
            code == 404 || rawError.contains("NOT_FOUND", ignoreCase = true) ->
                "Specified neural model was not found on the server cluster."
            rawError.isNotBlank() ->
                "Neural core error (HTTP $code): ${extractCleanErrorMessage(rawError)}"
            else ->
                "Neural core communication failed (HTTP $code)."
        }
    }

    private fun extractCleanErrorMessage(rawJson: String): String {
        val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
        val match = messageRegex.find(rawJson)
        return match?.groupValues?.getOrNull(1) ?: rawJson.take(150)
    }

    private fun buildSystemPrompt(prefs: UserPreferences): String {
        return """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), a sophisticated, ultra-competent, loyal, and proactive AI assistant for Android.
            
            USER PROFILE & PROTOCOL:
            - Address the user respectfully as "${prefs.userName}" or "Sir" unless instructed otherwise.
            - Tone mode: ${prefs.assistantTone}.
            - Output language preference: ${prefs.selectedLanguage}.
            - You have access to real-time tools, system controls, scheduling, notes, and device telemetry.
            - SPEED & BREVITY MANDATE (CRITICAL):
              * For standard queries and conversational voice turns, respond immediately and concisely in 1 to 3 crisp sentences (max 40-50 words).
              * Only provide comprehensive multi-paragraph explanations or code blocks when explicitly requested by the user.
              * Avoid introductory filler phrases like "Certainly, here is...", "As an AI...", or "I would be happy to help you with that". Go straight to the answer.
              * Emulate the classic, polished wit, composed demeanor, and calm efficiency of Jarvis.
        """.trimIndent()
    }

    companion object {
        private const val roleUser = "user"
    }
}
