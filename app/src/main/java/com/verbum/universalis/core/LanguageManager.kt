package com.verbum.universalis.core

import android.content.Context
import android.content.res.Configuration
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
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
        updateLocale(context, lang)
    }

    fun setLanguage(context: Context, lang: String) {
        val settings = fileManager.loadSettings() ?: UserSettings()
        fileManager.saveSettings(settings.copy(appLanguage = lang))
        _appLanguage.value = lang
        updateLocale(context, lang)
    }

    private fun updateLocale(context: Context, lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
