package com.verbum.universalis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.repository.CatenaRepository
import com.verbum.universalis.data.repository.CrossRefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadCatenaViewModel @Inject constructor(
    private val catenaRepository: CatenaRepository,
    private val crossRefsRepository: CrossRefsRepository,
    private val bibleRepository: BibleRepository
) : ViewModel() {

    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()

    private val _downloadStatus = MutableStateFlow(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val _progressText = MutableStateFlow("")
    val progressText: StateFlow<String> = _progressText.asStateFlow()

    init {
        checkDownloaded()
    }

    private fun checkDownloaded() {
        viewModelScope.launch {
            val catenaOk = catenaRepository.isDatabaseDownloaded()
            val refsOk = crossRefsRepository.isDatabaseDownloaded()
            _isDownloaded.value = catenaOk && refsOk
        }
    }

    fun startDownload() {
        _downloadStatus.value = DownloadStatus.Downloading
        _progressText.value = "Downloading catena…"
        viewModelScope.launch {
            try {
                val catenaJob = async { catenaRepository.downloadDatabase() }
                val refsJob = async { crossRefsRepository.downloadDatabase() }

                val catenaOk = catenaJob.await()
                if (!catenaOk) {
                    _downloadStatus.value = DownloadStatus.Error
                    _progressText.value = "Catena download failed"
                    return@launch
                }

                _progressText.value = "Downloading cross-references…"
                val refsOk = refsJob.await()
                if (!refsOk) {
                    _downloadStatus.value = DownloadStatus.Error
                    _progressText.value = "Cross-references download failed"
                    return@launch
                }

                _progressText.value = "Downloading reference links…"
                val refsLinksOk = bibleRepository.preloadReferences()
                // refsLinksOk can be false if network issue; non-fatal

                _downloadStatus.value = DownloadStatus.Success
                _isDownloaded.value = true
            } catch (e: Exception) {
                _downloadStatus.value = DownloadStatus.Error
                _progressText.value = e.message ?: "Download failed"
            }
        }
    }

    fun resetToIdle() {
        _downloadStatus.value = DownloadStatus.Idle
        _progressText.value = ""
    }
}

enum class DownloadStatus {
    Idle,
    Downloading,
    Success,
    Error
}