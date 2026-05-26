package com.verbum.universalis.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    private val fileManager: FileManager
) {
    private val _appLanguage = MutableStateFlow("en")
    val appLanguage = _appLanguage.asStateFlow()

    fun initialize(context: Context) {
        val settings = fileManager.loadSettings()
        val lang = settings?.appLanguage ?: "en"
        _appLanguage.value = lang
        applyLanguage(context, lang)
    }

    fun setLanguage(context: Context, lang: String) {
        val settings = fileManager.loadSettings() ?: UserSettings()
        fileManager.saveSettings(settings.copy(appLanguage = lang))
        _appLanguage.value = lang
        applyLanguage(context, lang)
    }

    private fun applyLanguage(context: Context, lang: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        // Manual override for current session to ensure Compose sees the change
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
