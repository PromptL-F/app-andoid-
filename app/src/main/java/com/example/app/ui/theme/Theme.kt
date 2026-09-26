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
    primary = EmberOrange,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = EmberOrangeDark,
    onPrimaryContainer = Color(0xFFFFDCC7),
    secondary = Color(0xFFE07A4A),
    onSecondary = Color(0xFF2E1206),
    tertiary = EmberRed,
    onTertiary = Color(0xFFFFFFFF),
    background = HadesBackground,
    onBackground = HadesText,
    surface = HadesSurface,
    onSurface = HadesText,
    surfaceVariant = HadesSurfaceVariant,
    onSurfaceVariant = HadesTextMuted,
    outline = Color(0xFF8A8074)
)

private val LightColorScheme = lightColorScheme(
    primary = Orange40,
    secondary = OrangeGrey40,
    tertiary = Red40

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
