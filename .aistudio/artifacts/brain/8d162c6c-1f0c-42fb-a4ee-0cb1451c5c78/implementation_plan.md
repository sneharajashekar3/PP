# PagePilot — Private, Offline Native Android AI Assistant

PagePilot is a self-contained, zero-telemetry native Android AI assistant built with Kotlin, Jetpack Compose, and Material 3, designed to run our own independently developed decoder-only Transformer models completely offline on 8 GB RAM Android hardware. This plan separates our **Milestone 1 Foundation Deliverable** (built and verified inside Google AI Studio Build mode) from **Subsequent Milestones** (external PyTorch model training, LiteRT-LM export validation, advanced workspace features, and final signed release APK verification) while preserving the complete long-term product architecture.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> This plan incorporates all approved architectural decisions plus the four final technical corrections:

1. **Exact Model Parameter Mathematics & Automated Validation**:
   - Corrected the `PagePilot-Verify-Micro` calculation: a 32K vocabulary with $d_{\text{model}} = 128$ requires $4,096,000$ parameters for a single tied embedding matrix alone ($4.49\text{M}$ total with 2 layers). We therefore use a dedicated compact **4,096-token verification vocabulary** with **tied embeddings** and $d_{\text{ffn}} = 384$, yielding an exact **918,144 parameters (0.92M)** for `PagePilot-Verify-Micro` (and also document the exact **4,489,856-parameter (4.49M)** variant when testing the full 32,000-token production vocabulary).
   - Explicitly specify `d_ffn` (SwiGLU 3-matrix intermediate dimension) and `tie_word_embeddings` (`True` vs. `False`) for every tier, backed by both an automated Python parameter-count validation script (`validate_model_params.py`) and an in-app Kotlin `ModelParameterCalculator` verifier.
2. **Resilient SAF Model Storage & Missing-File Recovery**:
   - Importing a model via Android's Storage Access Framework (`OpenDocument`) takes persistable read URI permissions (`takePersistableUriPermission`) **and** copies the checkpoint into app-private internal storage (`context.filesDir/models/`) with atomic temp-file rename and SHA-256 verification.
   - Before every model load or app launch audit, the Model Manager verifies whether the app-private file and/or external SAF source URI still exist, transitioning missing or externally deleted files to `MISSING_FILE` status with clear recovery/re-import actions rather than crashing.
3. **Google AI Studio ZIP Export, Android Studio Continuation & Final Signed APK Milestone**:
   - Milestone 1 remains 100% compatible with Google AI Studio Build mode (single-activity, single-module, pure Kotlin + Jetpack Compose, zero NDK/C++ build blockers).
   - Includes a dedicated **Milestone 4: Project Export, Android Studio Integration & Signed Release APK Verification** workflow covering ZIP export, native runtime integration, signed release APK generation, installation, cold launch, SAF model import, and strict Airplane Mode offline verification.
4. **100% Local Diagnostics & Best-Effort Hardware Telemetry**:
   - All RAM, thermal, speed, and battery diagnostics remain strictly on-device in Room SQLite with zero network transmission.
   - Battery current (`BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`) and thermal status (`PowerManager`) are explicitly handled as **best-effort** telemetry with graceful `"Not reported by hardware HAL"` fallbacks for devices whose OEM kernel/fuel-gauge drivers return `Int.MIN_VALUE` or `0`.

---

### 1. Overview & Phased Roadmap

