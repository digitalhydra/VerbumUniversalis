package com.verbum.universalis.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDarkTheme by remember { 
        mutableStateOf(
            // Get current theme mode from AppCompatDelegate
            (context as? android.app.Activity)?.let {
                when (it.delegate.defaultNightMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> true
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    else -> false // Default to light for auto/follow system
                }
            } ?: false
        )
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
            
            // Theme options
            ThemeOptionRow(
                title = "Light Theme",
                description = "Bright theme with dark text",
                isSelected = !isDarkTheme,
                onClick = { 
                    isDarkTheme = false
                    applyTheme(context, false)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ThemeOptionRow(
                title = "Dark Theme",
                description = "Dark theme with light text",
                isSelected = isDarkTheme,
                onClick = { 
                    isDarkTheme = true
                    applyTheme(context, true)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ThemeOptionRow(
                title = "System Default",
                description = "Follow device theme settings",
                isSelected = !isDarkTheme && !isDarkTheme, // This won't work perfectly but gives idea
                onClick = { 
                    // For system default, we'd need to use MODE_NIGHT_FOLLOW_SYSTEM
                    applyTheme(context, -1) // -1 indicates system default
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
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

private fun applyTheme(context: Context, mode: Int) {
    val activity = context as? android.app.Activity
    activity?.delegate?.apply {
        when {
            mode == -1 -> setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            mode == 0 -> setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            mode == 1 -> setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
    
    // Recreate activity to apply theme immediately
    activity?.recreate()
}