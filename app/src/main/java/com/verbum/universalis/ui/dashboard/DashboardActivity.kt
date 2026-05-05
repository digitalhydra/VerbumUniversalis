package com.verbum.universalis.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.verbum.universalis.core.theme.VerbumTheme
import androidx.navigation.compose.rememberNavController
import com.verbum.universalis.ui.navigation.VerbumNavGraph

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
