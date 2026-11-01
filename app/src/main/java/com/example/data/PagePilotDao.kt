package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PagePilotDao {

    // --- Conversations ---
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :newTitle, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun renameConversation(conversationId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun setConversationPinned(conversationId: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun setConversationArchived(conversationId: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    // --- Messages ---
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesForConversationSync(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: MessageEntity)

    @Query("UPDATE messages SET content = :content, tokensPerSec = :tps, tokenCount = :tokens, prefillMs = :prefillMs, generationDurationMs = :durationMs WHERE id = :messageId")
    suspend fun updateMessageStreamingProgress(
        messageId: String,
        content: String,
        tps: Float,
        tokens: Int,
        prefillMs: Long,
        durationMs: Long
    )

    @Query("UPDATE messages SET feedback = :feedback WHERE id = :messageId")
    suspend fun updateMessageFeedback(messageId: String, feedback: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND createdAt >= :fromTimestamp")
    suspend fun deleteMessagesFromTimestamp(conversationId: String, fromTimestamp: Long)

    // --- Models ---
    @Query("SELECT * FROM models ORDER BY isSelected DESC, importedAt DESC")
    fun observeAllModels(): Flow<List<ModelMetadataEntity>>

    @Query("SELECT * FROM models ORDER BY isSelected DESC, importedAt DESC")
    suspend fun getAllModelsSync(): List<ModelMetadataEntity>

    @Query("SELECT * FROM models WHERE id = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelMetadataEntity?

    @Query("SELECT * FROM models WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedModelSync(): ModelMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModel(model: ModelMetadataEntity)

    @Update
    suspend fun updateModel(model: ModelMetadataEntity)

    @Query("UPDATE models SET isSelected = 0")
    suspend fun clearSelectedModels()

    @Query("UPDATE models SET isSelected = 1, lastUsedAt = :timestamp WHERE id = :modelId")
    suspend fun markModelSelected(modelId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE models SET runtimeStatus = :runtimeStatus WHERE id = :modelId")
    suspend fun updateModelRuntimeStatus(modelId: String, runtimeStatus: String)

    @Query("UPDATE models SET runtimeStatus = 'UNLOADED'")
    suspend fun markAllModelsUnloaded()

    @Query("UPDATE models SET integrityStatus = :integrityStatus WHERE id = :modelId")
    suspend fun updateModelIntegrityStatus(modelId: String, integrityStatus: String)

    @Query("DELETE FROM models WHERE id = :modelId")
    suspend fun deleteModelById(modelId: String)

    // --- Model Usage Telemetry Logs (Strictly Local) ---
    @Query("SELECT * FROM model_usage_logs ORDER BY timestamp DESC LIMIT 40")
    fun observeRecentUsageLogs(): Flow<List<ModelUsageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLog(log: ModelUsageLogEntity)

    // --- Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
