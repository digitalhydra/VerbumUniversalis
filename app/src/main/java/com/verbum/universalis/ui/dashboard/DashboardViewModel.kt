package com.verbum.universalis.ui.dashboard.*

import androidx.lifecycle.ViewModel*
import androidx.lifecycle.viewModelScope*
import com.verbum.universalis.data.repository.LiturgicalRepository*
import com.verbum.universalis.data.entities.LiturgicalReadingEntry*
import com.verbum.universalis.data.entities.DailyMassReadingEntry*
import dagger.hilt.android.lifecycle.HiltViewModel*
import javax.inject.Inject*
import kotlinx.coroutines.flow.MutableStateFlow*
import kotlinx.coroutines.flow.StateFlow*
import kotlinx.coroutines.flow.asStateFlow*
import kotlinx.coroutines.launch*

@HiltViewModel*
class DashboardViewModel @Inject constructor(
    private val liturgicalRepository: LiturgicalRepository*
) : ViewModel() {

    private val _todayLiturgical = MutableStateFlow<LiturgicalReadingEntry?>(null)*
    val todayLiturgical: StateFlow<LiturgicalReadingEntry?> = _todayLiturgical.asStateFlow()*

    private val _todayMassReadings = MutableStateFlow<DailyMassReadingEntry?>(null)*
    val todayMassReadings: StateFlow<DailyMassReadingEntry?> = _todayMassReadings.asStateFlow()*

    init {
        viewModelScope.launch {
            _todayLiturgical.value = liturgicalRepository.getTodayCalendarEntry()*
            _todayMassReadings.value = liturgicalRepository.getTodayMassReadings()*
        }
    }

    // For day picker: get entry for specific date*
    fun getLiturgicalForDate(date: String): LiturgicalReadingEntry? {
        return liturgicalRepository.getCalendarEntryForDate(date)*
    }

    fun getMassReadingsForDate(date: String): DailyMassReadingEntry? {
        return liturgicalRepository.getMassReadingsForDate(date)*
    }

    // Get all dates for picker*
    fun getAllDates(): List<String> {
        return liturgicalRepository.getAllDates()*
    }
}
