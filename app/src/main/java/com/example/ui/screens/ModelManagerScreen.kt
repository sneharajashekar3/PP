package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ModelIntegrityStatus
import com.example.data.ModelMetadataEntity
import com.example.data.ModelRuntimeStatus
import com.example.data.ModelUsageLogEntity
import com.example.domain.ModelParameterCalculator
import com.example.domain.PagePilotArchConfig
import com.example.domain.QuantizationType
import com.example.engine.HardwareTelemetrySnapshot
import com.example.engine.KvCacheStatus
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PilotTealDark
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import java.util.Locale

@Composable
fun ModelManagerScreen(
    models: List<ModelMetadataEntity>,
    loadedModel: ModelMetadataEntity?,
    hardwareSnapshot: HardwareTelemetrySnapshot,
    kvCacheStatus: KvCacheStatus,
    recentUsageLogs: List<ModelUsageLogEntity>,
    onImportSafUri: (Uri) -> Unit,
    onCreateVerifyCheckpoint: (Boolean) -> Unit,
    onAuditStorage: () -> Unit,
    onLoadModel: (ModelMetadataEntity) -> Unit,
    onUnloadModel: (String) -> Unit,
    onVerifyModel: (String) -> Unit,
    onSelectActiveModel: (ModelMetadataEntity) -> Unit,
    onDeleteModel: (ModelMetadataEntity) -> Unit
) {
    var inspectedModel by remember { mutableStateOf<ModelMetadataEntity?>(null) }

    val safDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportSafUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live 8 GB Hardware & Memory Telemetry Card
        item {
            HardwareTelemetryCard(
                hardwareSnapshot = hardwareSnapshot,
                loadedModel = loadedModel,
                kvCacheStatus = kvCacheStatus
            )
        }

        // 2. SAF Checkpoint Import & Verification Micro-Checkpoint Provisioning Bar
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
                        text = "Model Checkpoint Import & Storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Imported checkpoints are copied into app-private storage (filesDir/models/) with persistable SAF URI permissions and SHA-256 verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { safDocumentLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saf_import_model_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import via SAF")
                        }

                        OutlinedButton(
                            onClick = onAuditStorage,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("audit_storage_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audit Storage")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "PIPELINE VERIFICATION CHECKPOINTS (RANDOM WEIGHTS — NOT TRAINED AI)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCreateVerifyCheckpoint(true) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_918k_verify_button")
                        ) {
                            Text("Create 918K (V=4K)", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { onCreateVerifyCheckpoint(false) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_4_5m_verify_button")
                        ) {
                            Text("Create 4.49M (V=32K)", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 3. Registered Models List Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REGISTERED PAGEPILOT CHECKPOINTS (${models.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(models, key = { it.id }) { model ->
            ModelCheckpointCard(
                model = model,
                isCurrentlyLoaded = loadedModel?.id == model.id,
                onLoad = { onLoadModel(model) },
                onUnload = { onUnloadModel(model.id) },
                onVerify = { onVerifyModel(model.id) },
                onSelectActive = { onSelectActiveModel(model) },
                onInspectMetadata = { inspectedModel = model },
                onDelete = { onDeleteModel(model) }
            )
        }

        // 4. Local Telemetry Execution Logs
        if (recentUsageLogs.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Text(
                        text = "RECENT LOCAL EXECUTION TELEMETRY (ZERO NETWORK UPLOAD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentUsageLogs.take(5).forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${log.modelName} ${if (log.isVerificationRun) "[Verify]" else ""}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tokens: ${log.promptTokens}→${log.generatedTokens} • ${String.format(Locale.US, "%.1f", log.tokensPerSec)} tok/s • Prefill ${log.prefillMs}ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${log.usedRamMb} MB RAM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inspectedModel != null) {
        ModelMetadataInspectorDialog(
            model = inspectedModel!!,
            onDismiss = { inspectedModel = null },
            onReverify = {
                onVerifyModel(inspectedModel!!.id)
                inspectedModel = null
            }
        )
    }
}

@Composable
private fun HardwareTelemetryCard(
    hardwareSnapshot: HardwareTelemetrySnapshot,
    loadedModel: ModelMetadataEntity?,
    kvCacheStatus: KvCacheStatus
) {
    val progress = (hardwareSnapshot.deviceRamUsagePercent / 100f).coerceIn(0f, 1f)
    val barColor = when {
        hardwareSnapshot.deviceRamUsagePercent >= 85 -> StatusRose
        hardwareSnapshot.deviceRamUsagePercent >= 70 -> StatusAmber
        else -> StatusEmerald
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
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
                        tint = barColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "8 GB Device Memory & HAL Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${hardwareSnapshot.deviceRamUsagePercent}% Used",
                    style = MaterialTheme.typography.labelMedium,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricColumn(
                    label = "System RAM",
                    value = "${hardwareSnapshot.usedDeviceRamMb} / ${hardwareSnapshot.totalDeviceRamMb} MB"
                )
                TelemetryMetricColumn(
                    label = "Available RAM",
                    value = "${hardwareSnapshot.availableDeviceRamMb} MB"
                )
                TelemetryMetricColumn(
                    label = "JVM / Native Heap",
                    value = "${hardwareSnapshot.jvmAllocatedMb} / ${hardwareSnapshot.nativeHeapAllocatedMb} MB"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = if (hardwareSnapshot.isThermalThrottlingWarning) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Thermal: ${hardwareSnapshot.thermalStatusLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryStd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Battery: ${hardwareSnapshot.batteryLevelPercent}% • ${hardwareSnapshot.batteryCurrentReport}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (loadedModel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Loaded KV Cache: ${kvCacheStatus.allocatedSlots}/${kvCacheStatus.maxContextSlots} slots (${kvCacheStatus.allocatedKb} KB) • Layers: ${kvCacheStatus.numLayers} • KV Heads: ${kvCacheStatus.numKvHeads}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModelCheckpointCard(
    model: ModelMetadataEntity,
    isCurrentlyLoaded: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onVerify: () -> Unit,
    onSelectActive: () -> Unit,
    onInspectMetadata: () -> Unit,
    onDelete: () -> Unit
) {
    val integrityStatus = runCatching { ModelIntegrityStatus.valueOf(model.integrityStatus) }
        .getOrDefault(ModelIntegrityStatus.UNVERIFIED)
    val isIntegrityOk = integrityStatus == ModelIntegrityStatus.VERIFIED

    val statusBadgeColor = when {
        !isIntegrityOk -> StatusRose
        isCurrentlyLoaded -> StatusEmerald
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isCurrentlyLoaded) 1.5.dp else 1.dp,
            color = if (isCurrentlyLoaded) StatusEmerald else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(statusBadgeColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version ${model.version} • Config: ${model.configId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onInspectMetadata,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Inspect model metadata",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (model.isRandomWeightsVerification) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PilotTealDark.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = PilotTealDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RANDOM WEIGHTS — Technical Pipeline Verification Only",
                            style = MaterialTheme.typography.labelSmall,
                            color = PilotTealDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 9 Mandatory Metadata Fields Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetadataPairRow(
                    leftLabel = "Exact Parameters",
                    leftValue = String.format(Locale.US, "%,d (%s)", model.exactParameterCount, model.parameterLabel),
                    rightLabel = "Quantization",
                    rightValue = model.quantization
                )
                MetadataPairRow(
                    leftLabel = "Context Length",
                    leftValue = "${model.contextLength} tokens (V=${model.vocabSize})",
                    rightLabel = "Tied Embeddings / d_ffn",
                    rightValue = "${if (model.tieWordEmbeddings) "Tied" else "Untied"} • FFN=${model.dFfn}"
                )
                MetadataPairRow(
                    leftLabel = "File Size / Storage Req.",
                    leftValue = "${model.fileSizeBytes / 1024} KB (${model.requiredStorageMb} MB req)",
                    rightLabel = "Memory Requirement",
                    rightValue = "~${model.estimatedRamMb} MB RAM"
                )
                MetadataPairRow(
                    leftLabel = "Integrity Status",
                    leftValue = integrityStatus.label,
                    rightLabel = "Runtime Status",
                    rightValue = if (isCurrentlyLoaded) "LOADED IN RAM" else model.runtimeStatus
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SHA-256: ${model.sha256Checksum.take(24)}… • App-Private Copy: ${if (model.localFilePath.isNotEmpty()) "Yes" else "No"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentlyLoaded) {
                    Button(
                        onClick = onUnload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("unload_model_${model.id}")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unload")
                    }
                } else {
                    Button(
                        onClick = onLoad,
                        enabled = isIntegrityOk,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_model_${model.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Model")
                    }
                }

                OutlinedButton(
                    onClick = onVerify,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("verify_model_${model.id}")
                ) {
                    Icon(
                        imageVector = if (isIntegrityOk) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify")
                }

                if (!model.isSelected) {
                    TextButton(onClick = onSelectActive) {
                        Text("Set Active")
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_model_${model.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete model",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataPairRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leftLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = leftValue,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rightLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = rightValue,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ModelMetadataInspectorDialog(
    model: ModelMetadataEntity,
    onDismiss: () -> Unit,
    onReverify: () -> Unit
) {
    val arch = remember(model) {
        PagePilotArchConfig(
            configId = model.configId,
            displayName = model.name,
            stageDescription = "",
            vocabSize = model.vocabSize,
            tieWordEmbeddings = model.tieWordEmbeddings,
            numLayers = model.numLayers,
            dModel = model.dModel,
            numQueryHeads = model.numQueryHeads,
            numKvHeads = model.numKvHeads,
            dFfn = model.dFfn,
            maxContextLength = model.contextLength,
            defaultQuantization = QuantizationType.INT8,
            isVerificationOnly = model.isRandomWeightsVerification,
            expectedExactParams = model.exactParameterCount
        )
    }
    val breakdown = remember(arch) { ModelParameterCalculator.calculate(arch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checkpoint Binary & Math Inspector") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Name: ${model.name} (v${model.version})", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = buildString {
                        appendLine("• Config ID: ${model.configId}")
                        appendLine("• Vocab Size (V): ${String.format(Locale.US, "%,d", model.vocabSize)}")
                        appendLine("• Tied Embeddings: ${model.tieWordEmbeddings}")
                        appendLine("• Layers (L): ${model.numLayers} | d_model: ${model.dModel}")
                        appendLine("• GQA Heads: ${model.numQueryHeads} Q : ${model.numKvHeads} KV (d_head=${arch.headDim})")
                        appendLine("• SwiGLU d_ffn: ${model.dFfn}")
                        appendLine("• Embedding/Head Params: ${String.format(Locale.US, "%,d", breakdown.embeddingAndLmHeadParams)}")
                        appendLine("• Per-Layer Attn Params: ${String.format(Locale.US, "%,d", breakdown.perLayerAttentionParams)}")
                        appendLine("• Per-Layer FFN Params: ${String.format(Locale.US, "%,d", breakdown.perLayerSwiGluFfnParams)}")
                        appendLine("• Per-Layer RMSNorms: ${String.format(Locale.US, "%,d", breakdown.perLayerRmsNormParams)}")
                        appendLine("• Final RMSNorm: ${breakdown.finalRmsNormParams}")
                        appendLine("• Exact Calculated Total: ${String.format(Locale.US, "%,d", breakdown.exactTotalParams)}")
                        appendLine("• Math Verification: ${if (breakdown.matchesExpected) "PASSED" else "MISMATCH"}")
                        appendLine("• SHA-256: ${model.sha256Checksum}")
                        appendLine("• App-Private Path: ${model.localFilePath}")
                        append("• Persistable SAF Permission: ${model.hasPersistableUriPermission}")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onReverify) {
                Text("Re-Verify SHA-256")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
