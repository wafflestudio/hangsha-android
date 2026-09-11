package com.example.hangsha_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Green50,
    onPrimary = Cream5,
    primaryContainer = Sand20,
    onPrimaryContainer = Ink90,
    secondary = Coral60,
    onSecondary = Cream5,
    secondaryContainer = Peach20,
    onSecondaryContainer = Ink90,
    background = Cream5,
    onBackground = Ink90,
    surface = PureWhite,
    onSurface = Ink90,
    surfaceVariant = Cream10,
    onSurfaceVariant = Ink60,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Cream5,
    surfaceContainer = Cream10,
    surfaceContainerHigh = Sand20,
    surfaceContainerHighest = Sand30,
    outline = Color(0xFF767A74),
    outlineVariant = Color(0xFFD8D9D3),
    error = Color(0xFFBA1A1A),
    onError = PureWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Mint70,
    onPrimary = Ink90,
    primaryContainer = Forest30,
    onPrimaryContainer = Cream10,
    secondary = Coral50,
    onSecondary = Ink90,
    secondaryContainer = Brick30,
    onSecondaryContainer = Cream10,
    background = Ink100,
    onBackground = Cream5,
    surface = Ink90,
    onSurface = Cream5,
    surfaceVariant = Forest20,
    onSurfaceVariant = Sand30,
    surfaceContainerLowest = Color(0xFF0B0E0C),
    surfaceContainerLow = Color(0xFF151916),
    surfaceContainer = Color(0xFF1C211E),
    surfaceContainerHigh = Color(0xFF272C28),
    surfaceContainerHighest = Color(0xFF323733),
    outline = Color(0xFF8C938D),
    outlineVariant = Color(0xFF3C443F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun HangshaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
