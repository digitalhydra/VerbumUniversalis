package com.verbum.universalis.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    primary = VerbunGold,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onPrimary = DarkTextPrimary // Text on Gold buttons
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    primary = VerbunGold,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onPrimary = LightTextPrimary
)

@Composable
fun VerbumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        // Note: Typography setup requires font files. 
        // We'll apply SourceSerifPro and Inter once the font files are added to res/font.
        // For now, using default Material3 typography.
        content = content
    )
}
