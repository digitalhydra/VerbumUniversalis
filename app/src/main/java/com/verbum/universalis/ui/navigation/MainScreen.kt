package com.verbum.universalis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    // Phase 1: Single-pane navigation (skeleton screens)
    // Tablet two-pane: InterlinearReaderScreen uses ListDetailPaneScaffold
    
    VerbumNavGraph(navController = navController)
}
