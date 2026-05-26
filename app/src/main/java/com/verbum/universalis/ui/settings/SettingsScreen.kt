package com.verbum.universalis.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.verbum.universalis.R
import com.verbum.universalis.data.ssh.SSHKeyManager
import com.verbum.universalis.data.sync.GitSyncViewModel
import com.verbum.universalis.data.sync.SyncStatus
import kotlinx.coroutines.launch

// Route constants
object Route {
    const val DownloadCatena = "download_catena"
    const val Sync = "sync"
    const val Notes = "notes"
    const val Theme = "theme"
    const val Language = "language"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gitSyncViewModel: GitSyncViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val syncStatus by gitSyncViewModel.syncStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val menuItems = listOf(
                SettingsMenuItem(
                    title = stringResource(R.string.download_catena),
                    description = stringResource(R.string.download_catena_desc),
                    icon = Icons.Default.Download,
                    route = Route.DownloadCatena
                ),
                SettingsMenuItem(
                    title = stringResource(R.string.sync),
                    description = stringResource(R.string.sync_desc),
                    icon = Icons.Default.Sync,
                    route = Route.Sync
                ),
                SettingsMenuItem(
                    title = stringResource(R.string.notes),
                    description = stringResource(R.string.notes_desc),
                    icon = Icons.Default.Note,
                    route = Route.Notes
                ),
                SettingsMenuItem(
                    title = stringResource(R.string.theme),
                    description = stringResource(R.string.theme_desc),
                    icon = Icons.Default.Brightness6,
                    route = Route.Theme
                ),
                SettingsMenuItem(
                    title = stringResource(R.string.language),
                    description = stringResource(R.string.language_desc),
                    icon = Icons.Default.Language,
                    route = Route.Language
                )
            )

            menuItems.forEach { item ->
                SettingsMenuCard(
                    item = item,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

data class SettingsMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun SettingsMenuCard(
    item: SettingsMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
