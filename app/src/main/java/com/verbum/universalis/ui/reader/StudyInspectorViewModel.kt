package com.verbum.universalis.ui.reader

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.repository.BibleRepository
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import com.verbum.universalis.data.entities.InterlinearWordEntity
import com.verbum.universalis.data.entities.LexiconEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch

val Application.studyInspectorDataStore by preferencesDataStore(name = "study_inspector_settings")

enum class InspectorTab { LEXICON, CATENA, REFERENCES, MY_NOTES }

enum class ChurchTradition(val displayName: String) {
    ALL("All"), CATHOLIC("Catholic"), ORTHODOX("Orthodox"), PROTESTANT("Protestant")
}

@HiltViewModel
class StudyInspectorViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val app: Application
) : ViewModel() {

    private val SELECTED_WORD_ID_KEY = stringPreferencesKey("selected_word_id")
    private val ACTIVE_TAB_KEY = stringPreferencesKey("active_tab")

    private val _selectedWord = MutableStateFlow<InterlinearWordEntity?>(null)
    val selectedWord: StateFlow<InterlinearWordEntity?> = _selectedWord.asStateFlow()

    val lexiconEntry: Flow<LexiconEntity?> = _selectedWord.map { word ->
        if (word == null) null
        else repository.getLexiconEntry(word.lemma)
    }

    // Current verse for Catena + Cross-Refs lookup
    private val _currentVerse = MutableStateFlow<Triple<Int, Int, Int>?>(null)

    val catenaEntries: Flow<List<CatenaCommentaryEntity>> = _currentVerse.map { verse ->
        if (verse == null) emptyList()
        else repository.getCatenaForVerse(verse.first, verse.second, verse.third)
    }

    val crossRefs: Flow<List<BibleRepository.Reference>> = _currentVerse.map { verse ->
        if (verse == null) emptyList()
        else repository.getReferencesForVerse(verse.first, verse.second, verse.third)
    }

    private val _activeTab = MutableStateFlow(InspectorTab.LEXICON)
    val activeTab: StateFlow<InspectorTab> = _activeTab.asStateFlow()

    private val _activeTradition = MutableStateFlow(ChurchTradition.ALL)
    val activeTradition: StateFlow<ChurchTradition> = _activeTradition.asStateFlow()

    init {
        viewModelScope.launch {
            app.studyInspectorDataStore.data.collect { preferences ->
                val tabName = preferences[ACTIVE_TAB_KEY]
                if (tabName != null) {
                    _activeTab.value = InspectorTab.valueOf(tabName)
                }
            }
        }
    }

    fun selectWord(word: InterlinearWordEntity?) {
        _selectedWord.value = word
        if (word != null) {
            setActiveTab(InspectorTab.LEXICON)
        }
    }

    fun setCurrentVerse(bookId: Int, chapter: Int, verseNumber: Int) {
        _currentVerse.value = Triple(bookId, chapter, verseNumber)
        if (_selectedWord.value == null) {
            setActiveTab(InspectorTab.CATENA)
        }
    }

    fun setActiveTab(tab: InspectorTab) {
        _activeTab.value = tab
        viewModelScope.launch {
            app.studyInspectorDataStore.edit { preferences ->
                preferences[ACTIVE_TAB_KEY] = tab.name
            }
        }
    }

    fun setActiveTradition(t: ChurchTradition) {
        _activeTradition.value = t
    }

    // Filter catena entries by tradition
    fun getFilteredCatena(entries: List<CatenaCommentaryEntity>): List<CatenaCommentaryEntity> {
        if (_activeTradition.value == ChurchTradition.ALL) return entries
        return entries.filter { it.period?.contains(_activeTradition.value.displayName, ignoreCase = true) == true }
    }

    fun isCatenaDownloaded(): Boolean = repository.isCatenaDownloaded()
    suspend fun downloadCatena(): Boolean = repository.downloadCatena()
    fun isCrossRefsDownloaded(): Boolean = repository.isCrossRefsDownloaded()
    suspend fun downloadCrossRefs(): Boolean = repository.downloadCrossRefs()

    fun parseReference(ref: String): Triple<Int, Int, Int>? = repository.parseReference(ref)
}