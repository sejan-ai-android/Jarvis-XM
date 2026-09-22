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

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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

            // Take last 20 messages for context
            val contextMessages = history.takeLast(20)
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
                    temperature = 0.7,
                    maxOutputTokens = 2048
                )
            )

            // Resolve modern model with fallback if 404 / NOT_FOUND
            val normalizedModel = when (modelName) {
                "gemini-1.5-flash", "gemini-flash" -> "gemini-2.5-flash"
                "gemini-1.5-pro", "gemini-pro" -> "gemini-2.5-pro"
                else -> modelName
            }

            val candidatesToTry = listOf(
                normalizedModel,
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-2.5-pro"
            ).distinct()

            var lastError = ""
            var lastCode = 0

            for (currentModel in candidatesToTry) {
                val response = apiService.generateContent(
                    model = currentModel,
                    apiKey = apiKey,
                    request = request
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val candidateText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!candidateText.isNullOrBlank()) {
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

                    // If 404 (Not Found / Model deprecated), proceed to next candidate
                    if (response.code() == 404 || errorBody.contains("NOT_FOUND", ignoreCase = true)) {
                        continue
                    }

                    return@withContext JarvisResult.Error("Neural core error (HTTP ${response.code()}): $errorBody")
                }
            }

            JarvisResult.Error("Neural core error (HTTP $lastCode): $lastError")
        } catch (e: Exception) {
            JarvisResult.Error("Communications relay failure: ${e.localizedMessage ?: "Unknown connection error"}", e)
        }
    }

    private fun buildSystemPrompt(prefs: UserPreferences): String {
        return """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), a sophisticated, ultra-competent, loyal, and proactive AI assistant for Android.
            
            USER PROFILE & PROTOCOL:
            - Address the user respectfully as "${prefs.userName}" or "Sir" unless instructed otherwise.
            - Tone mode: ${prefs.assistantTone}.
            - Output language preference: ${prefs.selectedLanguage}.
            - You have access to real-time tools, system controls, scheduling, notes, and device telemetry.
            - Provide clear, concise, highly structured, and insightful answers. Use bold headings, bullet points, or concise code blocks when appropriate.
            - Avoid robotic clichés. Emulate the classic, polished wit, composed demeanor, and calm resourcefulness of Jarvis.
        """.trimIndent()
    }

    companion object {
        private const val roleUser = "user"
    }
}
