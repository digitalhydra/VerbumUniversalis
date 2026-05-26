package com.verbum.universalis.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.verbum.universalis.core.LanguageManager
import com.verbum.universalis.core.theme.ThemeManager
import com.verbum.universalis.core.theme.VerbumTheme
import com.verbum.universalis.ui.navigation.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    
    @javax.inject.Inject
    lateinit var languageManager: LanguageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        languageManager.initialize(this)
        ThemeManager.initialize(this)
        setContent {
            VerbumTheme {
                MainScreen()
            }
        }
    }
}
