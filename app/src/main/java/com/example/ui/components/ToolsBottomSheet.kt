package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.viewmodel.QuickToolAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsBottomSheet(
    onSelectTool: (QuickToolAction, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nova Quick Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Paste Clipboard Button
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            inputText = clip.getItemAt(0).text?.toString() ?: ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", fontSize = 12.sp)
                }
            }

            Text(
                text = "Apply powerful AI transformations to your text with one tap.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Paste or type text to transform (optional)...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("tools_input_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            val sampleFallback = if (inputText.isNotBlank()) inputText else "the user's previous topic"

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                item {
                    ToolItemRow(
                        title = "Summarize Text",
                        description = "Key points & bulleted executive summary",
                        icon = Icons.AutoMirrored.Filled.ShortText,
                        onClick = { onSelectTool(QuickToolAction.SUMMARIZE, sampleFallback); onDismiss() }
                    )
                }
                item {
                    ToolItemRow(
                        title = "Proofread & Polish",
                        description = "Correct grammar, tone and improve flow",
                        icon = Icons.Default.AutoFixHigh,
                        onClick = { onSelectTool(QuickToolAction.POLISH_GRAMMAR, sampleFallback); onDismiss() }
                    )
                }
                item {
                    ToolItemRow(
                        title = "Translate to Spanish",
                        description = "Fluent and accurate Spanish translation",
                        icon = Icons.Default.Language,
                        onClick = { onSelectTool(QuickToolAction.TRANSLATE_SPANISH, sampleFallback); onDismiss() }
                    )
                }
                item {
                    ToolItemRow(
                        title = "Translate to French",
                        description = "Natural French translation",
                        icon = Icons.Default.Language,
                        onClick = { onSelectTool(QuickToolAction.TRANSLATE_FRENCH, sampleFallback); onDismiss() }
                    )
                }
                item {
                    ToolItemRow(
                        title = "Translate to Japanese",
                        description = "Japanese with pronunciation & nuances",
                        icon = Icons.Default.Language,
                        onClick = { onSelectTool(QuickToolAction.TRANSLATE_JAPANESE, sampleFallback); onDismiss() }
                    )
                }
                item {
                    ToolItemRow(
                        title = "Extract Action Items",
                        description = "Identify tasks, owners and checklists",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        onClick = { onSelectTool(QuickToolAction.EXTRACT_ACTION_ITEMS, sampleFallback); onDismiss() }
                    )
                }
                item {
                    ToolItemRow(
                        title = "Explain Code Line-by-Line",
                        description = "Breakdown logic and suggest fixes",
                        icon = Icons.Default.Code,
                        onClick = { onSelectTool(QuickToolAction.EXPLAIN_CODE, sampleFallback); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolItemRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NovaSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
