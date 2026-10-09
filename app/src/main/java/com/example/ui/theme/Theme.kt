package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NovaPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = NovaPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = NovaSecondary,
    onSecondary = Color.Black,
    secondaryContainer = NovaSecondaryDark,
    onSecondaryContainer = Color.White,
    tertiary = NovaTertiary,
    onTertiary = Color.White,
    background = SpaceBackground,
    onBackground = TextPrimaryDark,
    surface = SpaceSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = SpaceCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NovaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = NovaPrimaryDark,
    secondary = NovaSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = NovaTertiary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding colors by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
