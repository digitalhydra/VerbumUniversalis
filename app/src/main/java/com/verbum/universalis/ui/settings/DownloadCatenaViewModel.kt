package com.verbum.universalis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.CatenaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadCatenaViewModel @Inject constructor(
    private val catenaRepository: CatenaRepository
) : ViewModel() {

    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()

    private val _downloadStatus = MutableStateFlow(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    init {
        checkDownloaded()
    }

    private fun checkDownloaded() {
        viewModelScope.launch {
            _isDownloaded.value = catenaRepository.isDatabaseDownloaded()
        }
    }

    fun startDownload() {
        _downloadStatus.value = DownloadStatus.Downloading
        viewModelScope.launch {
            val success = catenaRepository.downloadDatabase()
            _downloadStatus.value = if (success) DownloadStatus.Success else DownloadStatus.Error
            if (success) {
                _isDownloaded.value = true
            }
        }
    }

    fun resetToIdle() {
        _downloadStatus.value = DownloadStatus.Idle
    }
}

enum class DownloadStatus {
    Idle,
    Downloading,
    Success,
    Error
}