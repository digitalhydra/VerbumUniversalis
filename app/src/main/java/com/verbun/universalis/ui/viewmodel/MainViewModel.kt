package com.verbun.universalis.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.verbun.universalis.data.db.AppDatabase
import com.verbun.universalis.data.repository.BibleRepository
import com.verbun.universalis.data.entities.VerseWithTexts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BibleRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BibleRepository(
            db.verseDao(),
            db.interlinearDao(),
            db.lexiconDao()
        )
    }

    // Example: Fetch Genesis 1 (Book 1, Chapter 1)
    val genesis1: Flow<List<VerseWithTexts>> = repository.getChapter(1, 1)
}
