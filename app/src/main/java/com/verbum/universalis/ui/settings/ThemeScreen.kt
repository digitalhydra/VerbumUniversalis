package com.verbum.universalis.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.verbum.universalis.data.json.FileManager
import com.verbum.universalis.data.json.UserSettings
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }
    
    // Load current theme from UserSettings
    var currentTheme by remember {
        mutableStateOf(runBlocking {
            fileManager.loadSettings()?.theme ?: "system"
        })
    }
    
    // Map theme string to isDarkTheme boolean
    // "system" defaults to false (light) for simplicity
    var isDarkTheme by remember(currentTheme) {
        mutableStateOf(currentTheme == "dark")
    }
    
    // Load theme from settings on first composition
    LaunchedEffect(Unit) {
        val settings = fileManager.loadSettings()
        currentTheme = settings?.theme ?: "system"
        isDarkTheme = settings?.theme == "dark"
        
        // Apply the saved theme via AppCompatDelegate
        applyThemeFromSettings(settings?.theme ?: "system")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Theme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Select Theme",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            ThemeOptionRow(
                title = "Light Theme",
                description = "Bright theme with dark text",
                isSelected = currentTheme == "light",
                onClick = {
                    currentTheme = "light"
                    isDarkTheme = false
                    saveAndApplyTheme(context, fileManager, "light")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ThemeOptionRow(
                title = "Dark Theme",
                description = "Dark theme with light text",
                isSelected = currentTheme == "dark",
                onClick = {
                    currentTheme = "dark"
                    isDarkTheme = true
                    saveAndApplyTheme(context, fileManager, "dark")
                }
            )
        }
    }
}

private fun saveAndApplyTheme(context: Context, fileManager: FileManager, theme: String) {
    // Save theme to UserSettings
    val existingSettings = fileManager.loadSettings()
    val updatedSettings = existingSettings?.copy(theme = theme) ?: UserSettings(theme = theme)
    fileManager.saveSettings(updatedSettings)
    
    // Apply theme via AppCompatDelegate
    applyThemeFromSettings(theme)
}

private fun applyThemeFromSettings(theme: String) {
    val mode = when (theme) {
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}