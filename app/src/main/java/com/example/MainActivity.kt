package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.PagePilotViewModel
import com.example.ui.components.PagePilotNavigationDrawerContent
import com.example.ui.components.PagePilotTopBar
import com.example.ui.components.copyToClipboard
import com.example.ui.screens.AboutAndMilestonesScreen
import com.example.ui.screens.ArchitectureAndMathSpecScreen
import com.example.ui.screens.HomeAndChatWorkspaceScreen
import com.example.ui.screens.ModelManagerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PagePilotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PagePilotViewModel = viewModel()
            PagePilotAppRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun PagePilotAppRoot(viewModel: PagePilotViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val loadedModel by viewModel.loadedModel.collectAsStateWithLifecycle()
    val kvCacheStatus by viewModel.kvCacheStatus.collectAsStateWithLifecycle()
    val hardwareSnapshot by viewModel.hardwareSnapshot.collectAsStateWithLifecycle()
    val recentUsageLogs by viewModel.recentUsageLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val composerText by viewModel.composerText.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val exportedMarkdown by viewModel.exportedMarkdownPreview.collectAsStateWithLifecycle()

    val themeMode = remember(settings.themeMode) {
        runCatching { AppThemeMode.valueOf(settings.themeMode) }.getOrDefault(AppThemeMode.DARK)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var milestone3ModalMessage by remember { mutableStateOf<String?>(null) }

    // Mandatory BackHandler for secondary screens or active conversation return
    BackHandler(enabled = currentDestination != AppDestination.HOME_OR_CHAT || activeConversationId != null) {
        if (currentDestination != AppDestination.HOME_OR_CHAT) {
            viewModel.navigateTo(AppDestination.HOME_OR_CHAT)
        } else if (activeConversationId != null) {
            viewModel.startNewChat()
        }
    }

    PagePilotTheme(themeMode = themeMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                PagePilotNavigationDrawerContent(
                    currentDestination = currentDestination,
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onNewChat = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onSelectConversation = { id ->
                        viewModel.openConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onRenameConversation = viewModel::renameConversation,
                    onTogglePinConversation = viewModel::togglePinConversation,
                    onToggleArchiveConversation = viewModel::toggleArchiveConversation,
                    onDeleteConversation = viewModel::deleteConversation,
                    onExportConversation = viewModel::exportConversation,
                    onNavigateTo = { dest ->
                        viewModel.navigateTo(dest)
                        scope.launch { drawerState.close() }
                    },
                    onShowMilestone3Dialog = { msg ->
                        milestone3ModalMessage = msg
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    PagePilotTopBar(
                        title = currentDestination.title,
                        loadedModel = loadedModel,
                        hardwareSnapshot = hardwareSnapshot,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenModelManager = { viewModel.navigateTo(AppDestination.MODEL_MANAGER) },
                        onNewChat = viewModel::startNewChat
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Dismissible Status / Feedback Banner
                    AnimatedVisibility(visible = statusBanner != null) {
                        statusBanner?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = viewModel::dismissStatusBanner) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentDestination) {
                            AppDestination.HOME_OR_CHAT -> {
                                HomeAndChatWorkspaceScreen(
                                    activeConversationId = activeConversationId,
                                    messages = activeMessages,
                                    recentConversations = conversations.filter { !it.isArchived },
                                    loadedModel = loadedModel,
                                    availableModels = models,
                                    kvCacheStatus = kvCacheStatus,
                                    hardwareSnapshot = hardwareSnapshot,
                                    composerText = composerText,
                                    isGenerating = isGenerating,
                                    onComposerTextChange = viewModel::updateComposerText,
                                    onSendPrompt = viewModel::sendPrompt,
                                    onStopGeneration = viewModel::stopGeneration,
                                    onEditMessage = viewModel::editUserMessageAndRegenerate,
                                    onRegenerateMessage = viewModel::regenerateAssistantResponse,
                                    onFeedbackChange = { msg, fb -> viewModel.recordMessageFeedback(msg.id, fb) },
                                    onSelectConversation = viewModel::openConversation,
                                    onQuickLoadModel = viewModel::loadModelIntoMemory,
                                    onOpenModelManager = { viewModel.navigateTo(AppDestination.MODEL_MANAGER) },
                                    onOpenArchitectureSpec = { viewModel.navigateTo(AppDestination.ARCHITECTURE_SPEC) },
                                    onOpenSettings = { viewModel.navigateTo(AppDestination.SETTINGS) },
                                    onShowStatusMessage = viewModel::showStatusBanner
                                )
                            }

                            AppDestination.MODEL_MANAGER -> {
                                ModelManagerScreen(
                                    models = models,
                                    loadedModel = loadedModel,
                                    hardwareSnapshot = hardwareSnapshot,
                                    kvCacheStatus = kvCacheStatus,
                                    recentUsageLogs = recentUsageLogs,
                                    onImportSafUri = viewModel::importModelFromSaf,
                                    onCreateVerifyCheckpoint = viewModel::generateVerificationCheckpoint,
                                    onAuditStorage = viewModel::auditAllModelsStorage,
                                    onLoadModel = viewModel::loadModelIntoMemory,
                                    onUnloadModel = viewModel::unloadModelFromMemory,
                                    onVerifyModel = viewModel::verifyModelIntegrity,
                                    onSelectActiveModel = viewModel::selectActiveModel,
                                    onDeleteModel = viewModel::deleteModel
                                )
                            }

                            AppDestination.ARCHITECTURE_SPEC -> {
                                ArchitectureAndMathSpecScreen()
                            }

                            AppDestination.SETTINGS -> {
                                SettingsScreen(
                                    settings = settings,
                                    onUpdateSettings = viewModel::updateSettings
                                )
                            }

                            AppDestination.ABOUT -> {
                                AboutAndMilestonesScreen()
                            }
                        }
                    }
                }
            }
        }

        if (exportedMarkdown != null) {
            AlertDialog(
                onDismissRequest = viewModel::clearExportPreview,
                title = { Text("Exported Offline Conversation (Markdown)") },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = exportedMarkdown!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            copyToClipboard(context, "PagePilot Conversation Export", exportedMarkdown!!)
                            viewModel.clearExportPreview()
                            viewModel.showStatusBanner("Conversation Markdown copied to clipboard.")
                        }
                    ) {
                        Text("Copy Markdown")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::clearExportPreview) {
                        Text("Close")
                    }
                }
            )
        }

        if (milestone3ModalMessage != null) {
            AlertDialog(
                onDismissRequest = { milestone3ModalMessage = null },
                title = { Text("Milestone 3 Modular Extension Point") },
                text = {
                    Text(
                        text = milestone3ModalMessage!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { milestone3ModalMessage = null }) {
                        Text("Got It")
                    }
                }
            )
        }
    }
}
