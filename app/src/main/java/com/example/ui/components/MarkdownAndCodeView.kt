package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.MessageEntity
import com.example.data.MessageFeedback
import com.example.data.MessageRole
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PilotTealDark
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class MarkdownSegment {
    data class Prose(val text: String) : MarkdownSegment()
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment()
}

private fun splitMarkdownSegments(raw: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val regex = Regex("```([a-zA-Z0-9_+-]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val before = raw.substring(lastIndex, match.range.first).trim()
            if (before.isNotEmpty()) {
                segments.add(MarkdownSegment.Prose(before))
            }
        }
        val lang = match.groupValues[1].ifBlank { "code" }
        val codeBody = match.groupValues[2].trimEnd()
        segments.add(MarkdownSegment.CodeBlock(lang, codeBody))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim()
        if (tail.isNotEmpty()) {
            segments.add(MarkdownSegment.Prose(tail))
        }
    }
    return if (segments.isEmpty()) listOf(MarkdownSegment.Prose(raw)) else segments
}

@Composable
fun ChatMessageItem(
    message: MessageEntity,
    onCopyText: (String) -> Unit,
    onEditMessage: (MessageEntity) -> Unit,
    onRegenerateMessage: (MessageEntity) -> Unit,
    onFeedbackChange: (MessageEntity, MessageFeedback) -> Unit
) {
    val isUser = message.role == MessageRole.USER.name
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val timestampStr = remember(message.createdAt) { timeFormatter.format(Date(message.createdAt)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Bubble (ChatGPT / Gemini style elevated pill)
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = timestampStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onCopyText(message.content) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = { onEditMessage(message) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("edit_message_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            // Assistant / Pipeline Verification Response
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                PagePilotLogo(size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PagePilot",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (message.isVerificationOutput) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PilotTealDark.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = PilotTealDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PIPELINE VERIFICATION (RANDOM WEIGHTS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PilotTealDark
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MarkdownContentRenderer(
                        markdown = message.content,
                        onCopyCode = onCopyText
                    )

                    if (message.tokenCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${message.tokenCount} tokens • ${String.format(Locale.US, "%.1f", message.tokensPerSec)} tok/s • Prefill ${message.prefillMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Assistant Action Row: Timestamp, Copy, Regenerate, Feedback
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = timestampStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { onCopyText(message.content) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy assistant response",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { onRegenerateMessage(message) },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("regenerate_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate response",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                val isUp = message.feedback == MessageFeedback.POSITIVE.name
                val isDown = message.feedback == MessageFeedback.NEGATIVE.name
                IconButton(
                    onClick = {
                        val next = if (isUp) MessageFeedback.NONE else MessageFeedback.POSITIVE
                        onFeedbackChange(message, next)
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isUp) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Helpful response",
                        tint = if (isUp) StatusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val next = if (isDown) MessageFeedback.NONE else MessageFeedback.NEGATIVE
                        onFeedbackChange(message, next)
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isDown) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Unhelpful response",
                        tint = if (isDown) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownContentRenderer(
    markdown: String,
    onCopyCode: (String) -> Unit
) {
    val segments = remember(markdown) { splitMarkdownSegments(markdown) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (segment in segments) {
            when (segment) {
                is MarkdownSegment.Prose -> {
                    val styled = formatInlineMarkdown(
                        text = segment.text,
                        codeColor = MaterialTheme.colorScheme.secondary,
                        quoteColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = styled,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is MarkdownSegment.CodeBlock -> {
                    SyntaxHighlightedCodeCard(
                        language = segment.language,
                        code = segment.code,
                        onCopyCode = { onCopyCode(segment.code) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SyntaxHighlightedCodeCard(
    language: String,
    code: String,
    onCopyCode: () -> Unit
) {
    val highlighted = remember(code) { highlightSyntax(code) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0E14))
            .border(1.dp, Color(0xFF262B36), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B24))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                IconButton(
                    onClick = onCopyCode,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code block",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

private fun formatInlineMarkdown(
    text: String,
    codeColor: Color,
    quoteColor: Color
): AnnotatedString = buildAnnotatedString {
    val lines = text.lines()
    lines.forEachIndexed { idx, line ->
        when {
            line.startsWith("> ") -> {
                withStyle(SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic)) {
                    append(line.removePrefix("> "))
                }
            }
            line.startsWith("### ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(line.removePrefix("### "))
                }
            }
            else -> {
                var cursor = 0
                val tokenRegex = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`)")
                for (m in tokenRegex.findAll(line)) {
                    if (m.range.first > cursor) {
                        append(line.substring(cursor, m.range.first))
                    }
                    val token = m.value
                    if (token.startsWith("**") && token.endsWith("**")) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(token.removeSurrounding("**"))
                        }
                    } else if (token.startsWith("`") && token.endsWith("`")) {
                        withStyle(SpanStyle(fontFamily = JetBrainsMonoFamily, color = codeColor, fontWeight = FontWeight.Medium)) {
                            append(token.removeSurrounding("`"))
                        }
                    }
                    cursor = m.range.last + 1
                }
                if (cursor < line.length) {
                    append(line.substring(cursor))
                }
            }
        }
        if (idx < lines.lastIndex) append("\n")
    }
}

private fun highlightSyntax(code: String): AnnotatedString = buildAnnotatedString {
    val keywordRegex = Regex("\\b(class|fun|val|var|def|import|from|return|if|else|for|while|true|false|True|False|None|null|override|suspend|object|interface)\\b")
    var lastIdx = 0
    for (m in keywordRegex.findAll(code)) {
        if (m.range.first > lastIdx) {
            append(code.substring(lastIdx, m.range.first))
        }
        withStyle(SpanStyle(color = Color(0xFF818CF8), fontWeight = FontWeight.SemiBold)) {
            append(m.value)
        }
        lastIdx = m.range.last + 1
    }
    if (lastIdx < code.length) {
        append(code.substring(lastIdx))
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
}
