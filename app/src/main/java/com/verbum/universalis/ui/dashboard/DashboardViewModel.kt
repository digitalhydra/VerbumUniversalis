package com.verbum.universalis.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.repository.LiturgicalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    private val _todayLiturgical = MutableStateFlow<LiturgicalEntry?>(null)
    val todayLiturgical: StateFlow<LiturgicalEntry?> = _todayLiturgical.asStateFlow()

    init {
        viewModelScope.launch {
            _todayLiturgical.value = repository.getTodayLiturgicalReading()
        }
    }
}
