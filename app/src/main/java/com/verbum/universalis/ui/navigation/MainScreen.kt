package com.verbum.universalis.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowsizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowsizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen() {
    val windowsSizeClass = calculateWindowsizeClass(LocalContext.current)
    val navController = rememberNavController()

    // Check if we are on a tablet (Expanded width)
    val useTwoPane = windowsSizeClass.widthSizeClass == WindowsizeClass.WidthSizeClass.Expanded

    if (useTwoPane) {
        // Two-Pane Layout: Left (NavHost) + Right (Study Inspector)
        // This is a simplified version - full implementation requires 
        // a custom TwoPaneScaffold or modifying NavHost.
        // For now, we just show the default NavHost.
        // TODO: Implement TwoPaneScaffold for Tablet
        VerbumNavGraph(navController = navController)
    } else {
        // Single Pane (Phone)
        VerbumNavGraph(navController = navController)
    }
}
