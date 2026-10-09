package com.example.ui.viewmodel

import android.net.Uri
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.model.Persona
import com.example.data.model.PredefinedPersonas

data class ChatUiState(
    val sessions: List<ChatSessionEntity> = emptyList(),
    val currentSessionId: String? = null,
    val currentSessionTitle: String = "Nova AI",
    val messages: List<ChatMessageEntity> = emptyList(),
    val selectedPersona: Persona = PredefinedPersonas.ALL.first(),
    val isGenerating: Boolean = false,
    val customApiKey: String = "",
    val temperature: Float = 0.7f,
    val attachedImageUri: Uri? = null,
    val attachedImageBase64: String? = null,
    val isListeningVoice: Boolean = false,
    val currentlySpeakingMessageId: String? = null,
    val isSettingsDialogOpen: Boolean = false,
    val isPersonaDialogOpen: Boolean = false,
    val isToolsMenuOpen: Boolean = false,
    val infoBannerMessage: String? = null
)
