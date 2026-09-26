package com.example.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = VioletDeep,
    onPrimaryContainer = Color(0xFFF2E6FF),
    secondary = NeonPurple,
    onSecondary = Color(0xFF160D2B),
    tertiary = NeonBlue,
    onTertiary = Color(0xFFFFFFFF),
    background = HadesBackground,
    onBackground = HadesText,
    surface = HadesSurface,
    onSurface = HadesText,
    surfaceVariant = HadesSurfaceVariant,
    onSurfaceVariant = HadesTextMuted,
    surfaceContainer = HadesSurface,
    surfaceContainerHigh = HadesSurfaceRaised,
    surfaceContainerHighest = HadesSurfaceBright,
    outline = Color(0xFF6F5A8C),
    outlineVariant = Color(0xFF3D2D55)
)

private val LightColorScheme = lightColorScheme(
    primary = Violet40,
    secondary = Lavender40,
    tertiary = Magenta40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
