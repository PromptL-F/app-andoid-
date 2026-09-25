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
    primary = Color(0xFFD18AFF),
    onPrimary = Color(0xFF2A0648),
    primaryContainer = Color(0xFF7436B8),
    onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFFF9DE2),
    onSecondary = Color(0xFF3A174F),
    tertiary = Color(0xFFFF7FB8),
    background = Color(0xFF0F0B14),
    onBackground = Color(0xFFEDE5F2),
    surface = Color(0xFF17111F),
    onSurface = Color(0xFFEDE5F2),
    surfaceVariant = Color(0xFF2A2133),
    onSurfaceVariant = Color(0xFFCEC2D5),
    outline = Color(0xFF96899F)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

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
