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

    private val _activeTab = MutableStateFlow(InspectorTab.LEXICON)
    val activeTab: StateFlow<InspectorTab> = _activeTab.asStateFlow()

    fun selectWord(word: InterlinearWordEntity?) {
        _selectedWord.value = word
        if (word != null) {
            viewModelScope.launch {
                _lexiconEntry.value = repository.getLexiconEntry(word.lemma)
            }
            setActiveTab(InspectorTab.LEXICON)
        }
    }

    fun setCurrentVerse(bookId: Int, chapter: Int, verseNumber: Int) {
        _currentVerse.value = Triple(bookId, chapter, verseNumber)
        viewModelScope.launch {
            _catenaEntries.value = repository.getCatenaForVerse(bookId, chapter, verseNumber)
            _crossRefs.value = repository.getReferencesForVerse(bookId, chapter, verseNumber)
        }
        if (_selectedWord.value == null) {
            setActiveTab(InspectorTab.CATENA)
        }
    }

    fun setActiveTab(tab: InspectorTab) {
        _activeTab.value = tab
    }

    fun isCatenaDownloaded(): Boolean = repository.isCatenaDownloaded()
    suspend fun downloadCatena(): Boolean = repository.downloadCatena()
    fun isCrossRefsDownloaded(): Boolean = repository.isCrossRefsDownloaded()
    suspend fun downloadCrossRefs(): Boolean = repository.downloadCrossRefs()

    fun parseReference(ref: String): Triple<Int, Int, Int>? = repository.parseReference(ref)
}