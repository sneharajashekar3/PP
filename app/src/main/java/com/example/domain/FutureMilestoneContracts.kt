package com.example.domain

/**
 * Modular Architecture Contracts for Future Milestones (Milestones 2, 3 & 4).
 *
 * Milestone 1 focuses exclusively on:
 * - Native Android Chat UI (Light & Dark themes)
 * - Jetpack Compose Navigation
 * - Room SQLite database & persistent chat history
 * - SAF Model Manager with app-private checkpoint storage & missing-file recovery
 * - LocalModelEngine abstraction & pipeline verification
 * - Exact parameter calculation & SHA-256 integrity verification
 * - 8 GB RAM, thermal, and best-effort battery monitoring
 *
 * The contracts below establish clean compile-time extension boundaries so that
 * Projects, User Memory, PDF/BM25 RAG, Voice Input, and the LiteRT-LM / C++ JNI
 * runtime plug into the app in subsequent milestones without rewriting the UI.
 */

data class ProjectWorkspaceSpec(
    val id: String,
    val name: String,
    val systemInstructions: String,
    val documentCount: Int
)

interface ProjectWorkspaceContract {
    val isImplementedInCurrentMilestone: Boolean get() = false
    suspend fun listWorkspaces(): List<ProjectWorkspaceSpec>
}

data class UserMemoryItemSpec(
    val id: String,
    val fact: String,
    val isEnabled: Boolean
)

interface UserMemoryContract {
    val isImplementedInCurrentMilestone: Boolean get() = false
    suspend fun getActiveMemoriesForContext(): List<UserMemoryItemSpec>
}

data class RagCitationSpec(
    val documentName: String,
    val pageNumber: Int?,
    val bm25Score: Float,
    val snippet: String
)

interface DocumentRagContract {
    val isImplementedInCurrentMilestone: Boolean get() = false
    suspend fun retrieveRelevantChunks(query: String, topK: Int = 3): List<RagCitationSpec>
}

interface VoiceInputContract {
    val isImplementedInCurrentMilestone: Boolean get() = false
    fun isOfflineRecognizerAvailable(): Boolean
}

enum class MilestoneStatus(val badge: String) {
    COMPLETED_M1("Functional in Milestone 1"),
    VALIDATION_GATE_M2("Milestone 2: PyTorch Training & LiteRT-LM Validation Gate"),
    PLANNED_EXTENSION_M3("Milestone 3: Architecture Prepared (Deferred Implementation)"),
    APK_RELEASE_M4("Milestone 4: ZIP Export, Android Studio & Signed Release APK")
}

data class FeatureMilestoneReport(
    val featureName: String,
    val status: MilestoneStatus,
    val technicalSummary: String
)

object MilestoneRoadmapRegistry {
    val FEATURE_REPORTS: List<FeatureMilestoneReport> = listOf(
        FeatureMilestoneReport(
            featureName = "Native Compose Chat UI (Light & Dark Themes)",
            status = MilestoneStatus.COMPLETED_M1,
            technicalSummary = "Complete Home screen, Navigation Drawer, Chat thread with streaming, Markdown/code blocks, copy/edit/regenerate/stop/feedback, and theme switching."
        ),
        FeatureMilestoneReport(
            featureName = "Room SQLite Persistent Chat & Model Registry",
            status = MilestoneStatus.COMPLETED_M1,
            technicalSummary = "Full local SQLite persistence for conversations, branching messages, settings, model metadata, and local telemetry logs across app restarts."
        ),
        FeatureMilestoneReport(
            featureName = "SAF Model Manager & App-Private Checkpoint Storage",
            status = MilestoneStatus.COMPLETED_M1,
            technicalSummary = "Imports checkpoints via Storage Access Framework, persists URI permissions, copies into filesDir/models/, verifies SHA-256 & headers, and recovers from missing files."
        ),
        FeatureMilestoneReport(
            featureName = "Exact Parameter Calculator & Validator",
            status = MilestoneStatus.COMPLETED_M1,
            technicalSummary = "Mathematically verifies all 6 PagePilot configurations (918,144 params at V=4096 up to 3,015,355,392 params at 3B) in both Kotlin and Python."
        ),
        FeatureMilestoneReport(
            featureName = "Replaceable LocalModelEngine & Pipeline Verifier",
            status = MilestoneStatus.COMPLETED_M1,
            technicalSummary = "Clean Kotlin abstraction supporting load/unload, KV cache, sampling, and streaming. Includes explicit 918K random-weight pipeline test mode; zero fake AI responses."
        ),
        FeatureMilestoneReport(
            featureName = "8 GB RAM, Thermal & Best-Effort Battery Telemetry",
            status = MilestoneStatus.COMPLETED_M1,
            technicalSummary = "Monitors system/app RAM via ActivityManager, thermal status via PowerManager, and best-effort battery metrics with zero network transmission."
        ),
        FeatureMilestoneReport(
            featureName = "Independent PyTorch Training & LiteRT-LM Export Gate",
            status = MilestoneStatus.VALIDATION_GATE_M2,
            technicalSummary = "Train 100.09M experimental & 336.12M mobile checkpoints in PyTorch; validate torch.export -> ai-edge-torch conversion, tokenization parity, and KV cache before locking runtime."
        ),
        FeatureMilestoneReport(
            featureName = "Projects, User Memory, PDF/BM25 RAG & Offline Voice",
            status = MilestoneStatus.PLANNED_EXTENSION_M3,
            technicalSummary = "Modular domain contracts and database hooks prepared in Milestone 1; full document extraction, BM25 lexical scoring, and workspace memory scheduled for Milestone 3."
        ),
        FeatureMilestoneReport(
            featureName = "ZIP Export, Android Studio Native Hook & Signed APK",
            status = MilestoneStatus.APK_RELEASE_M4,
            technicalSummary = "Export ZIP from Google AI Studio, complete native inference integration in Android Studio, build signed release APK, and verify under Airplane Mode."
        )
    )
}
