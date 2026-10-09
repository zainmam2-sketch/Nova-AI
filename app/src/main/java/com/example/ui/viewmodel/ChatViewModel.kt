package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechManager
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.model.Persona
import com.example.data.model.PredefinedPersonas
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)
    val speechManager = SpeechManager(application)

    private val prefs = application.getSharedPreferences("nova_ai_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    init {
        // Load saved preferences
        val savedKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        val savedTemp = prefs.getFloat("gemini_temperature", 0.7f)
        _uiState.update { it.copy(customApiKey = savedKey, temperature = savedTemp) }

        // Observe TTS state
        viewModelScope.launch {
            speechManager.currentlySpeakingMessageId.collectLatest { speakingId ->
                _uiState.update { it.copy(currentlySpeakingMessageId = speakingId) }
            }
        }

        // Observe STT state
        viewModelScope.launch {
            speechManager.isListening.collectLatest { isListening ->
                _uiState.update { it.copy(isListeningVoice = isListening) }
            }
        }

        // Observe all sessions
        viewModelScope.launch {
            repository.allSessions.collectLatest { sessionList ->
                _uiState.update { it.copy(sessions = sessionList) }
                if (_uiState.value.currentSessionId == null) {
                    if (sessionList.isNotEmpty()) {
                        selectSession(sessionList.first())
                    } else {
                        startNewChat()
                    }
                } else {
                    // Update current title if changed
                    sessionList.find { it.id == _uiState.value.currentSessionId }?.let { cur ->
                        _uiState.update {
                            it.copy(
                                currentSessionTitle = cur.title,
                                selectedPersona = PredefinedPersonas.getById(cur.personaId)
                            )
                        }
                    }
                }
            }
        }
    }

    fun startNewChat(personaId: String? = null) {
        val targetPersonaId = personaId ?: _uiState.value.selectedPersona.id
        viewModelScope.launch {
            val persona = PredefinedPersonas.getById(targetPersonaId)
            val newSession = repository.createNewSession(
                personaId = targetPersonaId,
                initialTitle = "New Chat"
            )
            _uiState.update {
                it.copy(
                    currentSessionId = newSession.id,
                    currentSessionTitle = newSession.title,
                    selectedPersona = persona,
                    attachedImageUri = null,
                    attachedImageBase64 = null
                )
            }
            observeMessages(newSession.id)
        }
    }

    fun selectSession(session: ChatSessionEntity) {
        if (_uiState.value.currentSessionId == session.id) return
        speechManager.stopSpeaking()
        val persona = PredefinedPersonas.getById(session.personaId)
        _uiState.update {
            it.copy(
                currentSessionId = session.id,
                currentSessionTitle = session.title,
                selectedPersona = persona,
                attachedImageUri = null,
                attachedImageBase64 = null
            )
        }
        observeMessages(session.id)
    }

    private fun observeMessages(sessionId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collectLatest { msgs ->
                _uiState.update {
                    it.copy(
                        messages = msgs,
                        isGenerating = msgs.any { m -> m.status == "sending" }
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val hasImage = _uiState.value.attachedImageBase64 != null
        if (trimmed.isEmpty() && !hasImage) return

        val sessionId = _uiState.value.currentSessionId ?: return
        val imageBase64 = _uiState.value.attachedImageBase64
        val imageUriString = _uiState.value.attachedImageUri?.toString()
        val customKey = _uiState.value.customApiKey
        val temp = _uiState.value.temperature

        // Clear attached image for next message
        _uiState.update {
            it.copy(
                attachedImageUri = null,
                attachedImageBase64 = null,
                isGenerating = true
            )
        }

        viewModelScope.launch {
            repository.sendMessage(
                sessionId = sessionId,
                userPrompt = trimmed,
                imageBase64 = imageBase64,
                imageMimeType = "image/jpeg",
                imageUriString = imageUriString,
                customApiKey = customKey,
                customTemperature = temp
            )
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(attachedImageUri = null, attachedImageBase64 = null) }
            return
        }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Resize bitmap if larger than 1024px to ensure snappy upload
                    val maxDimension = 1024
                    val scaledBitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val targetWidth: Int
                        val targetHeight: Int
                        if (ratio > 1) {
                            targetWidth = maxDimension
                            targetHeight = (maxDimension / ratio).toInt()
                        } else {
                            targetHeight = maxDimension
                            targetWidth = (maxDimension * ratio).toInt()
                        }
                        Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                    } else {
                        originalBitmap
                    }

                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                    _uiState.update {
                        it.copy(
                            attachedImageUri = uri,
                            attachedImageBase64 = base64
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(attachedImageUri = null, attachedImageBase64 = null) }
            }
        }
    }

    fun removeAttachedImage() {
        _uiState.update { it.copy(attachedImageUri = null, attachedImageBase64 = null) }
    }

    fun toggleVoiceListening(onTextAppended: (String) -> Unit) {
        if (_uiState.value.isListeningVoice) {
            speechManager.stopListening()
        } else {
            speechManager.startListening { recognizedText ->
                onTextAppended(recognizedText)
            }
        }
    }

    fun toggleTts(message: ChatMessageEntity) {
        speechManager.speak(message.id, message.content)
    }

    fun setPersona(persona: Persona) {
        val sessionId = _uiState.value.currentSessionId
        _uiState.update { it.copy(selectedPersona = persona, isPersonaDialogOpen = false) }
        if (sessionId != null) {
            viewModelScope.launch {
                // Update session's persona in DB
                val session = repository.getSession(sessionId)
                if (session != null) {
                    repository.updateSessionTitle(sessionId, session.title) // update timestamp
                }
            }
        }
    }

    fun deleteSession(session: ChatSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session.id)
            if (_uiState.value.currentSessionId == session.id) {
                val remaining = _uiState.value.sessions.filter { it.id != session.id }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first())
                } else {
                    startNewChat()
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            startNewChat()
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun updateCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _uiState.update { it.copy(customApiKey = key.trim()) }
    }

    fun updateTemperature(temp: Float) {
        prefs.edit().putFloat("gemini_temperature", temp).apply()
        _uiState.update { it.copy(temperature = temp) }
    }

    fun openSettingsDialog(open: Boolean) {
        _uiState.update { it.copy(isSettingsDialogOpen = open) }
    }

    fun openPersonaDialog(open: Boolean) {
        _uiState.update { it.copy(isPersonaDialogOpen = open) }
    }

    fun openToolsMenu(open: Boolean) {
        _uiState.update { it.copy(isToolsMenuOpen = open) }
    }

    fun runQuickTool(toolAction: QuickToolAction, inputPrompt: String, onTextFilled: (String) -> Unit) {
        val prompt = when (toolAction) {
            QuickToolAction.SUMMARIZE -> "Please provide a concise, structured bulleted summary of the following text:\n\n$inputPrompt"
            QuickToolAction.TRANSLATE_SPANISH -> "Translate the following text into fluent, natural Spanish:\n\n$inputPrompt"
            QuickToolAction.TRANSLATE_FRENCH -> "Translate the following text into fluent, natural French:\n\n$inputPrompt"
            QuickToolAction.TRANSLATE_JAPANESE -> "Translate the following text into natural Japanese (with romaji & English explanation):\n\n$inputPrompt"
            QuickToolAction.POLISH_GRAMMAR -> "Proofread, correct any grammatical errors, and polish the tone of this text for clarity and impact:\n\n$inputPrompt"
            QuickToolAction.EXTRACT_ACTION_ITEMS -> "Extract all concrete action items, responsibilities, and deadlines from this text:\n\n$inputPrompt"
            QuickToolAction.EXPLAIN_CODE -> "Analyze and explain this code line-by-line, highlighting potential bugs or performance improvements:\n\n$inputPrompt"
        }
        onTextFilled(prompt)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}

enum class QuickToolAction {
    SUMMARIZE,
    TRANSLATE_SPANISH,
    TRANSLATE_FRENCH,
    TRANSLATE_JAPANESE,
    POLISH_GRAMMAR,
    EXTRACT_ACTION_ITEMS,
    EXPLAIN_CODE
}
