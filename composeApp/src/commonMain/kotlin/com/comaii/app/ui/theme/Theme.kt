package com.comaii.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun parseColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return Color(("FF$cleanHex").toLong(16))
}

fun companyColorScheme(
    primaryHex: String = "#FF6B00",
    secondaryHex: String = "#FFFFFF",
    accentHex: String = "#333333",
) = lightColorScheme(
    primary = parseColor(primaryHex),
    secondary = parseColor(accentHex),
    background = parseColor(secondaryHex),
    surface = parseColor(secondaryHex),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = parseColor(accentHex),
    onSurface = parseColor(accentHex),
)

@Composable
fun ComaiiTheme(
    primaryColor: String = "#FF6B00",
    secondaryColor: String = "#FFFFFF",
    accentColor: String = "#333333",
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = companyColorScheme(primaryColor, secondaryColor, accentColor),
        content = content,
    )
}