#### Milestone 1: Native Foundation, Persistence, Model Manager & `LocalModelEngine` *(Current Build Scope)*
- **What We Deliver Now**:
  - **Premium Conversational UI (Light & Dark Mode)**: Home screen with original PagePilot geometric emblem, greeting, active model status bar, starter prompts, and recent chats; Navigation Drawer; Full Chat View with streaming token rendering, Markdown & syntax-highlighted code blocks, copy, edit, regenerate, stop generation, thumbs up/down feedback, and timestamps; Multiline Composer with character/token counters and generation controls.
  - **Room SQLite Persistence**: Full local persistence across app restarts for multiple conversations, message history (including edit/regenerate branches), model registry metadata, recent model usage logs, and local settings.
  - **SAF Model Manager, App-Private Storage & Integrity Verifier**: Import `.ppilot` / `.litertlm` / `.tflite` checkpoints via Android's Storage Access Framework, copy checkpoints safely into `filesDir/models/` while recording persistable URI permissions, detect deleted/missing files gracefully, verify SHA-256 integrity and binary headers, validate exact parameter counts, and manage model load/unload/delete lifecycle.
  - **Replaceable `LocalModelEngine` & Technical Pipeline Verifier**: Complete Kotlin interface supporting load, unload, KV-cache management, configurable sampling (`temperature`, `topK`, `topP`, `repetitionPenalty`, `maxTokens`), streaming tokens, and cancellation. Includes an explicit **Technical Pipeline Verification Mode** using the 0.92M (`V=4096`) or 4.49M (`V=32000`) randomly initialized micro-checkpoint purely to validate binary loading, tokenizer encoding/decoding, KV-cache allocation, and streaming mechanics—explicitly labeled as untrained verification output and never presented as an intelligent assistant.
  - **8 GB Hardware Telemetry (Best-Effort Local Diagnostics)**: Real-time monitoring of system & app RAM (`ActivityManager.MemoryInfo`), OOM safety guardrails, thermal status (`PowerManager`), and best-effort battery level/current (`BatteryManager`).

#### Milestone 2: Independent PyTorch Model Training & LiteRT-LM Export-Validation Gate *(External Python/PyTorch + Android Studio)*
- Train the initial **100.09M Experimental PagePilot Transformer** (or **124.67M** untied variant) and custom 32K BPE tokenizer from scratch in PyTorch, validated by the automated parameter-count verification script.
- Execute the **LiteRT-LM Export-Validation Gate**: test `torch.export` / `ai-edge-torch` lowering of our exact custom decoder-only Transformer, verify numerical logit parity, test tokenizer integration, and benchmark loading/streaming on an 8 GB Android device.
- Scale training to the **336.12M Practical Mobile Model** (4-bit quantized), followed by empirical profiling of **1.10B** and **3.02B** checkpoints on 8 GB hardware.

#### Milestone 3: Advanced Workspace Features *(Modular Extension Milestone)*
- Plug in **Projects**, **User Memory**, **PDF/Document Processing + BM25 RAG**, and **Offline Voice Input** against the domain contracts prepared in Milestone 1.

#### Milestone 4: Project ZIP Export, Android Studio Continuation & Signed Release APK Verification
- Export the complete project as a ZIP from Google AI Studio, open in Android Studio, bind the validated native inference runtime into `LocalModelEngine`, assemble and sign the production release APK, and verify installation, cold launch, SAF checkpoint import, and 100% offline execution under Airplane Mode.

---

### 2. User Experience & Visual Design

