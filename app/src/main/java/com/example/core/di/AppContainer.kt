package com.example.core.di

import android.content.Context
import com.example.ai.gemini.GeminiRepository
import com.example.data.local.ConversationRepository
import com.example.data.local.JarvisDatabase
import com.example.data.prefs.KeyRepository
import com.example.data.prefs.KeyValidator
import com.example.data.prefs.SecureKeyStore
import com.example.data.prefs.UserPreferencesRepository

/**
 * Clean dependency injection container managing application-wide singletons.
 */
class AppContainer(val context: Context) {

    val secureKeyStore: SecureKeyStore by lazy {
        SecureKeyStore(context)
    }

    val keyValidator: KeyValidator by lazy {
        KeyValidator()
    }

    val keyRepository: KeyRepository by lazy {
        KeyRepository(secureKeyStore, keyValidator)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    private val database: JarvisDatabase by lazy {
        JarvisDatabase.getDatabase(context)
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepository(database.conversationDao())
    }

    val geminiRepository: GeminiRepository by lazy {
        GeminiRepository(secureKeyStore)
    }

    val voiceManager: com.example.voice.JarvisVoiceManager by lazy {
        com.example.voice.JarvisVoiceManager(context, secureKeyStore)
    }
}
