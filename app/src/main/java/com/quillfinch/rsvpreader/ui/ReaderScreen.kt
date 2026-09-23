package com.quillfinch.rsvpreader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.quillfinch.rsvpreader.ReaderState
import com.quillfinch.rsvpreader.data.ReaderSettings
import com.quillfinch.rsvpreader.rsvp.Word
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    state: ReaderState,
    settings: ReaderSettings,
    onTap: () -> Unit,
    onSkip: (Int) -> Unit,
    onRestart: () -> Unit,
    onScrub: (Float) -> Unit,
    onWpmChange: (Int) -> Unit,
    onExit: () -> Unit,
) {
    val background = Color(settings.backgroundColor)
    val textColor = Color(settings.textColor)
    val focusColor = Color(settings.focusColor)

    ImmersiveEffect()
    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
    ) {
        ScrubBar(
            progress = if (state.wordCount == 0) 0f else state.index.toFloat() / state.wordCount,
            accent = focusColor,
            onScrub = onScrub,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 8.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Exit", tint = textColor.copy(alpha = 0.6f))
        }

        state.word?.let { word ->
            RsvpWord(
                word = word,
                textColor = textColor,
                focusColor = focusColor,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (!state.playing && !state.finished) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 96.dp)
                    .size(88.dp),
            )
        }

        if (state.finished) {
            Text(
                text = "Done!",
                color = textColor.copy(alpha = 0.8f),
                fontSize = 22.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 96.dp),
            )
        }

        AnimatedVisibility(
            visible = !state.playing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ControlsPanel(
                state = state,
                wpm = settings.wpm,
                textColor = textColor,
                focusColor = focusColor,
                onSkip = onSkip,
                onRestart = onRestart,
                onWpmChange = onWpmChange,
            )
        }
    }
}

/** Draws the word with its focus (ORP) letter held exactly at the center of the screen. */
@Composable
private fun RsvpWord(
    word: Word,
    textColor: Color,
    focusColor: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val style = remember(textColor) {
        TextStyle(
            fontSize = 64.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
    val charWidth = measurer.measure("M", style).size.width
    val annotated = remember(word.text, textColor, focusColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = textColor)) { append(word.text.substring(0, word.orpIndex)) }
            withStyle(SpanStyle(color = focusColor, fontWeight = FontWeight.Bold)) { append(word.text[word.orpIndex]) }
            withStyle(SpanStyle(color = textColor)) { append(word.text.substring(word.orpIndex + 1)) }
        }
    }
    Text(
        text = annotated,
        style = style,
        maxLines = 1,
        modifier = modifier.offset { IntOffset(-(word.orpIndex * charWidth + charWidth / 2), 0) },
    )
}

@Composable
private fun ControlsPanel(
    state: ReaderState,
    wpm: Int,
    textColor: Color,
    focusColor: Color,
    onSkip: (Int) -> Unit,
    onRestart: () -> Unit,
    onWpmChange: (Int) -> Unit,
) {
    val secondsLeft = if (wpm <= 0) 0 else (state.wordCount - state.index) * 60 / wpm
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (state.finished) "${state.wordCount} words read" else "${formatTime(secondsLeft)} left",
            color = textColor.copy(alpha = 0.7f),
            fontSize = 13.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onSkip(-1) }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous sentence", tint = textColor, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onRestart) {
                Icon(Icons.Filled.Refresh, contentDescription = "Restart", tint = textColor, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { onSkip(1) }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next sentence", tint = textColor, modifier = Modifier.size(36.dp))
            }
        }
        Slider(
            value = wpm.toFloat(),
            onValueChange = { onWpmChange(it.roundToInt()) },
            valueRange = 100f..1000f,
            steps = 35,
            colors = SliderDefaults.colors(
                thumbColor = focusColor,
                activeTrackColor = focusColor,
            ),
        )
        Text(text = "$wpm WPM", color = textColor.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
private fun ScrubBar(
    progress: Float,
    accent: Color,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableIntStateOf(0) }
    fun update(x: Float) {
        if (width > 0) onScrub((x / width).coerceIn(0f, 1f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { width = it.width }
            .pointerInput(Unit) { detectTapGestures { update(it.x) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { update(it.x) }) { change, _ ->
                    change.consume()
                    update(change.position.x)
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(accent.copy(alpha = 0.25f))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(accent)
        )
    }
}

@Composable
private fun ImmersiveEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}
