package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ConversationRepository(private val dao: ConversationDao) {

    val allConversations: Flow<List<ConversationEntity>> = dao.getAllConversations()

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        dao.getMessagesForConversation(conversationId)

    suspend fun getRecentMessages(conversationId: String, limit: Int = 20): List<MessageEntity> =
        dao.getRecentMessages(conversationId, limit)

    suspend fun getOrCreateDefaultConversation(): ConversationEntity {
        val existing = dao.getConversationById("default_session")
        if (existing != null) return existing

        val defaultConv = ConversationEntity(
            id = "default_session",
            title = "Neural Link Session",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertConversation(defaultConv)
        return defaultConv
    }

    suspend fun createNewConversation(title: String): ConversationEntity {
        val conv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "New Session" },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertConversation(conv)
        return conv
    }

    suspend fun insertMessage(
        conversationId: String,
        sender: String,
        content: String,
        status: String = "sent",
        isVoice: Boolean = false
    ): Long {
        val message = MessageEntity(
            conversationId = conversationId,
            sender = sender,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = status,
            isVoiceMessage = isVoice
        )
        val id = dao.insertMessage(message)
        // Update conversation timestamp
        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            dao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun updateMessageStatus(id: Long, status: String, content: String) {
        dao.updateMessageStatus(id, status, content)
    }

    suspend fun deleteConversation(id: String) {
        dao.deleteConversation(id)
    }

    suspend fun clearAll() {
        dao.clearAllConversations()
    }
}
