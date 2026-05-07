package com.verbum.universalis.core.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightGold,
    background = DeepCharcoal,
    surface = DarkGray,
    onPrimary = DeepCharcoal,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = MutedCharcoal
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
