package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AcademicEmerald,
    secondary = AccentAzure,
    tertiary = GoldAccent,
    background = SlateDarkBg,
    surface = SlateSurfaceDark,
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFFEADDFF),
    onBackground = SoftOffWhite,
    onSurface = SoftOffWhite,
    surfaceVariant = Color(0xFF35333B),
    onSurfaceVariant = NeutralSilver
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicEmeraldLight,
    secondary = AccentAzureLight,
    tertiary = GoldAccentLight,
    background = SlateLightBg,
    surface = SlateSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDarkest,
    onSurface = TextDarkest,
    surfaceVariant = Color(0xFFEADDFF),
    onSurfaceVariant = NeutralCharcoal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
