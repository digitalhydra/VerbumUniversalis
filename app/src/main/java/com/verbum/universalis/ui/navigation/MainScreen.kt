package com.verbum.universalis.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verbum.universalis.core.theme.VerbumBlue

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    
    // Bottom bar only on Dashboard
    val showBottomBar = currentRoute == Route.Dashboard.route
    
    val items = listOf(
        NavigationItem("Bible", Route.ReadingCanvas.route, Icons.Default.MenuBook),
        NavigationItem("Readings", Route.Dashboard.route, Icons.Default.CalendarMonth),
        NavigationItem("Teaching", Route.Catechism.route, Icons.Default.HistoryEdu),
        NavigationItem("Settings", Route.Settings.route, Icons.Default.Menu)
    )

    Scaffold(
        bottomBar = {} // We'll use a floating bar instead
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            VerbumNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )

            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .fillMaxWidth(0.9f)
                        .height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        items.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route?.startsWith(item.route) == true } == true
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (selected) VerbumBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) VerbumBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
