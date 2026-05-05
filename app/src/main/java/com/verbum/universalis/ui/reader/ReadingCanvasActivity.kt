package com.verbum.universalis.ui.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.verbum.universalis.core.theme.VerbumTheme
import androidx.hilt.navigation.compose.hiltViewModel

class ReadingCanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VerbumTheme {
                ReadingScreen(viewModel = hiltViewModel())
            }
        }
    }
}
