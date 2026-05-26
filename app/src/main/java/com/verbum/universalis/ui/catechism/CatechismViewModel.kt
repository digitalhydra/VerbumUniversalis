package com.verbum.universalis.ui.catechism

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.core.LanguageManager
import com.verbum.universalis.data.db.CccSearchResultEntity
import com.verbum.universalis.data.repository.CatechismRepository
import com.verbum.universalis.data.json.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class CatechismViewModel @Inject constructor(
    private val repository: CatechismRepository,
    private val fileManager: FileManager,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CccParagraphUiState?>(null)
    val uiState: StateFlow<CccParagraphUiState?> = _uiState.asStateFlow()

    private val _tocItems = MutableStateFlow<List<CccTocNode>>(emptyList())
    val tocItems: StateFlow<List<CccTocNode>> = _tocItems.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CccSearchResultEntity>>(emptyList())
    val searchResults: StateFlow<List<CccSearchResultEntity>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Int>>(emptyList())
    val bookmarks: StateFlow<List<Int>> = _bookmarks.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _bookmarks.value = fileManager.loadCccBookmarks()
            
            // Observe language changes
            languageManager.appLanguage.collect { lang ->
                _tocItems.value = repository.getTocTree(lang)
                // Re-fetch current paragraph if active
                _uiState.value?.number?.let { num ->
                    selectParagraph(num)
                }
            }
        }
    }

    fun selectParagraph(number: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val lang = languageManager.appLanguage.value
            val entity = repository.getParagraph(number, lang)
            if (entity != null) {
                // Fetch text-based footnotes from ccc_footnotes
                val footnoteEntities = repository.getFootnotesForParagraph(number)
                val footnotes = footnoteEntities.map { fe ->
                    val bibleRefs = repository.getFootnoteBibleRefs(fe.id).map { br ->
                        FootnoteBibleRef(
                            bookId = br.bookId,
                            chapter = br.chapter,
                            verse = br.verseStart,
                            refText = br.refText,
                            position = br.refPosition,
                            length = br.refLength
                        )
                    }
                    Footnote(
                        id = fe.footnoteNumber,
                        text = fe.footnoteText,
                        bibleRefs = bibleRefs
                    )
                }
                
                // Parse formatted JSON
                val elements = try {
                    json.decodeFromString<List<CccElementJson>>(entity.formattedJson).map { jsonElement ->
                        when (jsonElement.type) {
                            "text" -> CccElement.Text(
                                text = jsonElement.text ?: "",
                                b = jsonElement.attrs?.b ?: false,
                                i = jsonElement.attrs?.i ?: false
                            )
                            "bible-ref" -> CccElement.BibleRef(
                                refText = jsonElement.ref_text ?: "",
                                bookId = jsonElement.book_id ?: 0,
                                chapter = jsonElement.chapter ?: 0,
                                verseStart = jsonElement.verse_start ?: 0
                            )
                            "ref-ccc" -> CccElement.CccRef(
                                refNumber = jsonElement.ref_number ?: 0
                            )
                            else -> CccElement.Text(jsonElement.text ?: "")
                        }
                    }
                } catch (e: Exception) {
                    listOf(CccElement.Text(entity.plain_text))
                }

                _uiState.value = CccParagraphUiState(
                    number = entity.number,
                    title = entity.tocPath.split(" > ").last(),
                    tocPath = entity.tocPath,
                    elements = elements,
                    footnotes = footnotes,
                    isRead = false, // TODO: Persist read state
                    isBookmarked = _bookmarks.value.contains(entity.number)
                )
            }
        }
    }

    fun toggleRead() {
        _uiState.value = _uiState.value?.let { it.copy(isRead = !it.isRead) }
    }

    fun toggleBookmark() {
        val currentNum = _uiState.value?.number ?: return
        val currentBookmarks = _bookmarks.value.toMutableList()
        
        if (currentBookmarks.contains(currentNum)) {
            currentBookmarks.remove(currentNum)
        } else {
            currentBookmarks.add(currentNum)
        }
        
        _bookmarks.value = currentBookmarks
        _uiState.value = _uiState.value?.copy(isBookmarked = currentBookmarks.contains(currentNum))
        
        viewModelScope.launch(Dispatchers.IO) {
            fileManager.saveCccBookmarks(currentBookmarks)
        }
    }

    fun setSearchVisible(visible: Boolean) {
        _isSearching.value = visible
        if (!visible) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun performSearch(query: String) {
        _searchQuery.value = query
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        
        // Debug: Hardcode a result to test UI binding if DB fails
        if (query.lowercase() == "debug") {
            _searchResults.value = listOf(
                CccSearchResultEntity(26, "DEBUG MODE", "This is a <b>fake</b> result to test if UI works.")
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lang = languageManager.appLanguage.value
            val dbResults = repository.search(query, lang)
            _searchResults.value = dbResults
        }
    }
}

@Serializable
data class CccElementJson(
    val type: String,
    val text: String? = null,
    val attrs: CccAttributes? = null,
    val number: Int? = null,
    val ref_number: Int? = null,
    val book_id: Int? = null,
    val chapter: Int? = null,
    val verse_start: Int? = null,
    val ref_text: String? = null
)

@Serializable
data class CccAttributes(
    val b: Boolean? = null,
    val i: Boolean? = null,
    val href: String? = null
)
