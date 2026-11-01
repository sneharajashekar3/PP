package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppSettingsEntity
import com.example.domain.FeatureMilestoneReport
import com.example.domain.MilestoneRoadmapRegistry
import com.example.domain.MilestoneStatus
import com.example.domain.ModelParameterCalculator
import com.example.domain.ParameterBreakdown
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PilotTealDark
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import java.util.Locale

@Composable
fun ArchitectureAndMathSpecScreen() {
    val allBreakdowns = remember {
        ModelParameterCalculator.CANONICAL_CONFIGS.map { ModelParameterCalculator.calculate(it) }
    }
    val allValid = remember { ModelParameterCalculator.validateAllCanonicalConfigs() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (allValid) StatusEmerald else StatusAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Parameter-Count Validation: ${if (allValid) "ALL 6 CONFIGS VERIFIED" else "CHECK FAILED"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Every PagePilot decoder-only Transformer configuration explicitly accounts for Tied vs. Untied Embeddings (V × d_model vs. 2 × V × d_model), Grouped-Query Attention (W_q, W_k, W_v, W_o), SwiGLU FFN (3 × d_model × d_ffn), and Pre/Final RMSNorm weights. Also verified via `model_research/validate_model_params.py`.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(allBreakdowns, key = { it.config.configId }) { item ->
            ParameterBreakdownCard(item)
        }

        // LiteRT-LM Export-Validation Gate Specification Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Milestone 2: LiteRT-LM Export-Validation Gate",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildString {
                            appendLine("We do not assume arbitrary custom PyTorch architectures convert automatically to LiteRT-LM. Before selecting LiteRT-LM as our production inference engine, our custom `PagePilotForCausalLM` must pass 4 concrete validation gates on `PagePilot-Exp-100M`:")
                            appendLine("1. Graph Lowering Gate: `torch.export.export()` + `ai-edge-torch` lowering of custom RMSNorm, RoPE, static ring-buffer GQA KV cache, and SwiGLU without unsupported ATen op fallbacks.")
                            appendLine("2. Tokenizer Parity Gate: Exact token ID sequence parity between Python BPE and Android BPE.")
                            appendLine("3. Quantization Gate: 4-bit block-32 weight quantization perplexity and logit drift validation.")
                            append("4. On-Device 8 GB Benchmark Gate: Measure prefill/decode tok/s, native heap peak, thermal status, and battery draw. If blocked, `LocalModelEngine` binds to our replaceable C++/NDK backend in Android Studio.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ParameterBreakdownCard(b: ParameterBreakdown) {
    val c = b.config
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = c.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (b.matchesExpected) StatusEmerald.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = String.format(Locale.US, "%,d params", b.exactTotalParams),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (b.matchesExpected) StatusEmerald else StatusAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = c.stageDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildString {
                    appendLine("V=${String.format(Locale.US, "%,d", c.vocabSize)} | TiedEmbeds=${c.tieWordEmbeddings} | L=${c.numLayers} | d_model=${c.dModel} | Heads=${c.numQueryHeads}:${c.numKvHeads} (d_head=${c.headDim}) | SwiGLU d_ffn=${c.dFfn}")
                    appendLine("• Embedding + LM Head : ${String.format(Locale.US, "%,14d", b.embeddingAndLmHeadParams)}")
                    appendLine("• Per-Layer Attention : ${String.format(Locale.US, "%,14d", b.perLayerAttentionParams)}")
                    appendLine("• Per-Layer SwiGLU FFN: ${String.format(Locale.US, "%,14d", b.perLayerSwiGluFfnParams)}")
                    appendLine("• Per-Layer RMSNorms  : ${String.format(Locale.US, "%,14d", b.perLayerRmsNormParams)}")
                    appendLine("• All ${c.numLayers} Layers Total  : ${String.format(Locale.US, "%,14d", b.allLayersTotalParams)}")
                    appendLine("• Final RMSNorm       : ${String.format(Locale.US, "%,14d", b.finalRmsNormParams)}")
                    append("• Est. Footprint      : INT4 ~${b.estimatedWeightMbInt4} MB | FP16 ~${b.estimatedWeightMbFp16} MB | Runtime RAM ~${b.estimatedTotalRuntimeRamMb} MB")
                },
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    onUpdateSettings: ((AppSettingsEntity) -> AppSettingsEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Mode Selection (Light, Dark, System like ChatGPT)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Appearance & Theme (ChatGPT / Gemini Dual-Theme)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = settings.themeMode == "LIGHT",
                            onClick = { onUpdateSettings { it.copy(themeMode = "LIGHT") } },
                            label = { Text("Light") },
                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("theme_light_chip")
                        )
                        FilterChip(
                            selected = settings.themeMode == "DARK",
                            onClick = { onUpdateSettings { it.copy(themeMode = "DARK") } },
                            label = { Text("Dark") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("theme_dark_chip")
                        )
                        FilterChip(
                            selected = settings.themeMode == "SYSTEM",
                            onClick = { onUpdateSettings { it.copy(themeMode = "SYSTEM") } },
                            label = { Text("System") },
                            leadingIcon = { Icon(Icons.Default.SettingsBrightness, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("theme_system_chip")
                        )
                    }
                }
            }
        }

        // LocalModelEngine Sampling & 8 GB Guardrail Controls
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LocalModelEngine Sampling & 8 GB RAM Guardrails",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SettingSliderItem(
                        title = "Temperature",
                        valueText = String.format(Locale.US, "%.2f", settings.temperature),
                        value = settings.temperature,
                        valueRange = 0.1f..1.5f,
                        onValueChange = { v -> onUpdateSettings { it.copy(temperature = v) } }
                    )

                    SettingSliderItem(
                        title = "Top-P (Nucleus Sampling)",
                        valueText = String.format(Locale.US, "%.2f", settings.topP),
                        value = settings.topP,
                        valueRange = 0.1f..1.0f,
                        onValueChange = { v -> onUpdateSettings { it.copy(topP = v) } }
                    )

                    SettingSliderItem(
                        title = "Top-K Candidates",
                        valueText = "${settings.topK}",
                        value = settings.topK.toFloat(),
                        valueRange = 1f..100f,
                        onValueChange = { v -> onUpdateSettings { it.copy(topK = v.toInt()) } }
                    )

                    SettingSliderItem(
                        title = "Repetition Penalty",
                        valueText = String.format(Locale.US, "%.2f", settings.repetitionPenalty),
                        value = settings.repetitionPenalty,
                        valueRange = 1.0f..1.5f,
                        onValueChange = { v -> onUpdateSettings { it.copy(repetitionPenalty = v) } }
                    )

                    SettingSliderItem(
                        title = "Max Output Tokens",
                        valueText = "${settings.maxTokens} tokens",
                        value = settings.maxTokens.toFloat(),
                        valueRange = 16f..1024f,
                        onValueChange = { v -> onUpdateSettings { it.copy(maxTokens = v.toInt()) } }
                    )

                    SettingSliderItem(
                        title = "8 GB Device RAM Safety Ceiling",
                        valueText = "${settings.ramSafetyCeilingPct}% max system RAM",
                        value = settings.ramSafetyCeilingPct.toFloat(),
                        valueRange = 60f..92f,
                        onValueChange = { v -> onUpdateSettings { it.copy(ramSafetyCeilingPct = v.toInt()) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSliderItem(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
fun AboutAndMilestonesScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StatusEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PagePilot Milestone 1 Deliverable & Honest Status Report",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Built for Google AI Studio Build compatibility (Single-Activity, Single-Module, Kotlin, Jetpack Compose, Room SQLite, Zero C++/NDK dependencies in hosted mode) and ready for ZIP export into Android Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(MilestoneRoadmapRegistry.FEATURE_REPORTS, key = { it.featureName }) { report ->
            MilestoneReportCard(report)
        }

        // Android Studio Continuation & Final Signed APK Workflow Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ZIP Export, Android Studio Continuation & Signed Release APK Workflow",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildString {
                            appendLine("1. Export Project as ZIP: Use Google AI Studio's Export ZIP option to download the complete Gradle project including `model_research/validate_model_params.py` and `model_research/pagepilot_transformer.py`.")
                            appendLine("2. Independent PyTorch Training: Run `python3 model_research/validate_model_params.py` and train `PagePilot-Exp-100M` (100,092,672 params) and `PagePilot-Mobile-336M` (336,118,784 params) on your GPU workstation.")
                            appendLine("3. LiteRT-LM Export-Validation Gate: Validate conversion of custom RMSNorm/RoPE/GQA/SwiGLU layers to `.litertlm` or bind a C++/NDK JNI implementation to `LocalModelEngine` in Android Studio.")
                            appendLine("4. Build Signed Release APK: Run `./gradlew assembleRelease` in Android Studio with your release signing keystore.")
                            append("5. Airplane-Mode Verification: Install APK on an 8 GB RAM Android device, enable Airplane Mode, launch PagePilot, import your `.ppilot` checkpoint via SAF, and verify offline token generation and Room persistence across restarts.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneReportCard(report: FeatureMilestoneReport) {
    val isComplete = report.status == MilestoneStatus.COMPLETED_M1
    val badgeColor = when (report.status) {
        MilestoneStatus.COMPLETED_M1 -> StatusEmerald
        MilestoneStatus.VALIDATION_GATE_M2 -> PilotTealDark
        MilestoneStatus.PLANNED_EXTENSION_M3 -> StatusAmber
        MilestoneStatus.APK_RELEASE_M4 -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.featureName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isComplete) "Functional M1" else "Deferred",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.technicalSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
