package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["updatedAt"]), Index(value = ["isPinned"]), Index(value = ["isArchived"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val activeModelId: String?,
    val projectId: String? = null, // Modular schema hook for Milestone 3 Projects
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val lastMessagePreview: String = "",
    val messageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

enum class MessageFeedback {
    NONE,
    POSITIVE,
    NEGATIVE
}

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"]), Index(value = ["createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val parentMessageId: String? = null,
    val role: String, // MessageRole.name
    val content: String,
    val modelIdUsed: String? = null,
    val isVerificationOutput: Boolean = false,
    val feedback: String = MessageFeedback.NONE.name,
    val tokensPerSec: Float = 0f,
    val tokenCount: Int = 0,
    val prefillMs: Long = 0L,
    val generationDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ModelIntegrityStatus(val label: String) {
    VERIFIED("Verified (SHA-256 & Header Match)"),
    UNVERIFIED("Pending Verification"),
    INVALID_HEADER("Invalid PagePilot Header"),
    PARAMETER_MISMATCH("Parameter Count Math Mismatch"),
    CHECKSUM_MISMATCH("SHA-256 Checksum Mismatch"),
    MISSING_FILE("Missing Local & Source File")
}

enum class ModelRuntimeStatus(val label: String) {
    UNLOADED("Unloaded"),
    LOADING("Loading into RAM..."),
    LOADED("Loaded in Memory"),
    ERROR("Load Error / OOM Guardrail")
}

@Entity(
    tableName = "models",
    indices = [Index(value = ["isSelected"]), Index(value = ["importedAt"])]
)
data class ModelMetadataEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val configId: String,
    val exactParameterCount: Long,
    val parameterLabel: String,
    val vocabSize: Int,
    val tieWordEmbeddings: Boolean,
    val numLayers: Int,
    val dModel: Int,
    val numQueryHeads: Int,
    val numKvHeads: Int,
    val dFfn: Int,
    val quantization: String,
    val contextLength: Int,
    val fileSizeBytes: Long,
    val requiredStorageMb: Long,
    val estimatedRamMb: Long,
    val sha256Checksum: String,
    val localFilePath: String,
    val sourceSafUri: String?,
    val hasPersistableUriPermission: Boolean = false,
    val isRandomWeightsVerification: Boolean = false,
    val integrityStatus: String = ModelIntegrityStatus.VERIFIED.name,
    val runtimeStatus: String = ModelRuntimeStatus.UNLOADED.name,
    val isSelected: Boolean = false,
    val importedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)

@Entity(
    tableName = "model_usage_logs",
    indices = [Index(value = ["modelId"]), Index(value = ["timestamp"])]
)
data class ModelUsageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: String,
    val modelName: String,
    val conversationId: String,
    val promptTokens: Int,
    val generatedTokens: Int,
    val tokensPerSec: Float,
    val prefillMs: Long,
    val decodeMs: Long,
    val usedRamMb: Long,
    val availableRamMb: Long,
    val thermalStatusLabel: String,
    val batteryLevelPct: Int,
    val batteryCurrentMicroAmps: String,
    val isVerificationRun: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "DARK", // SYSTEM, LIGHT, DARK
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val maxTokens: Int = 256,
    val contextWindowLimit: Int = 1024,
    val ramSafetyCeilingPct: Int = 80 // Warn/block if total RAM usage exceeds 80% on 8GB device
)
