package com.verbun.universalis.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbun.universalis.data.json.FileManager
import com.verbun.universalis.data.json.Note
import com.verbun.universalis.data.repository.BibleRepository
import com.verbun.universalis.data.entities.InterlinearWordEntity
import com.verbun.universalis.data.entities.LexiconEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class InspectorTab { LEXICON, CATENA, REFERENCES, MY_NOTES }

@HiltViewModel
class InterlinearViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val fileManager: FileManager
) : ViewModel() {

    private val _selectedVerseId = MutableStateFlow<Int?>(null)
    val selectedVerseId: StateFlow<Int?> = _selectedVerseId.asStateFlow()

    // Notes for selected verse
    val notes: Flow<List<Note>> = _selectedVerseId.map { verseId ->
        if (verseId == null) emptyList()
        else fileManager.loadNotes().filter { it.verseId == verseId }
    }

    // Catena entries for selected verse
    private val _catenaEntries = MutableStateFlow<List<BibleRepository.CatenaEntry>>(emptyList())
    val catenaEntries: StateFlow<List<BibleRepository.CatenaEntry>> = _catenaEntries.asStateFlow()

    // References for selected verse
    private val _references = MutableStateFlow<List<BibleRepository.Reference>>(emptyList())
    val references: StateFlow<List<BibleRepository.Reference>> = _references.asStateFlow()

    // Check if a word is highlighted (simplified)
    fun isWordHighlighted(word: InterlinearWordEntity): Boolean {
        val verseId = _selectedVerseId.value ?: return false
        val highlights = fileManager.loadHighlights().filter { it.verseId == verseId }
        return highlights.isNotEmpty()
    }

    val words: Flow<List<InterlinearWordEntity>> = _selectedVerseId.map { verseId ->
        if (verseId == null) emptyList()
        else repository.getInterlinearWordsForVerse(verseId)
    }

    private val _selectedWord = MutableStateFlow<InterlinearWordEntity?>(null)
    val selectedWord: StateFlow<InterlinearWordEntity?> = _selectedWord.asStateFlow()

    val lexiconEntry: Flow<LexiconEntity?> = _selectedWord.map { word ->
        if (word == null) null
        else repository.getLexiconEntry(word.lemma)
    }

    private val _showMorphology = MutableStateFlow(true)
    val showMorphology: StateFlow<Boolean> = _showMorphology.asStateFlow()

    fun toggleMorphology() {
        _showMorphology.value = !_showMorphology.value
    }

    private val _lastVerseIdForInspector = MutableStateFlow<Int?>(null)
    private val _activeTab = MutableStateFlow(InspectorTab.LEXICON)
    val activeTab: StateFlow<InspectorTab> = _activeTab.asStateFlow()

    fun setVerse(verseId: Int?) {
        if (verseId == _selectedVerseId.value) {
            return
        }
        _selectedVerseId.value = verseId
        _selectedWord.value = null
        _activeTab.value = InspectorTab.LEXICON
        _lastVerseIdForInspector.value = verseId

        // Load catena entries and references for this verse
        verseId?.let {
            viewModelScope.launch {
                _catenaEntries.value = repository.getCatenaForVerse(it)
                _references.value = repository.getReferencesForVerse(it)
            }
        } ?: run {
            _catenaEntries.value = emptyList()
            _references.value = emptyList()
        }
    }

    fun selectWord(word: InterlinearWordEntity) {
        _selectedWord.value = word
    }

    fun setActiveTab(tab: InspectorTab) {
        _activeTab.value = tab
    }
}
