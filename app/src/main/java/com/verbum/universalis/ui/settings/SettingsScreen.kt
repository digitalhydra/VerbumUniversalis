package com.verbum.universalis.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.verbum.universalis.data.ssh.SSHKeyManager
import com.verbum.universalis.data.sync.GitSyncViewModel
import com.verbum.universalis.data.sync.SyncStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gitSyncViewModel: GitSyncViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    val syncStatus by gitSyncViewModel.syncStatus.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { 
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            items(listOf(
                SettingsMenuItem(
                    title = "Download Catena",
                    description = "Download the Catena commentary database from GitHub releases",
                    icon = androidx.compose.material3.Icons.Default.Download,
                    route = Route.DownloadCatena.route
                ),
                SettingsMenuItem(
                    title = "Sync",
                    description = "Sync your notes and highlights with GitHub",
                    icon = androidx.compose.material3.Icons.Default.Sync,
                    route = Route.Sync.route
                ),
                SettingsMenuItem(
                    title = "Notes",
                    description = "Manage your notes and highlights",
                    icon = androidx.compose.material3.Icons.Default.Note,
                    route = Route.Notes.route
                ),
                SettingsMenuItem(
                    title = "Theme",
                    description = "Change the app theme (light/dark)",
                    icon = androidx.compose.material3.Icons.Default.Brightness_6,
                    route = Route.Theme.route
                )
            )) { item ->
                SettingsMenuCard(
                    item = item,
                    onClick = { navController.navigate(item.route) }
                )
            }
        }
    }
}

// We'll create a simple data class for the menu item
data class SettingsMenuItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.material3.Icons,
    val route: String
)

// And a simple card for the menu item
@Composable
fun SettingsMenuCard(
    item: SettingsMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}