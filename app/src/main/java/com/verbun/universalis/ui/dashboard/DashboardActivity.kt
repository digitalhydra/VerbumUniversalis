package com.verbun.universalis.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.verbun.universalis.core.theme.VerbumTheme
import androidx.navigation.compose.rememberNavController
import com.verbun.universalis.ui.navigation.VerbumNavGraph

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VerbumTheme {
                com.verbun.universalis.ui.navigation.MainScreen()
            }
        }
    }
}
