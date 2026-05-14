package com.verbum.universalis.ui.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.verbum.universalis.core.theme.ThemeManager
import com.verbum.universalis.core.theme.VerbumTheme
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReadingCanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.initialize(this)
        setContent {
            VerbumTheme {
                ReadingScreen(viewModel = hiltViewModel())
            }
        }
    }
}
