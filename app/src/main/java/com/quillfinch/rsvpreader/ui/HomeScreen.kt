package com.quillfinch.rsvpreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quillfinch.rsvpreader.data.ReaderSettings
import com.quillfinch.rsvpreader.data.SettingsRepository
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    text: String,
    fileName: String?,
    busy: Boolean,
    error: String?,
    settings: ReaderSettings,
    onTextChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onStart: () -> Unit,
    onWpmChange: (Int) -> Unit,
    onTextColorChange: (Long) -> Unit,
    onBackgroundColorChange: (Long) -> Unit,
    onFocusColorChange: (Long) -> Unit,
    onResetColors: () -> Unit,
    onErrorShown: () -> Unit,
) {
    var colorDialog by remember { mutableStateOf<String?>(null) }
    val wordCount = remember(text) { text.split(Regex("\\s+")).count { it.isNotBlank() } }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "RSVP Reader",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste text here, or open a file below") },
                minLines = 5,
                maxLines = 10,
            )

            fileName?.let {
                Text(
                    text = "Opened: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPickFile,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open file")
                }
                Button(
                    onClick = onStart,
                    enabled = wordCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start reading")
                }
            }

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (wordCount > 0) {
                Text(
                    text = "$wordCount words · about ${formatDuration(wordCount, settings.wpm)} at ${settings.wpm} WPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            Text("Speed", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = settings.wpm.toFloat(),
                onValueChange = { onWpmChange(it.roundToInt()) },
                valueRange = SettingsRepository.MIN_WPM.toFloat()..SettingsRepository.MAX_WPM.toFloat(),
                steps = (SettingsRepository.MAX_WPM - SettingsRepository.MIN_WPM) / 25 - 1,
            )

            HorizontalDivider()

            Text("Colors", style = MaterialTheme.typography.titleMedium)
            ColorRow("Text color", Color(settings.textColor)) { colorDialog = "text" }
            ColorRow("Background", Color(settings.backgroundColor)) { colorDialog = "background" }
            ColorRow("Focus letter", Color(settings.focusColor)) { colorDialog = "focus" }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(settings.backgroundColor), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(settings.textColor))) { append("read") }
                        withStyle(SpanStyle(color = Color(settings.focusColor), fontWeight = FontWeight.Bold)) { append("i") }
                        withStyle(SpanStyle(color = Color(settings.textColor))) { append("ng") }
                    },
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            TextButton(onClick = onResetColors) { Text("Reset colors to defaults") }
        }
    }

    when (colorDialog) {
        "text" -> ColorPickerDialog(
            title = "Text color",
            initial = Color(settings.textColor),
            onDismiss = { colorDialog = null },
            onConfirm = { onTextColorChange(it.toArgb().toLong() and 0xFFFFFFFFL); colorDialog = null },
        )
        "background" -> ColorPickerDialog(
            title = "Background",
            initial = Color(settings.backgroundColor),
            onDismiss = { colorDialog = null },
            onConfirm = { onBackgroundColorChange(it.toArgb().toLong() and 0xFFFFFFFFL); colorDialog = null },
        )
        "focus" -> ColorPickerDialog(
            title = "Focus letter",
            initial = Color(settings.focusColor),
            onDismiss = { colorDialog = null },
            onConfirm = { onFocusColorChange(it.toArgb().toLong() and 0xFFFFFFFFL); colorDialog = null },
        )
    }
}

@Composable
private fun ColorRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
        )
    }
}

private fun formatDuration(wordCount: Int, wpm: Int): String {
    val minutes = wordCount / wpm.coerceAtLeast(1)
    return when {
        minutes < 1 -> "under a minute"
        minutes == 1 -> "1 minute"
        minutes < 60 -> "$minutes minutes"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}
