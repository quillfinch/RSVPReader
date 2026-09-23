package com.quillfinch.rsvpreader.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit

/**
 * Draws one word with its focus (ORP) letter held at the exact center of the
 * available width, so the eye stays still no matter how long the word is.
 */
@Composable
fun PivotWord(
    text: String,
    orpIndex: Int,
    fontSize: TextUnit,
    textColor: Color,
    focusColor: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        color = textColor,
    )
    val annotated = remember(text, textColor, focusColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = textColor)) { append(text.substring(0, orpIndex)) }
            withStyle(SpanStyle(color = focusColor, fontWeight = FontWeight.Bold)) { append(text[orpIndex]) }
            withStyle(SpanStyle(color = textColor)) { append(text.substring(orpIndex + 1)) }
        }
    }
    // Monospace glyphs share one advance, so the focus letter's center sits at
    // orp * charWidth + charWidth / 2 inside the word; centering the word box and
    // shifting back by half the leftover width pins that point to the middle.
    val charWidth = measurer.measure("M", style).size.width
    val wordWidth = measurer.measure(annotated, style).size.width
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = annotated,
            style = style,
            maxLines = 1,
            modifier = Modifier.offset {
                IntOffset((wordWidth - (orpIndex * 2 + 1) * charWidth) / 2, 0)
            },
        )
    }
}
