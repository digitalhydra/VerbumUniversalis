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
        typography = androidx.compose.material3.Typography(
            displayLarge = ContentTypography.displayLarge,
            displayMedium = ContentTypography.displayMedium,
            displaySmall = ContentTypography.displaySmall,
            headlineLarge = ContentTypography.headlineLarge,
            headlineMedium = ContentTypography.headlineMedium,
            headlineSmall = ContentTypography.headlineSmall,
            titleLarge = UITypography.titleLarge,
            titleMedium = UITypography.titleMedium,
            titleSmall = UITypography.titleSmall,
            bodyLarge = ContentTypography.bodyLarge,
            bodyMedium = ContentTypography.bodyMedium,
            bodySmall = ContentTypography.bodySmall,
            labelLarge = UITypography.labelLarge,
            labelMedium = UITypography.labelMedium,
            labelSmall = UITypography.labelSmall
        ),
        content = content
    )
}