- **Key User Flows (Milestone 1)**:
  1. **Home & Honest Model Readiness Bar**: Opening PagePilot displays our original geometric emblem, a top **Active Model Status Pill** (showing either the loaded PagePilot checkpoint and live RAM usage, or an honest *"No Trained Model Loaded — Open Model Manager"* state), starter prompt suggestions, and recent conversations.
  2. **Navigation Drawer & Conversation Management**: Access **New Chat**, **Search Conversations**, **Pinned Chats**, **Recent Chats**, **Archived Chats**, **Model Manager**, **Settings**, **About & Model Architecture Spec**, plus clearly badged architectural preview entry points for **Projects** and **Memory** indicating their milestone status.
  3. **Model Manager & Resilient SAF Checkpoint Lifecycle**:
     - Users tap **Import PagePilot Checkpoint** (`ActivityResultContracts.OpenDocument`) to select a local model file.
     - PagePilot requests `takePersistableUriPermission` (when supported by the provider) **and** streams the checkpoint into app-private storage (`filesDir/models/<model_id>.ppilot`) while computing its SHA-256 digest and parsing its header.
     - If an external source file or local checkpoint file is ever deleted or corrupted, a single-tap **Audit Storage & Integrity** check (also run automatically on startup) marks the entry `MISSING_FILE` or `CHECKSUM_MISMATCH` with a clean **Re-import / Remove Entry** recovery action.
     - Users can also generate a clearly labeled **Pipeline Verification Micro-Checkpoint (`0.92M`, `V=4096`, Random Weights)** on-device to verify the binary parser, KV-cache allocator, and token streamer without external files.
  4. **Chat & Streaming Interaction**: When no model is loaded, submitting a prompt never fakes an AI answer—it explains that a loaded PagePilot model is required and offers a direct button to open the Model Manager. When a technical verification checkpoint or imported PagePilot model is loaded, tokens stream via `LocalModelEngine` with live tokens/sec, prefill latency, and RAM delta metrics, full Markdown/code rendering, message editing, regeneration, copy, stop generation, and local feedback logging.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-craft dual-theme interface combining ChatGPT's clean typographic hierarchy, Gemini's spacious composer ergonomics, and Google AI Edge Gallery's transparent hardware/model telemetry badges.
  - *Color Palette & Mood*:
    - **Dark Theme**: Obsidian canvas (`#0D0F12`), elevated graphite surfaces (`#161920`), hairline dividers (`#262B36`), crisp alabaster text (`#F3F4F6`), muted slate labels (`#9CA3AF`), and indigo-teal accents (`#6366F1` / `#14B8A6`).
    - **Light Theme**: Warm paper canvas (`#F9FAFB`), pure white surface cards (`#FFFFFF`), subtle stone borders (`#E5E7EB`), deep ink primary text (`#111827`), cool slate secondary text (`#4B5563`), and deep indigo-teal accents (`#4338CA` / `#0D9488`).
  - *Typography & Hierarchy*: Locally bundled Google Fonts via `font-util`—**Plus Jakarta Sans** for headings and navigation, **Inter** for chat prose, and **JetBrains Mono** for code blocks, SHA-256 digests, exact parameter tables, and RAM/token telemetry.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Exact Parameter Accounting & Dedicated Verification Vocabulary**
  - *Chosen Approach*: Explicitly account for every weight tensor in our bias-free decoder-only Transformer:
    $$\text{Params}_{\text{embed}} = \begin{cases} V \cdot d_{\text{model}} & \text{if tied (`tie\_word\_embeddings = True`)} \\ 2 \cdot V \cdot d_{\text{model}} & \text{if untied (`tie\_word\_embeddings = False`)} \end{cases}$$
    $$\text{Params}_{\text{layer}} = \underbrace{2 d_{\text{model}}^2 + 2 d_{\text{model}} (H_{kv} \cdot d_{\text{head}})}_{\text{GQA } (W_q, W_k, W_v, W_o)} + \underbrace{3 d_{\text{model}} d_{\text{ffn}}}_{\text{SwiGLU } (W_{\text{gate}}, W_{\text{up}}, W_{\text{down}})} + \underbrace{2 d_{\text{model}}}_{\text{2 RMSNorms}}$$
    $$\text{Params}_{\text{total}} = \text{Params}_{\text{embed}} + L \cdot \text{Params}_{\text{layer}} + \underbrace{d_{\text{model}}}_{\text{Final RMSNorm}}$$
    Use $V = 4,096$ with tied embeddings for the sub-1M `PagePilot-Verify-Micro` checkpoint (**918,144 parameters**), and provide an automated validation calculator in both Kotlin (`ModelParameterCalculator`) and Python (`validate_model_params.py`) that asserts exact tensor totals before training or loading.
  - *Why*: Eliminates mathematical discrepancies where a 32K vocabulary alone would consume 4.10M–8.19M parameters at $d_{\text{model}} = 128$, and guarantees deterministic memory estimation on 8 GB devices.
- **Decision 2 — App-Private Checkpoint Copying + Persistable SAF URI Fallback**
  - *Chosen Approach*: Copy SAF-imported checkpoints into `context.filesDir/models/` during import while also requesting `takePersistableUriPermission` on the source URI and recording both `localFilePath` and `sourceUri` in Room.
  - *Why*: External files in `Downloads/` or USB/SD storage are frequently moved, renamed, or evicted by the user, and SAF `InputStream` random access is slower than direct `FileChannel` memory mapping (`MappedByteBuffer`) from app-private storage.
- **Decision 3 — Strict Offline Operation & Best-Effort Local Hardware Telemetry**
  - *Chosen Approach*: Remove cloud/network libraries from active dependencies, declare zero `INTERNET` permissions in `AndroidManifest.xml`, and wrap `BatteryManager` current-now queries (`BATTERY_PROPERTY_CURRENT_NOW`) and `PowerManager` thermal queries in defensive best-effort readers that clearly report `"Hardware sensor unsupported"` when an Android OEM kernel or emulator returns `0` or `Int.MIN_VALUE`.
  - *Why*: Prevents misleading `0 mA` readings on unsupported fuel-gauge HALs while keeping 100% of device diagnostics local.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### System & Milestone Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────────────┐
