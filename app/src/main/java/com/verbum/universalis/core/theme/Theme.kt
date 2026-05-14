package com.verbum.universalis.core.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/**
 * Theme manager that holds the current theme state and can be observed by UI.
 * This allows theme changes to be applied without restarting the app.
 * Singleton instance shared across all Activities.
 */
object ThemeManager {
    private val _theme = MutableStateFlow("system")
    val theme = _theme.asStateFlow()
    
    private var initialized = false
    
    fun initialize(context: Context) {
        if (!initialized) {
            _theme.value = try {
                FileManager(context).loadSettings()?.theme ?: "system"
            } catch (e: Exception) {
                "system"
            }
            initialized = true
        }
    }
    
    fun setTheme(context: Context, theme: String) {
        try {
            val settings = FileManager(context).loadSettings() ?: UserSettings()
            val updated = settings.copy(theme = theme)
            FileManager(context).saveSettings(updated)
            _theme.value = theme
            
            // Also apply via AppCompatDelegate
            val mode = when (theme) {
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}

@Composable
fun VerbumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Use the global ThemeManager singleton to observe theme changes
    // This allows theme changes to apply without restarting the app
    val themeState by ThemeManager.theme.collectAsState()
    
    val isDark = when (themeState) {
        "dark" -> true
        "light" -> false
        else -> darkTheme // fallback to parameter or system
    }
    
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VerbumTypography,
        content = content
    )
}