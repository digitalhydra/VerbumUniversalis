package com.verbum.universalis.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.verbum.universalis.core.theme.VerbumTheme

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    VerbumTheme(context = context) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Settings (Preferences & Git Sync)")
        }
    }
}
