package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.model.PredefinedPersonas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chatDao = db.chatDao()

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getSession(sessionId: String): ChatSessionEntity? {
        return withContext(Dispatchers.IO) {
            chatDao.getSessionById(sessionId)
        }
    }

    suspend fun createNewSession(personaId: String = "nova_core", initialTitle: String = "New Chat"): ChatSessionEntity {
        return withContext(Dispatchers.IO) {
            val session = ChatSessionEntity(
                id = UUID.randomUUID().toString(),
                title = initialTitle,
                personaId = personaId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            chatDao.insertSession(session)
            session
        }
    }

    suspend fun updateSessionTitle(sessionId: String, newTitle: String) {
        withContext(Dispatchers.IO) {
            chatDao.updateSessionTitle(sessionId, newTitle, System.currentTimeMillis())
        }
    }

    suspend fun deleteSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            chatDao.deleteSessionById(sessionId)
        }
    }

    suspend fun clearAllHistory() {
        withContext(Dispatchers.IO) {
            chatDao.clearAllSessions()
        }
    }

    suspend fun deleteMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            chatDao.deleteMessageById(messageId)
        }
    }

    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        imageBase64: String? = null,
        imageMimeType: String = "image/jpeg",
        imageUriString: String? = null,
        customApiKey: String? = null,
        customTemperature: Float? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        val persona = PredefinedPersonas.getById(session?.personaId ?: "nova_core")

        // 1. Insert user message into DB
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessageEntity(
            id = userMsgId,
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            imageUri = imageUriString,
            timestamp = System.currentTimeMillis(),
            status = "sent"
        )
        chatDao.insertMessage(userMessage)

        // 2. Insert placeholder assistant message
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = ChatMessageEntity(
            id = assistantMsgId,
            sessionId = sessionId,
            role = "model",
            content = "...",
            timestamp = System.currentTimeMillis() + 1,
            status = "sending"
        )
        chatDao.insertMessage(placeholderAssistant)
        chatDao.touchSession(sessionId, System.currentTimeMillis())

        // 3. Resolve API Key
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }

        if (apiKey.isEmpty()) {
            val missingKeyMsg = "⚠️ Gemini API key is missing or not configured.\n\n" +
                "To connect Nova AI with Google Gemini:\n" +
                "1. Add your key to the Secrets panel in AI Studio (`GEMINI_API_KEY`), OR\n" +
                "2. Tap the Settings icon in the top right and paste your API key directly."
            chatDao.updateMessage(
                placeholderAssistant.copy(
                    content = missingKeyMsg,
                    status = "error"
                )
            )
            return@withContext Result.failure(Exception("Missing API key"))
        }

        // 4. Build Conversation History
        try {
            val existingMessages = chatDao.getMessagesListForSession(sessionId)
            val contentsList = mutableListOf<Content>()

            // Add previous turns (exclude current pending assistant msg, limit to last 16 turns for token sanity)
            val previousHistory = existingMessages
                .filter { it.id != assistantMsgId && it.id != userMsgId && it.status == "sent" }
                .takeLast(16)

            for (msg in previousHistory) {
                contentsList.add(
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.content))
                    )
                )
            }

            // Current user turn
            val currentTurnParts = mutableListOf<Part>()
            if (!userPrompt.isBlank()) {
                currentTurnParts.add(Part(text = userPrompt))
            }
            if (!imageBase64.isNullOrEmpty()) {
                currentTurnParts.add(
                    Part(
                        inlineData = InlineData(
                            mimeType = imageMimeType,
                            data = imageBase64
                        )
                    )
                )
            }
            contentsList.add(
                Content(
                    role = "user",
                    parts = currentTurnParts
                )
            )

            // System instruction from persona
            val systemInstruction = Content(
                parts = listOf(Part(text = persona.systemInstruction))
            )

            val request = GenerateContentRequest(
                contents = contentsList,
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfig(
                    temperature = customTemperature ?: persona.defaultTemperature
                )
            )

            // 5. Call API
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val responseText = candidate?.content?.parts?.firstOrNull()?.text

            if (!responseText.isNullOrBlank()) {
                chatDao.updateMessage(
                    placeholderAssistant.copy(
                        content = responseText,
                        status = "sent"
                    )
                )

                // If session is still titled "New Chat", generate a short title from user prompt
                if (session?.title == "New Chat") {
                    val smartTitle = if (userPrompt.length > 28) {
                        userPrompt.take(25) + "..."
                    } else if (userPrompt.isNotBlank()) {
                        userPrompt
                    } else {
                        "Image Inquiry"
                    }
                    chatDao.updateSessionTitle(sessionId, smartTitle, System.currentTimeMillis())
                }

                Result.success(responseText)
            } else {
                val blockReason = response.promptFeedback?.blockReason ?: "Empty response"
                val errorMsg = "Unable to generate response ($blockReason)."
                chatDao.updateMessage(
                    placeholderAssistant.copy(
                        content = errorMsg,
                        status = "error"
                    )
                )
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Connection error: ${e.localizedMessage ?: "Unknown error"}.\n\nPlease check your internet connection or verify your Gemini API key."
            chatDao.updateMessage(
                placeholderAssistant.copy(
                    content = errorMsg,
                    status = "error"
                )
            )
            Result.failure(e)
        }
    }
}
