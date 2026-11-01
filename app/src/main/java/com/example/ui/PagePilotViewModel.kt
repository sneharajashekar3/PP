package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettingsEntity
import com.example.data.ConversationEntity
import com.example.data.MessageEntity
import com.example.data.MessageFeedback
import com.example.data.MessageRole
import com.example.data.ModelIntegrityStatus
import com.example.data.ModelMetadataEntity
import com.example.data.ModelRuntimeStatus
import com.example.data.ModelUsageLogEntity
import com.example.data.PagePilotDatabase
import com.example.data.PagePilotRepository
import com.example.engine.ConversationTurnContext
import com.example.engine.DeviceHardwareMonitor
import com.example.engine.HardwareTelemetrySnapshot
import com.example.engine.KvCacheStatus
import com.example.engine.LocalModelEngine
import com.example.engine.PagePilotVerificationEngine
import com.example.engine.SamplingConfig
import com.example.engine.StreamTokenEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppDestination(val route: String, val title: String) {
    HOME_OR_CHAT("chat", "PagePilot"),
    MODEL_MANAGER("model_manager", "Model Manager"),
    ARCHITECTURE_SPEC("architecture_spec", "Architecture & Math Spec"),
    SETTINGS("settings", "Settings"),
    ABOUT("about", "About & Milestones")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PagePilotViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PagePilotDatabase.getInstance(application)
    val repository = PagePilotRepository(application, database.pagePilotDao())
    val hardwareMonitor = DeviceHardwareMonitor(application)
    val modelEngine: LocalModelEngine = PagePilotVerificationEngine(hardwareMonitor)

    private val _currentDestination = MutableStateFlow(AppDestination.HOME_OR_CHAT)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _exportedMarkdownPreview = MutableStateFlow<String?>(null)
    val exportedMarkdownPreview: StateFlow<String?> = _exportedMarkdownPreview.asStateFlow()

    private val _hardwareSnapshot = MutableStateFlow(hardwareMonitor.captureSnapshot())
    val hardwareSnapshot: StateFlow<HardwareTelemetrySnapshot> = _hardwareSnapshot.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val models: StateFlow<List<ModelMetadataEntity>> = repository.models
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentUsageLogs: StateFlow<List<ModelUsageLogEntity>> = repository.recentUsageLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity> = repository.settings
        .flatMapLatest { entity -> flowOf(entity ?: AppSettingsEntity()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId == null) flowOf(emptyList()) else repository.observeMessages(convId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loadedModel: StateFlow<ModelMetadataEntity?> = modelEngine.loadedModel
    val kvCacheStatus: StateFlow<KvCacheStatus> = modelEngine.kvCacheStatus
    val isGenerating: StateFlow<Boolean> = modelEngine.isGenerating

    private var activeStreamJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
            // Ensure all models start in UNLOADED state after cold process launch and run storage integrity audit
            val existingModels = database.pagePilotDao().getAllModelsSync()
            database.pagePilotDao().markAllModelsUnloaded()
            if (existingModels.isEmpty()) {
                // Provision the sub-1M (918,144 params, V=4096) pipeline verification checkpoint on first launch
                repository.createVerificationMicroCheckpoint(useCompact4kVocab = true)
            } else {
                repository.auditAndVerifyAllModels()
            }
        }

        // Periodic strictly local hardware telemetry refresh (every 3 seconds)
        viewModelScope.launch {
            while (isActive) {
                _hardwareSnapshot.value = hardwareMonitor.captureSnapshot()
                delay(3000L)
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateComposerText(text: String) {
        _composerText.value = text
    }

    fun dismissStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusBanner(message: String) {
        _statusBannerMessage.value = message
    }

    fun clearExportPreview() {
        _exportedMarkdownPreview.value = null
    }

    fun startNewChat() {
        _activeConversationId.value = null
        _composerText.value = ""
        modelEngine.clearKvCache()
        _currentDestination.value = AppDestination.HOME_OR_CHAT
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        _currentDestination.value = AppDestination.HOME_OR_CHAT
        viewModelScope.launch {
            val msgs = repository.getMessagesSync(conversationId)
            modelEngine.updateConversationContext(
                msgs.map { ConversationTurnContext(it.role, it.content) }
            )
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameConversation(conversationId, newTitle)
        }
    }

    fun togglePinConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository.togglePinConversation(conversation)
        }
    }

    fun toggleArchiveConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository.toggleArchiveConversation(conversation)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
            repository.deleteConversation(conversationId)
        }
    }

    fun exportConversation(conversationId: String) {
        viewModelScope.launch {
            val md = repository.exportConversationAsMarkdown(conversationId)
            if (md.isNotBlank()) {
                _exportedMarkdownPreview.value = md
            }
        }
    }

    // --- Chat Send, Stream, Edit, Regenerate & Stop ---

    fun sendPrompt(overridePrompt: String? = null) {
        val rawPrompt = (overridePrompt ?: _composerText.value).trim()
        if (rawPrompt.isEmpty() || isGenerating.value) return

        val currentlyLoaded = loadedModel.value
        if (currentlyLoaded == null) {
            _statusBannerMessage.value =
                "No PagePilot model is loaded in RAM. Open Model Manager to load a verified checkpoint—PagePilot never fakes AI responses."
            return
        }

        if (overridePrompt == null) {
            _composerText.value = ""
        }

        viewModelScope.launch {
            val convId = _activeConversationId.value ?: run {
                val created = repository.createConversation(
                    title = rawPrompt.take(40),
                    activeModelId = currentlyLoaded.id
                )
                _activeConversationId.value = created.id
                created.id
            }

            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.USER.name,
                content = rawPrompt,
                modelIdUsed = currentlyLoaded.id,
                createdAt = System.currentTimeMillis()
            )
            repository.insertMessage(userMsg)
            triggerAssistantGeneration(convId, rawPrompt, parentUserMessageId = userMsg.id)
        }
    }

    fun editUserMessageAndRegenerate(message: MessageEntity, updatedContent: String) {
        val trimmed = updatedContent.trim()
        if (trimmed.isEmpty() || isGenerating.value) return
        val currentlyLoaded = loadedModel.value
        if (currentlyLoaded == null) {
            _statusBannerMessage.value = "Load a verified PagePilot checkpoint in Model Manager before regenerating."
            return
        }

        viewModelScope.launch {
            // Truncate messages from the edited message timestamp onward and insert the updated turn
            repository.truncateMessagesFrom(message.conversationId, message.createdAt)
            val newUserMsg = message.copy(
                id = UUID.randomUUID().toString(),
                content = trimmed,
                createdAt = System.currentTimeMillis()
            )
            repository.insertMessage(newUserMsg)
            triggerAssistantGeneration(
                conversationId = message.conversationId,
                promptText = trimmed,
                parentUserMessageId = newUserMsg.id
            )
        }
    }

    fun regenerateAssistantResponse(assistantMessage: MessageEntity) {
        if (isGenerating.value) return
        val currentlyLoaded = loadedModel.value
        if (currentlyLoaded == null) {
            _statusBannerMessage.value = "Load a verified PagePilot checkpoint in Model Manager before regenerating."
            return
        }

        viewModelScope.launch {
            val allMsgs = repository.getMessagesSync(assistantMessage.conversationId)
            val idx = allMsgs.indexOfFirst { it.id == assistantMessage.id }
            val precedingUserMsg = if (idx > 0) {
                allMsgs.subList(0, idx).lastOrNull { it.role == MessageRole.USER.name }
            } else {
                allMsgs.lastOrNull { it.role == MessageRole.USER.name }
            }
            val promptText = precedingUserMsg?.content ?: return@launch

            repository.truncateMessagesFrom(assistantMessage.conversationId, assistantMessage.createdAt)
            triggerAssistantGeneration(
                conversationId = assistantMessage.conversationId,
                promptText = promptText,
                parentUserMessageId = precedingUserMsg.id
            )
        }
    }

    private fun triggerAssistantGeneration(
        conversationId: String,
        promptText: String,
        parentUserMessageId: String?
    ) {
        val activeModel = loadedModel.value ?: return
        activeStreamJob?.cancel()

        activeStreamJob = viewModelScope.launch {
            val historyMsgs = repository.getMessagesSync(conversationId)
            val turnContexts = historyMsgs.map { ConversationTurnContext(it.role, it.content) }
            val currentSettings = settings.value
            val samplingConfig = SamplingConfig(
                temperature = currentSettings.temperature,
                topK = currentSettings.topK,
                topP = currentSettings.topP,
                repetitionPenalty = currentSettings.repetitionPenalty,
                maxTokens = currentSettings.maxTokens,
                contextWindowLimit = currentSettings.contextWindowLimit
            )

            val assistantMsgId = UUID.randomUUID().toString()
            val placeholderMsg = MessageEntity(
                id = assistantMsgId,
                conversationId = conversationId,
                parentMessageId = parentUserMessageId,
                role = MessageRole.ASSISTANT.name,
                content = "…",
                modelIdUsed = activeModel.id,
                isVerificationOutput = activeModel.isRandomWeightsVerification,
                createdAt = System.currentTimeMillis() + 1L
            )
            repository.insertMessage(placeholderMsg)

            var prefillTimeMs = 0L
            val streamStartMs = System.currentTimeMillis()

            modelEngine.streamTokens(turnContexts, promptText, samplingConfig).collect { event ->
                when (event) {
                    is StreamTokenEvent.PrefillStarted -> {
                        prefillTimeMs = (System.currentTimeMillis() - streamStartMs).coerceAtLeast(1L)
                    }
                    is StreamTokenEvent.TokenDelta -> {
                        repository.updateStreamingMessage(
                            messageId = assistantMsgId,
                            conversationId = conversationId,
                            content = event.cumulativeText,
                            tps = event.tokensPerSecond,
                            tokenCount = event.generatedCount,
                            prefillMs = prefillTimeMs,
                            durationMs = System.currentTimeMillis() - streamStartMs
                        )
                    }
                    is StreamTokenEvent.Completed -> {
                        repository.updateStreamingMessage(
                            messageId = assistantMsgId,
                            conversationId = conversationId,
                            content = event.fullText,
                            tps = event.averageTokensPerSecond,
                            tokenCount = event.generatedTokens,
                            prefillMs = event.prefillDurationMs,
                            durationMs = event.prefillDurationMs + event.decodeDurationMs
                        )
                        val hw = hardwareMonitor.captureSnapshot()
                        _hardwareSnapshot.value = hw
                        repository.recordUsageLog(
                            ModelUsageLogEntity(
                                modelId = activeModel.id,
                                modelName = activeModel.name,
                                conversationId = conversationId,
                                promptTokens = event.promptTokens,
                                generatedTokens = event.generatedTokens,
                                tokensPerSec = event.averageTokensPerSecond,
                                prefillMs = event.prefillDurationMs,
                                decodeMs = event.decodeDurationMs,
                                usedRamMb = hw.usedDeviceRamMb,
                                availableRamMb = hw.availableDeviceRamMb,
                                thermalStatusLabel = hw.thermalStatusLabel,
                                batteryLevelPct = hw.batteryLevelPercent,
                                batteryCurrentMicroAmps = hw.batteryCurrentReport,
                                isVerificationRun = event.isVerificationOutput
                            )
                        )
                    }
                    is StreamTokenEvent.Failed -> {
                        repository.updateStreamingMessage(
                            messageId = assistantMsgId,
                            conversationId = conversationId,
                            content = "**[${event.error.code}]** ${event.error.userMessage}\n\n_${event.error.technicalDetail}_",
                            tps = 0f,
                            tokenCount = 0,
                            prefillMs = 0L,
                            durationMs = 0L
                        )
                        _statusBannerMessage.value = event.error.userMessage
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        modelEngine.cancelGeneration()
        activeStreamJob?.cancel()
        _statusBannerMessage.value = "Token generation stopped by user."
    }

    fun recordMessageFeedback(messageId: String, feedback: MessageFeedback) {
        viewModelScope.launch {
            repository.setMessageFeedback(messageId, feedback)
        }
    }

    // --- Model Manager Operations ---

    fun importModelFromSaf(uri: Uri) {
        viewModelScope.launch {
            _statusBannerMessage.value = "Importing checkpoint into app-private storage & computing SHA-256..."
            val result = repository.importModelFromSafUri(uri)
            result.fold(
                onSuccess = { imported ->
                    _statusBannerMessage.value =
                        "Imported '${imported.name}' (${imported.integrityStatus}). SHA-256: ${imported.sha256Checksum.take(12)}…"
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "SAF Import failed: ${err.message}"
                }
            )
        }
    }

    fun generateVerificationCheckpoint(useCompact4kVocab: Boolean) {
        viewModelScope.launch {
            val result = repository.createVerificationMicroCheckpoint(useCompact4kVocab)
            result.fold(
                onSuccess = { entity ->
                    _statusBannerMessage.value =
                        "Created ${entity.name} (${entity.parameterLabel} exact params, SHA-256 verified) in app-private storage."
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Failed to create verification checkpoint: ${err.message}"
                }
            )
        }
    }

    fun verifyModelIntegrity(modelId: String) {
        viewModelScope.launch {
            val status = repository.verifySingleModelIntegrity(modelId)
            _statusBannerMessage.value = "Integrity verification complete: ${status.label}"
        }
    }

    fun auditAllModelsStorage() {
        viewModelScope.launch {
            val issues = repository.auditAndVerifyAllModels()
            _statusBannerMessage.value = if (issues == 0) {
                "All registered model checkpoints passed storage & SHA-256 verification."
            } else {
                "Storage audit detected $issues model(s) with missing files or header mismatches."
            }
        }
    }

    fun loadModelIntoMemory(model: ModelMetadataEntity) {
        viewModelScope.launch {
            // Re-verify storage & integrity before loading
            val integrity = repository.verifySingleModelIntegrity(model.id)
            if (integrity != ModelIntegrityStatus.VERIFIED) {
                _statusBannerMessage.value =
                    "Cannot load '${model.name}': ${integrity.label}."
                return@launch
            }

            repository.updateModelRuntimeState(model.id, ModelRuntimeStatus.LOADING)
            val result = modelEngine.loadModel(model, settings.value.ramSafetyCeilingPct)
            result.fold(
                onSuccess = {
                    repository.selectActiveModel(model.id)
                    repository.updateModelRuntimeState(model.id, ModelRuntimeStatus.LOADED)
                    _hardwareSnapshot.value = hardwareMonitor.captureSnapshot()
                    _statusBannerMessage.value = if (model.isRandomWeightsVerification) {
                        "Loaded '${model.name}' in Pipeline Verification Mode (Random Weights)."
                    } else {
                        "Loaded '${model.name}' into memory."
                    }
                },
                onFailure = { err ->
                    repository.updateModelRuntimeState(model.id, ModelRuntimeStatus.ERROR)
                    _statusBannerMessage.value = err.message ?: "Model load failed."
                }
            )
        }
    }

    fun unloadModelFromMemory(modelId: String) {
        viewModelScope.launch {
            modelEngine.unloadModel()
            repository.updateModelRuntimeState(modelId, ModelRuntimeStatus.UNLOADED)
            _hardwareSnapshot.value = hardwareMonitor.captureSnapshot()
            _statusBannerMessage.value = "Model unloaded from RAM and KV cache cleared."
        }
    }

    fun selectActiveModel(model: ModelMetadataEntity) {
        viewModelScope.launch {
            repository.selectActiveModel(model.id)
            _statusBannerMessage.value = "Selected '${model.name}' as preferred checkpoint."
        }
    }

    fun deleteModel(model: ModelMetadataEntity) {
        viewModelScope.launch {
            if (loadedModel.value?.id == model.id) {
                modelEngine.unloadModel()
            }
            repository.deleteModel(model.id)
            _statusBannerMessage.value = "Deleted '${model.name}' from app-private storage."
        }
    }

    // --- Settings Updates ---

    fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            val updated = transform(settings.value)
            repository.updateSettings(updated)
        }
    }
}
