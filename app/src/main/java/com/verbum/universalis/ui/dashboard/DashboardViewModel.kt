package com.verbum.universalis.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.LiturgicalRepository
import com.verbum.universalis.data.entities.DailyMassReadingEntry
import com.verbum.universalis.data.entities.Celebration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val liturgicalRepository: LiturgicalRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _massReadings = MutableStateFlow<DailyMassReadingEntry?>(null)
    val massReadings: StateFlow<DailyMassReadingEntry?> = _massReadings.asStateFlow()

    private val _celebration = MutableStateFlow<Celebration?>(null)
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()

    init {
        updateDate(java.time.LocalDate.now().toString())
    }

    fun updateDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            val readings = liturgicalRepository.getMassReadingsForDate(date)
            _massReadings.value = readings
            
            val celebration = liturgicalRepository.getCelebrationForDate(date)
            _celebration.value = celebration
            
            if (readings == null) {
                android.util.Log.w("DashboardViewModel", "No mass readings found for date: $date")
            }
        }
    }

    fun getMassReadingsForDate(date: String): DailyMassReadingEntry? {
        return liturgicalRepository.getMassReadingsForDate(date)
    }

    // Get all dates for picker
    fun getAllDates(): List<String> {
        return liturgicalRepository.getAllDates()
    }
}
