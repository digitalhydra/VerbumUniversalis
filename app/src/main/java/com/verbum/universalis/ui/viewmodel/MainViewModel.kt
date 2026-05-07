package com.verbum.universalis.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.daos.VerseWithTexts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {
    // Example: Fetch Genesis 1 (Book 1, Chapter 1)
    val genesis1: Flow<List<VerseWithTexts>> = repository.getChapter(1, 1)
}
