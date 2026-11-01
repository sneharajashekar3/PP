package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ConversationEntity
import com.example.data.MessageEntity
import com.example.data.MessageFeedback
import com.example.data.ModelMetadataEntity
import com.example.engine.HardwareTelemetrySnapshot
import com.example.engine.KvCacheStatus
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.PagePilotComposer
import com.example.ui.components.PagePilotLogo
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.PilotTealDark
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

private val SUGGESTED_PROMPTS = listOf(
    "Verify BPE tokenization & KV-cache pipeline on a 512-token context",
    "Test RoPE positional step and repetition penalty sampling",
    "Inspect RMSNorm & SwiGLU intermediate projection dimensions",
    "Stress-test streaming decode speed and 8 GB RAM headroom"
)

@Composable
fun HomeAndChatWorkspaceScreen(
    activeConversationId: String?,
    messages: List<MessageEntity>,
    recentConversations: List<ConversationEntity>,
    loadedModel: ModelMetadataEntity?,
    availableModels: List<ModelMetadataEntity>,
    kvCacheStatus: KvCacheStatus,
    hardwareSnapshot: HardwareTelemetrySnapshot,
    composerText: String,
    isGenerating: Boolean,
    onComposerTextChange: (String) -> Unit,
    onSendPrompt: (String?) -> Unit,
    onStopGeneration: () -> Unit,
    onEditMessage: (MessageEntity, String) -> Unit,
    onRegenerateMessage: (MessageEntity) -> Unit,
    onFeedbackChange: (MessageEntity, MessageFeedback) -> Unit,
    onSelectConversation: (String) -> Unit,
    onQuickLoadModel: (ModelMetadataEntity) -> Unit,
    onOpenModelManager: () -> Unit,
    onOpenArchitectureSpec: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editedText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Honest Active Model / Pipeline Verification Readiness Banner
        ModelReadinessHeaderStrip(
            loadedModel = loadedModel,
            availableModels = availableModels,
            kvCacheStatus = kvCacheStatus,
            onQuickLoadModel = onQuickLoadModel,
            onOpenModelManager = onOpenModelManager
        )

        Box(modifier = Modifier.weight(1f)) {
            if (activeConversationId == null || messages.isEmpty()) {
                HomeZeroStateContent(
                    loadedModel = loadedModel,
                    availableModels = availableModels,
                    recentConversations = recentConversations.take(4),
                    hardwareSnapshot = hardwareSnapshot,
                    onSelectPrompt = { prompt ->
                        onComposerTextChange(prompt)
                        if (loadedModel != null) {
                            onSendPrompt(prompt)
                        }
                    },
                    onSelectConversation = onSelectConversation,
                    onQuickLoadModel = onQuickLoadModel,
                    onOpenModelManager = onOpenModelManager,
                    onOpenArchitectureSpec = onOpenArchitectureSpec
                )
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onCopyText = { text ->
                                copyToClipboard(context, "PagePilot Message", text)
                                onShowStatusMessage("Copied to device clipboard.")
                            },
                            onEditMessage = { target ->
                                editingMessage = target
                                editedText = target.content
                            },
                            onRegenerateMessage = onRegenerateMessage,
                            onFeedbackChange = onFeedbackChange
                        )
                    }
                }
            }
        }

        PagePilotComposer(
            text = composerText,
            isGenerating = isGenerating,
            hasLoadedModel = loadedModel != null,
            onTextChange = onComposerTextChange,
            onSend = { onSendPrompt(null) },
            onStopGeneration = onStopGeneration,
            onOpenSamplingSettings = onOpenSettings,
            onShowMilestone3AttachmentInfo = {
                onShowStatusMessage(
                    "Milestone 3 Architecture Prepared: Document RAG (PDF/TXT/DOCX/CSV/JSON/MD + BM25) and Offline Voice are scheduled for Milestone 3."
                )
            }
        )
    }

    if (editingMessage != null) {
        val target = editingMessage!!
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit & Regenerate Prompt") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditMessage(target, editedText)
                        editingMessage = null
                    }
                ) {
                    Text("Save & Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModelReadinessHeaderStrip(
    loadedModel: ModelMetadataEntity?,
    availableModels: List<ModelMetadataEntity>,
    kvCacheStatus: KvCacheStatus,
    onQuickLoadModel: (ModelMetadataEntity) -> Unit,
    onOpenModelManager: () -> Unit
) {
    if (loadedModel == null) {
        val preferred = availableModels.firstOrNull { it.isSelected } ?: availableModels.firstOrNull()
        Surface(
            color = StatusAmber.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = StatusAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No model loaded in RAM. PagePilot never fakes AI responses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (preferred != null) {
                    TextButton(
                        onClick = { onQuickLoadModel(preferred) },
                        modifier = Modifier.testTag("banner_quick_load_button")
                    ) {
                        Text("Load ${preferred.parameterLabel}")
                    }
                } else {
                    TextButton(onClick = onOpenModelManager) {
                        Text("Model Manager")
                    }
                }
            }
        }
    } else if (loadedModel.isRandomWeightsVerification) {
        Surface(
            color = PilotTealDark.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = PilotTealDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pipeline Verification Mode: ${loadedModel.configId} (${loadedModel.parameterLabel} random weights) • KV Cache ${kvCacheStatus.allocatedSlots}/${kvCacheStatus.maxContextSlots}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeZeroStateContent(
    loadedModel: ModelMetadataEntity?,
    availableModels: List<ModelMetadataEntity>,
    recentConversations: List<ConversationEntity>,
    hardwareSnapshot: HardwareTelemetrySnapshot,
    onSelectPrompt: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onQuickLoadModel: (ModelMetadataEntity) -> Unit,
    onOpenModelManager: () -> Unit,
    onOpenArchitectureSpec: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 560.dp)
            ) {
                PagePilotLogo(size = 64.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Welcome to PagePilot",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Private, 100% offline Android AI assistant powered by our independent decoder-only Transformer architecture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Active Model & 8 GB RAM Readiness Card
        item {
            val preferredModel = availableModels.firstOrNull { it.isSelected } ?: availableModels.firstOrNull()
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (loadedModel != null) StatusEmerald else StatusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (loadedModel != null) {
                                    "Active Engine: ${loadedModel.name}"
                                } else {
                                    "Engine Standby (Unloaded)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${hardwareSnapshot.availableDeviceRamMb} MB Free RAM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (loadedModel != null) {
                            "Exact Parameters: ${loadedModel.parameterLabel} • Vocab: ${loadedModel.vocabSize} • Context: ${loadedModel.contextLength} tokens • Quant: ${loadedModel.quantization}"
                        } else if (preferredModel != null) {
                            "Ready in App-Private Storage: ${preferredModel.name} (${preferredModel.parameterLabel} params, SHA-256 verified). Load it into RAM to test the LocalModelEngine pipeline."
                        } else {
                            "Import a PagePilot checkpoint via SAF or generate a verification micro-checkpoint in Model Manager."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (loadedModel == null && preferredModel != null) {
                            Button(
                                onClick = { onQuickLoadModel(preferredModel) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("home_load_model_button")
                            ) {
                                Text("Load ${preferredModel.parameterLabel} Checkpoint")
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenModelManager,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("home_open_model_manager_button")
                        ) {
                            Text("Open Model Manager")
                        }
                        TextButton(onClick = onOpenArchitectureSpec) {
                            Text("Parameter Math")
                        }
                    }
                }
            }
        }

        // Suggested Prompts Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Text(
                    text = "SUGGESTED PIPELINE & PROMPT TESTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (prompt in SUGGESTED_PROMPTS) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPrompt(prompt) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Conversations List on Home Screen
        if (recentConversations.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                ) {
                    Text(
                        text = "RECENT LOCAL CONVERSATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (conv in recentConversations) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectConversation(conv.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (conv.lastMessagePreview.isNotBlank()) {
                                            Text(
                                                text = conv.lastMessagePreview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Offline Privacy Guarantee Footer
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = StatusEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero INTERNET permissions • 100% local Room SQLite & SAF storage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
