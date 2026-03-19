package com.example.hangsha_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Green50,
    onPrimary = Cream5,
    primaryContainer = Sand20,
    onPrimaryContainer = Ink90,
    secondary = Coral60,
    onSecondary = Cream5,
    secondaryContainer = Peach20,
    onSecondaryContainer = Ink90,
    background = Cream10,
    onBackground = Ink90,
    surface = Cream5,
    onSurface = Ink90,
    surfaceVariant = Sand30,
    onSurfaceVariant = Ink60
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
    onBackground = Cream10,
    surface = Ink90,
    onSurface = Cream10,
    surfaceVariant = Forest20,
    onSurfaceVariant = Sand20
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
