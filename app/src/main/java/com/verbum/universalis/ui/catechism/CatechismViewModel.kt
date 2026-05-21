package com.verbum.universalis.ui.catechism

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.universalis.data.db.CccSearchResultEntity
import com.verbum.universalis.data.repository.CatechismRepository
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
    private val repository: CatechismRepository
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

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _tocItems.value = repository.getTocTree()
        }
    }

    fun selectParagraph(number: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = repository.getParagraph(number)
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
                    listOf(CccElement.Text(entity.plainText))
                }

                _uiState.value = CccParagraphUiState(
                    number = entity.number,
                    title = entity.tocPath.split(" > ").last(),
                    tocPath = entity.tocPath,
                    elements = elements,
                    footnotes = footnotes,
                    isRead = false // TODO: Persist read state
                )
            }
        }
    }

    fun toggleRead() {
        _uiState.value = _uiState.value?.let { it.copy(isRead = !it.isRead) }
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
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = repository.search(query)
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
