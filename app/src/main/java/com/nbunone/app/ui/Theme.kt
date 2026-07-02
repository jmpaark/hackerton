package com.nbunone.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF4F46E5)
val IndigoLight = Color(0xFFEEF2FF)
val Amber = Color(0xFFF59E0B)
val Green = Color(0xFF10B981)
val Red = Color(0xFFEF4444)
val Slate = Color(0xFF64748B)

val ChartColors = listOf(
    Color(0xFF4F46E5), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFF06B6D4)
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Slate,
    surface = Color.White,
    background = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFF1F5F9),
    error = Red
)

@Composable
fun NbunoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
