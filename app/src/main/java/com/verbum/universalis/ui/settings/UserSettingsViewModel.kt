package com.verbum.universalis.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val app: Application
) : ViewModel() {

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = fileManager.loadSettings() ?: UserSettings()
            _userSettings.value = settings
        }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch {
            val updated = _userSettings.value.copy(theme = themeName)
            _userSettings.value = updated
            fileManager.saveSettings(updated)
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch {
            val updated = _userSettings.value.copy(language = langCode)
            _userSettings.value = updated
            fileManager.saveSettings(updated)
        }
    }

    fun updateLastRead(passage: String) {
        viewModelScope.launch {
            val updated = _userSettings.value.copy(lastReadPassage = passage)
            _userSettings.value = updated
            fileManager.saveSettings(updated)
        }
    }
}