│              MILESTONE 1: GOOGLE AI STUDIO NATIVE ANDROID BUILD               │
│          Single-Activity • Single-Module • Kotlin • Jetpack Compose           │
├───────────────────────────────────────────────────────────────────────────────┤
│  UI Layer (Material 3 Light & Dark Themes)                                    │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────────┐  │
│  │ Home & Chat Screen  │  │ Navigation Drawer   │  │ Model Manager & Spec  │  │
│  │ • Streaming Thread  │  │ • New / Search Chat │  │ • SAF File Import +   │  │
│  │ • Markdown & Code   │  │ • Pinned / Archived │  │   App-Private Copy    │  │
│  │ • Edit / Regenerate │  │ • Model Manager     │  │ • SHA-256 & Exact     │  │
│  │ • Stop & Feedback   │  │ • Settings & Spec   │  │   Param Calculator    │  │
│  │ • Multiline Composer│  │ • Future Hooks      │  │ • 8GB RAM & Best-     │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  │   Effort Hal Telemetry│  │
│             └────────────────────────┼─────────────┴───────────┬───────────┘  │
│                                      ▼                         │              │
│  State & Domain Layer (`PagePilotViewModel` + Modular Contracts)│             │
│  ┌───────────────────────────────────┐  ┌──────────────────────┴───────────┐  │
│  │ Chat & Model Registry Repositories│  │ LocalModelEngine (Interface)     │  │
│  │ • Conversation / Message CRUD     │  │ • loadModel() / unloadModel()    │  │
│  │ • SAF Copy -> `filesDir/models/`  │  │ • generateStream(Context, Config)│  │
│  │ • Missing-File Audit & Recovery   │  │ • cancelGeneration()             │  │
│  │ • Extension Contracts:            │  │ • clearKvCache() / getRamStats() │  │
│  │   Project / Memory / BM25 / Voice │  └─────────────────┬────────────────┘  │
│  └─────────────────┬─────────────────┘                    │                   │
│                    ▼                                      ▼                   │
│  ┌───────────────────────────────────┐  ┌──────────────────────────────────┐  │
│  │ Room SQLite Database (`pagepilot`)│  │ Milestone 1 Verification Engine  │  │
│  │ • conversations • messages        │  │ • Binary `.ppilot` Header Parser │  │
│  │ • models        • model_usage     │  │ • Exact Parameter Count Validator│  │
│  │ • settings      • future_schemas  │  │ • Explicit Pipeline Test Mode    │  │
│  └───────────────────────────────────┘  └─────────────────┬────────────────┘  │
└───────────────────────────────────────────────────────────┼───────────────────┘
                                                            │
         ===================================================╪====================
         External Training, Export & Final APK Verification │ (Milestones 2–4)
         ===================================================▼====================
