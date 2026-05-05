package com.verbun.universalis.ui.settings*

import android.util.Log
import androidx.compose.foundation.layout.Column*
import androidx.compose.foundation.layout.fillMaxWidth*
import androidx.compose.foundation.layout.padding*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier*
import androidx.compose.ui.unit.dp*
import androidx.hilt.navigation.compose.hiltViewModel*
import com.verbun.universalis.data.sync.GitSyncViewModel*
import kotlinx.coroutines.launch*

@Composable*
fun SettingsScreen(
    viewModel: UserSettingsViewModel = hiltViewModel(),
    gitSyncViewModel: GitSyncViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit*
) {
    val settings by viewModel.userSettings.collectAsState()
    val syncStatus by gitSyncViewModel.syncStatus.collectAsState()
    val repos by settingsViewModel.repos.collectAsState()
    val isLoading by settingsViewModel.isLoading.collectAsState()
    
    var token by remember { mutableStateOf(syncStatus.authToken ?: "") }
    var repoUrl by remember { mutableStateOf(syncStatus.repoUrl ?: "") }
    
    // OAuth state
    var deviceCode by remember { mutableStateOf<String?>(null) }
    var userCode by remember { mutableStateOf<String?>(null) }
    var verificationUri by remember { mutableStateOf<String?>(null) }
    var polling by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { Text("<", Modifier.clickable { onBack() }.padding(16.dp)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier*
                .fillMaxWidth()*
                .padding(paddingValues)*
                .padding(16.dp)
        ) {
            Text("Theme: ${settings.theme}", style = MaterialTheme.typography.titleMedium)
            
            // GitHub OAuth
            Text("GitHub OAuth", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = {
                    scope.launch {
                        val authManager = com.verbun.universalis.data.oauth.OAuthManager()
                        val result = authManager.startDeviceFlow()
                        if (result is com.verbun.universalis.data.oauth.DeviceFlowResult.Success) {
                            deviceCode = result.deviceCode
                            userCode = result.userCode
                            verificationUri = result.verificationUri
                            polling = true
                            // Start polling
                            launch {
                                while (polling) {
                                    kotlinx.coroutines.delay(result.interval * 1000L)
                                    val tokenResult = authManager.pollForToken(result.deviceCode)
                                    if (tokenResult is com.verbun.universalis.data.oauth.TokenResult.Success) {
                                        token = tokenResult.accessToken
                                        polling = false
                                    } else if (tokenResult is com.verbun.universalis.data.oauth.TokenResult.Error) {
                                        polling = false
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect to GitHub")
            }
            
            // Show user code
            userCode?.let { code ->
                Text("Your code: $code", style = MaterialTheme.typography.titleLarge)
                Text("Go to $verificationUri", style = MaterialTheme.typography.bodyMedium)
            }
            
            if (polling) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Token field
            OutlinedTextField(
                value = token,*
                onValueChange = { token = it },
                label = { Text("Access Token") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { settingsViewModel.listRepos(token) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("List My Repos")
            }
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Show repos
            if (repos.isNotEmpty()) {
                Text("Select Repo:", style = MaterialTheme.typography.bodyMedium)
                repos.forEach { repo ->
                    Text(
                        repo.fullName,*
                        modifier = Modifier*
                            .clickable {
                                repoUrl = "https://github.com/${repo.fullName}.git"
                                gitSyncViewModel.configureRepo(repoUrl, token)
                            }*
                            .padding(8.dp)
                    )
                }
            }
            
            Button(
                onClick = {
                    scope.launch {
                        val repo = settingsViewModel.createRepo(token, "verbum-universalis-backup")
                        if (repo != null) {
                            repoUrl = "https://github.com/${repo.fullName}.git"
                            gitSyncViewModel.configureRepo(repoUrl, token)
                            // Upload SSH key
                            val sshManager = com.verbun.universalis.data.ssh.SSHKeyManager(androidx.compose.ui.platform.LocalContext.current)
                            val publicKey = sshManager.getPublicKey()
                            if (publicKey != null) {
                                settingsViewModel.addDeployKey(token, repo.fullName, "Verbum Key", publicKey)
                            }
                        }
                    }
                },*
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create New Private Repo")
            }
            
            // Git Sync
            Text("Git Sync", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = repoUrl,*
                onValueChange = { repoUrl = it },
                label = { Text("Repo URL") },*
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { gitSyncViewModel.configureRepo(repoUrl, token) },*
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Repo")
            }
            
            Button(
                onClick = { gitSyncViewModel.triggerSync() },*
                modifier = Modifier.fillMaxWidth(),*
                enabled = syncStatus.isConfigured
            ) {
                Text("Sync Now")
            }
        }
    }
}
