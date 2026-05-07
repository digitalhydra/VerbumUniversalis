package com.verbum.universalis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.github.GitHubApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val gitHubApi: GitHubApiService
) : ViewModel() {

    private val _repos = MutableStateFlow<List<GitHubApiService.Repo>>(emptyList())
    val repos: StateFlow<List<GitHubApiService.Repo>> = _repos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _createdRepo = MutableStateFlow<GitHubApiService.Repo?>(null)
    val createdRepo: StateFlow<GitHubApiService.Repo?> = _createdRepo.asStateFlow()

    fun listRepos(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _repos.value = gitHubApi.listRepos(token)
            _isLoading.value = false
        }
    }

    fun createRepo(token: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _createdRepo.value = gitHubApi.createRepo(token, name)
            _isLoading.value = false
        }
    }

    fun addDeployKey(token: String, repoFullName: String, title: String, key: String) {
        viewModelScope.launch {
            gitHubApi.addDeployKey(token, repoFullName, title, key)
        }
    }
}