┌───────────────────────────────────────────────────────────────────────────────┐
│  MILESTONE 2: Independent PyTorch Training + `validate_model_params.py` +     │
│               LiteRT-LM Export-Validation Gate (100.09M -> 336.12M -> 1B/3B)  │
│  MILESTONE 3: Modular Extensions (Projects, User Memory, PDF/BM25 RAG, Voice) │
│  MILESTONE 4: Google AI Studio ZIP Export -> Android Studio Continuation ->   │
│               Signed Release APK Build -> Install & Airplane-Mode Verification│
└───────────────────────────────────────────────────────────────────────────────┘
```

#### Exact Mathematical Parameter Table Across All PagePilot Configurations

Every configuration uses bias-free linear projections (`bias=False`), Pre-RMSNorm (`2 * d_model` per layer + `1 * d_model` final norm), Grouped-Query Attention ($W_q, W_k, W_v, W_o$), and SwiGLU FFN ($W_{\text{gate}}, W_{\text{up}}, W_{\text{down}}$ with explicit intermediate dimension $d_{\text{ffn}}$):

| Configuration ID | Vocab ($V$) | Tied Embeds | Layers ($L$) | $d_{\text{model}}$ | Heads ($H_q : H_{kv}$) | $d_{\text{head}}$ | SwiGLU $d_{\text{ffn}}$ | Embed + LM Head Params | Per-Layer Params (Attn + FFN + Norm) | Final Norm | **Exact Total Parameters** |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **`PagePilot-Verify-Micro-918K`** *(Compact Vocab)* | $4,096$ | **Yes** | $2$ | $128$ | $4 : 2$ | $32$ | $384$ | $524,288$ | $49,152 + 147,456 + 256 = 196,864$ | $128$ | **918,144** *(0.92M)* |
| **`PagePilot-Verify-Micro-4.5M`** *(32K Vocab Test)* | $32,000$ | **Yes** | $2$ | $128$ | $4 : 2$ | $32$ | $384$ | $4,096,000$ | $49,152 + 147,456 + 256 = 196,864$ | $128$ | **4,489,856** *(4.49M)* |
| **`PagePilot-Exp-100M`** *(Stage 1 Experimental)* | $32,000$ | **Yes** | $12$ | $768$ | $12 : 4$ | $64$ | $2,048$ | $24,576,000$ | $1,572,864 + 4,718,592 + 1,536 = 6,292,992$ | $768$ | **100,092,672** *(100.09M)* |
| **`PagePilot-Mobile-336M`** *(Stage 2 Practical Mobile)* | $32,000$ | **No** | $24$ | $1,024$ | $16 : 4$ | $64$ | $2,816$ | $65,536,000$ | $2,621,440 + 8,650,752 + 2,048 = 11,274,240$ | $1,024$ | **336,118,784** *(336.12M)* |
| **`PagePilot-Edge-1.1B`** *(Stage 3A Scaling)* | $32,000$ | **No** | $22$ | $2,048$ | $32 : 4$ | $64$ | $5,632$ | $131,072,000$ | $9,437,184 + 34,603,008 + 4,096 = 44,044,288$ | $2,048$ | **1,100,048,384** *(1.10B)* |
| **`PagePilot-3B`** *(Stage 3B Flagship)* | $32,000$ | **No** | $28$ | $3,072$ | $24 : 8$ | $128$ | $8,192$ | $196,608,000$ | $25,165,824 + 75,497,472 + 6,144 = 100,669,440$ | $3,072$ | **3,015,355,392** *(3.02B)* |

- **Automated Parameter Validation (`ModelParameterCalculator` & `validate_model_params.py`)**:
  - Both the Android app's Model Architecture Inspector and the standalone Python specification include an automated parameter-count validator that computes the analytical breakdown above and compares it against actual instantiated tensor shapes (`sum(p.numel() for p in model.parameters())`), rejecting any binary header whose declared parameter count diverges from its structural configuration.

#### SAF Model Persistence & Missing-File Recovery Protocol

1. **Import & Copy**: When a user selects a file via `ActivityResultContracts.OpenDocument`, PagePilot attempts `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` and streams the file into `File(context.filesDir, "models/${uuid}.ppilot.tmp")`, computing the SHA-256 digest and verifying the PagePilot header on the fly before atomically renaming to `.ppilot`.
2. **Startup & Pre-Load Storage Audit**: On app startup and before `LocalModelEngine.loadModel()`, `ModelRegistryRepository.auditStoredModels()` checks `File(localFilePath).exists()` and `File(localFilePath).length() == expectedSizeBytes`. If the app-private file is missing, it checks whether the persisted `sourceUri` is still readable to offer a one-tap **Restore from Source URI**; if the external source file was also deleted, it transitions the model record to `IntegrityStatus.MISSING_FILE` and surfaces a non-crashing recovery banner in the Model Manager.

#### Final APK Build, Export & Airplane-Mode Verification Workflow

1. **Google AI Studio Build Verification**: Compile and verify Milestone 1 inside the hosted environment (`compile_applet` + Robolectric unit tests for parameter math, Room persistence, and SAF missing-file recovery).
2. **ZIP Export & Android Studio Continuation**: Export the project via AI Studio's **Export ZIP** action, open in Android Studio, and bind the Milestone 2 native inference runtime if targeting hardware-accelerated 336M–3B execution.
3. **Signed Release APK & Offline Verification**:
   - Build the signed release APK (`./gradlew assembleRelease`).
   - Install on an 8 GB RAM Android device, enable **Airplane Mode** (Wi-Fi + Cellular + Bluetooth off), cold-launch PagePilot, import a `.ppilot` checkpoint via SAF, verify SHA-256 & parameter counts, run generation, and confirm conversation persistence across process restarts.
