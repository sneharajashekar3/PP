package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.ModelMetadataEntity
import com.example.engine.HardwareTelemetrySnapshot
import com.example.ui.theme.PilotIndigoDark
import com.example.ui.theme.PilotTealDark
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

/**
 * Original PagePilot Geometric Emblem drawn via Compose Canvas.
 * Combines an origami paper-wing silhouette with a neural compass prism.
 */
@Composable
fun PagePilotLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(PilotIndigoDark.copy(alpha = 0.7f), PilotTealDark.copy(alpha = 0.7f))
                ),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height

            val outerWing = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                lineTo(w * 0.92f, h * 0.46f)
                lineTo(w * 0.62f, h * 0.58f)
                lineTo(w * 0.50f, h * 0.92f)
                lineTo(w * 0.38f, h * 0.58f)
                lineTo(w * 0.08f, h * 0.46f)
                close()
            }
            drawPath(
                path = outerWing,
                brush = Brush.linearGradient(
                    colors = listOf(PilotTealDark, PilotIndigoDark),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            val rightFacet = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.82f, h * 0.45f)
                lineTo(w * 0.50f, h * 0.55f)
                close()
            }
            drawPath(path = rightFacet, color = Color(0xFFF8FAFC))

            val leftFacet = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.18f, h * 0.45f)
                lineTo(w * 0.50f, h * 0.55f)
                close()
            }
            drawPath(path = leftFacet, color = Color(0xFFA5B4FC))
        }
    }
}

@Composable
fun PagePilotTopBar(
    title: String,
    loadedModel: ModelMetadataEntity?,
    hardwareSnapshot: HardwareTelemetrySnapshot,
    onOpenDrawer: () -> Unit,
    onOpenModelManager: () -> Unit,
    onNewChat: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("open_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Center Active Model Status Pill (Google AI Edge Gallery & ChatGPT inspired)
                val statusColor = when {
                    loadedModel == null -> StatusAmber
                    loadedModel.isRandomWeightsVerification -> PilotTealDark
                    else -> StatusEmerald
                }
                val modelLabel = when {
                    loadedModel == null -> "No Model Loaded • Tap to Load"
                    loadedModel.isRandomWeightsVerification -> "${loadedModel.name} [Verify]"
                    else -> "${loadedModel.name} (${loadedModel.parameterLabel})"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenModelManager)
                        .testTag("active_model_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = modelLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "RAM: ${hardwareSnapshot.usedDeviceRamMb}/${hardwareSnapshot.totalDeviceRamMb} MB (${hardwareSnapshot.deviceRamUsagePercent}%) • Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (loadedModel == null) Icons.Outlined.WarningAmber else if (loadedModel.isRandomWeightsVerification) Icons.Outlined.Science else Icons.Default.Memory,
                            contentDescription = "Manage active model",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.testTag("top_new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "Start new chat",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
