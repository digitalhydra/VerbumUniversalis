package com.verbum.universalis.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadCatenaScreen(
    onBack: () -> Unit = {},
    viewModel: DownloadCatenaViewModel = hiltViewModel()
) {
    val isDownloaded by viewModel.isDownloaded.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Download Catena") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Download Catena Commentary Database",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isDownloaded) {
                Text(
                    text = "Catena database is already downloaded and ready to use.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "This will download the verbum_catena.db file from GitHub releases (~200 MB).",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.Idle -> {
                        Button(
                            onClick = { viewModel.startDownload() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Download Now")
                        }
                    }
                    DownloadStatus.Downloading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 4.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Downloading...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    DownloadStatus.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Download completed successfully!",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You can now use the Catena commentary in the app.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onBack() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Done")
                            }
                        }
                    }
                    DownloadStatus.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Download failed. Please check your internet connection and try again.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.resetToIdle() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}