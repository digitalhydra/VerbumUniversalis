package com.verbum.universalis.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.ui.navigation.VerbumNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VerbumTheme {
                com.verbum.universalis.ui.navigation.MainScreen()
            }
        }
    }
}
