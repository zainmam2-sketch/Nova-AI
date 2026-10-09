package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.HistoryDrawerContent
import com.example.ui.components.PersonaSelectorDialog
import com.example.ui.components.QuickPromptsRow
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ToolsBottomSheet
import com.example.ui.components.getPersonaIcon
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }

    // Auto scroll to bottom when messages update
    LaunchedEffect(uiState.messages.size, uiState.isGenerating) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Handle back button to close drawer if open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                HistoryDrawerContent(
                    sessions = uiState.sessions,
                    currentSessionId = uiState.currentSessionId,
                    selectedPersona = uiState.selectedPersona,
                    onSelectSession = { session ->
                        viewModel.selectSession(session)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { session ->
                        viewModel.deleteSession(session)
                    },
                    onClearAllHistory = {
                        viewModel.clearAllHistory()
                        scope.launch { drawerState.close() }
                    },
                    onOpenSettings = {
                        viewModel.openSettingsDialog(true)
                        scope.launch { drawerState.close() }
                    },
                    onOpenPersonaDialog = {
                        viewModel.openPersonaDialog(true)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.openPersonaDialog(true) }
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = uiState.currentSessionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = NovaPrimary.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NovaPrimary.copy(alpha = 0.4f)),
                                        modifier = Modifier.testTag("persona_badge_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = getPersonaIcon(uiState.selectedPersona.iconType),
                                                contentDescription = null,
                                                tint = NovaSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = uiState.selectedPersona.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NovaSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Gemini 3.5 Flash",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // New Chat Button
                        IconButton(
                            onClick = { viewModel.startNewChat() },
                            modifier = Modifier.testTag("top_new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { viewModel.openSettingsDialog(true) },
                            modifier = Modifier.testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                ChatInputBar(
                    text = inputText,
                    onTextChanged = { inputText = it },
                    onSend = {
                        val textToSend = inputText
                        inputText = ""
                        viewModel.sendMessage(textToSend)
                    },
                    attachedImageUri = uiState.attachedImageUri,
                    onImageSelected = { viewModel.onImageSelected(it) },
                    onRemoveImage = { viewModel.removeAttachedImage() },
                    isGenerating = uiState.isGenerating,
                    isListeningVoice = uiState.isListeningVoice,
                    onToggleVoice = { onResult ->
                        viewModel.toggleVoiceListening(onResult)
                    },
                    onOpenToolsMenu = { viewModel.openToolsMenu(true) }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (uiState.messages.isEmpty()) {
                    // Empty Chat State with Hero Banner and Prompts
                    EmptyChatGreeting(
                        personaName = uiState.selectedPersona.name,
                        personaTagline = uiState.selectedPersona.tagline,
                        prompts = uiState.selectedPersona.suggestedPrompts,
                        onPromptClick = { prompt ->
                            inputText = prompt
                            viewModel.sendMessage(prompt)
                            inputText = ""
                        }
                    )
                } else {
                    // Chat Message List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatMessageItem(
                                message = message,
                                isSpeaking = uiState.currentlySpeakingMessageId == message.id,
                                onToggleTts = { viewModel.toggleTts(it) },
                                onDeleteMessage = { viewModel.deleteMessage(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (uiState.isSettingsDialogOpen) {
        SettingsDialog(
            customApiKey = uiState.customApiKey,
            temperature = uiState.temperature,
            onSaveApiKey = { viewModel.updateCustomApiKey(it) },
            onSaveTemperature = { viewModel.updateTemperature(it) },
            onDismiss = { viewModel.openSettingsDialog(false) }
        )
    }

    // Persona Selector Dialog
    if (uiState.isPersonaDialogOpen) {
        PersonaSelectorDialog(
            selectedPersona = uiState.selectedPersona,
            onSelectPersona = { viewModel.setPersona(it) },
            onDismiss = { viewModel.openPersonaDialog(false) }
        )
    }

    // Tools Bottom Sheet
    if (uiState.isToolsMenuOpen) {
        ToolsBottomSheet(
            onSelectTool = { toolAction, paramText ->
                viewModel.runQuickTool(toolAction, paramText) { filledText ->
                    inputText = filledText
                }
            },
            onDismiss = { viewModel.openToolsMenu(false) }
        )
    }
}

@Composable
fun EmptyChatGreeting(
    personaName: String,
    personaTagline: String,
    prompts: List<String>,
    onPromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Image
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(170.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NovaPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.nova_assistant_hero),
                contentDescription = "Nova Assistant",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC0B0F19))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to $personaName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = personaTagline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        QuickPromptsRow(
            prompts = prompts,
            onPromptClick = onPromptClick
        )
    }
}
