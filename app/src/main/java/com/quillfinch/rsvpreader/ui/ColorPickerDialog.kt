package com.quillfinch.rsvpreader.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SWATCHES = listOf(
    0xFFFFFFFF, 0xFFE6E1E9, 0xFF9E9E9E, 0xFF444746, 0xFF1C1B1F, 0xFF000000,
    0xFFFF5252, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0,
)

@Composable
fun ColorPickerDialog(
    title: String,
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    var hue by remember { mutableFloatStateOf(hueOf(initial)) }
    var saturation by remember { mutableFloatStateOf(saturationOf(initial)) }
    var brightness by remember { mutableFloatStateOf(brightnessOf(initial)) }
    val current = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SatValArea(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChange = { s, v -> saturation = s; brightness = v },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
                HueBar(
                    hue = hue,
                    onChange = { hue = it },
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(current, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(text = hexOf(current), style = MaterialTheme.typography.bodyMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SWATCHES.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { swatch ->
                                val color = Color(swatch)
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (current.toArgb() == swatch.toInt()) 2.dp else 1.dp,
                                            color = if (current.toArgb() == swatch.toInt()) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape,
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures {
                                                val hsv = FloatArray(3)
                                                AndroidColor.colorToHSV(color.toArgb(), hsv)
                                                hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(current) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SatValArea(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun update(position: androidx.compose.ui.geometry.Offset) {
        if (size.width == 0 || size.height == 0) return
        onChange(
            (position.x / size.width).coerceIn(0f, 1f),
            1f - (position.y / size.height).coerceIn(0f, 1f),
        )
    }
    Box(
        modifier
            .onSizeChanged { size = it }
            .background(
                Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))),
                RoundedCornerShape(12.dp),
            )
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
                RoundedCornerShape(12.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .pointerInput(hue) { detectTapGestures { update(it) } }
            .pointerInput(hue) {
                detectDragGestures(onDragStart = { update(it) }) { change, _ ->
                    change.consume()
                    update(change.position)
                }
            }
    ) {
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (saturation * size.width).roundToInt() - 10,
                        ((1f - brightness) * size.height).roundToInt() - 10,
                    )
                }
                .size(20.dp)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun update(position: androidx.compose.ui.geometry.Offset) {
        if (size.width == 0) return
        onChange(position.x.toFloat() / size.width * 360f)
    }
    val rainbow = remember {
        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }
    }
    Box(
        modifier
            .onSizeChanged { size = it }
            .background(Brush.horizontalGradient(rainbow), RoundedCornerShape(13.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { update(it) }) { change, _ ->
                    change.consume()
                    update(change.position)
                }
            }
    ) {
        Box(
            Modifier
                .offset { IntOffset((hue / 360f * size.width).roundToInt() - 10, -1) }
                .size(20.dp)
                .border(3.dp, Color.White, CircleShape)
        )
    }
}

private fun hueOf(color: Color): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

private fun saturationOf(color: Color): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return hsv[1]
}

private fun brightnessOf(color: Color): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return hsv[2]
}

private fun hexOf(color: Color): String = String.format("#%06X", 0xFFFFFF and color.toArgb())
