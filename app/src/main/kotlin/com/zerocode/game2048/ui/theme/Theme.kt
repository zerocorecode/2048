package com.zerocode.game2048.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF776E65)
val Paper = Color(0xFFFAF8EF)
val BoardColor = Color(0xFFBBADA0)
val EmptyColor = Color(0xFFCDC1B4)
val Accent = Color(0xFFC66D3B)
val Coral = Color(0xFFF67C5F)

private val GameColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFFFDF8),
    onSurface = Ink,
    outline = Color(0xFFD7CABE),
)

@Composable
fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GameColors, content = content)
}
