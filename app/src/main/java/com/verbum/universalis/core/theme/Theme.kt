package com.verbum.universalis.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VerbunGold,
    background = Color(0xFF000000), // Pure Black for OLED
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = MutedCharcoal,
    outlineVariant = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightGold,
    background = White,
    surface = OffWhite,
    onPrimary = DeepCharcoal,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = SoftGray
)

@Composable
fun VerbumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VerbumTypography,
        content = content
    )
}
