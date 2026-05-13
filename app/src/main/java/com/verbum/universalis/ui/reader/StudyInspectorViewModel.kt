package com.verbum.universalis.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import com.verbum.universalis.data.entities.InterlinearWordEntity
import com.verbum.universalis.data.entities.LexiconEntity
import com.verbum.universalis.data.repository.BibleRepository.Reference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InspectorTab { LEXICON, CATENA, REFERENCES, MY_NOTES }

@HiltViewModel
class StudyInspectorViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    private val _selectedWord = MutableStateFlow<InterlinearWordEntity?>(null)
    val selectedWord: StateFlow<InterlinearWordEntity?> = _selectedWord.asStateFlow()

    private val _lexiconEntry = MutableStateFlow<LexiconEntity?>(null)
    val lexiconEntry: StateFlow<LexiconEntity?> = _lexiconEntry.asStateFlow()

    private val _currentVerse = MutableStateFlow<Triple<Int, Int, Int>?>(null)
    private val _catenaEntries = MutableStateFlow<List<CatenaCommentaryEntity>>(emptyList())
    val catenaEntries: StateFlow<List<CatenaCommentaryEntity>> = _catenaEntries.asStateFlow()

    private val _crossRefs = MutableStateFlow<List<Reference>>(emptyList())
    val crossRefs: StateFlow<List<Reference>> = _crossRefs.asStateFlow()

    // Lazy loading states - only load when user opens study inspector
    private val _isLoadingCatena = MutableStateFlow(false)
    val isLoadingCatena: StateFlow<Boolean> = _isLoadingCatena.asStateFlow()

    private val _isLoadingRefs = MutableStateFlow(false)
    val isLoadingRefs: StateFlow<Boolean> = _isLoadingRefs.asStateFlow()

    // Track if data has been loaded for current verse (to avoid reloading on tab switch)
    private var dataLoadedForVerse: Triple<Int, Int, Int>? = null

    private val _activeTab = MutableStateFlow(InspectorTab.LEXICON)
    val activeTab: StateFlow<InspectorTab> = _activeTab.asStateFlow()

    fun selectWord(word: InterlinearWordEntity?) {
        _selectedWord.value = word
        if (word != null) {
            viewModelScope.launch {
                val lemma = word.lemma
                _lexiconEntry.value = if (lemma != null) {
                    repository.getLexiconEntry(lemma).first()
                } else {
                    null
                }
            }
            setActiveTab(InspectorTab.LEXICON)
        }
    }

    fun setCurrentVerse(bookId: Int, chapter: Int, verseNumber: Int) {
        val verse = Triple(bookId, chapter, verseNumber)
        _currentVerse.value = verse
        
        // Don't load data here - lazy load when user opens study inspector
        // Reset data if verse changed (but keep the verse info)
        if (dataLoadedForVerse != verse) {
            dataLoadedForVerse = verse
            _catenaEntries.value = emptyList()
            _crossRefs.value = emptyList()
        }
        
        if (_selectedWord.value == null) {
            setActiveTab(InspectorTab.CATENA)
        }
    }

    /**
     * Load catena and cross-references on demand.
     * Call this when the user opens the study inspector.
     * Only loads once per verse - subsequent calls are no-ops.
     */
    fun loadCatenaAndRefs() {
        val verse = _currentVerse.value ?: return
        
        // Don't reload if already loaded for this verse
        if (dataLoadedForVerse != verse) {
            dataLoadedForVerse = verse
        }
        
        // Load catena (only if not already loaded)
        if (_catenaEntries.value.isEmpty()) {
            viewModelScope.launch {
                _isLoadingCatena.value = true
                try {
                    _catenaEntries.value = repository.getCatenaForVerse(verse.first, verse.second, verse.third)
                } finally {
                    _isLoadingCatena.value = false
                }
            }
        }
        
        // Load cross-references (only if not already loaded)
        if (_crossRefs.value.isEmpty()) {
            viewModelScope.launch {
                _isLoadingRefs.value = true
                try {
                    _crossRefs.value = repository.getReferencesForVerse(verse.first, verse.second, verse.third)
                } finally {
                    _isLoadingRefs.value = false
                }
            }
        }
    }

    fun setActiveTab(tab: InspectorTab) {
        _activeTab.value = tab
    }

    private val _isCatenaDownloaded = MutableStateFlow(false)
    val isCatenaDownloaded: StateFlow<Boolean> = _isCatenaDownloaded.asStateFlow()

    private val _isCrossRefsDownloaded = MutableStateFlow(false)
    val isCrossRefsDownloaded: StateFlow<Boolean> = _isCrossRefsDownloaded.asStateFlow()

    init {
        checkDownloads()
    }

    private fun checkDownloads() {
        viewModelScope.launch {
            _isCatenaDownloaded.value = repository.isCatenaDownloaded()
            _isCrossRefsDownloaded.value = repository.isCrossRefsDownloaded()
        }
    }

    suspend fun downloadCatena(): Boolean {
        val success = repository.downloadCatena()
        if (success) _isCatenaDownloaded.value = true
        return success
    }

    suspend fun downloadCrossRefs(): Boolean {
        val success = repository.downloadCrossRefs()
        if (success) _isCrossRefsDownloaded.value = true
        return success
    }

    fun parseReference(ref: String): Triple<Int, Int, Int>? = repository.parseReference(ref)
}