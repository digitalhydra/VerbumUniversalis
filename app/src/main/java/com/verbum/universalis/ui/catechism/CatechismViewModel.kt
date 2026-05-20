package com.verbum.universalis.ui.catechism

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CatechismViewModel @Inject constructor() : ViewModel() {

    private val dummyParagraph27 = CccParagraphUiState(
        number = 27,
        title = "THE DESIRE FOR GOD",
        tocPath = "PART ONE > SECTION ONE > CHAPTER ONE",
        elements = listOf(
            CccElement.Text("The desire for God is written in the human heart, because man is created by God and for God; and God never ceases to attract man to himself. (GS 19 § 1). Only in God will he find the truth and happiness he never stops searching for ("),
            CccElement.BibleRef("Acts 17:28", bookId = 51, chapter = 17, verseStart = 28),
            CccElement.Text("). Even when this desire is obscured, man's very nature is to seek his source, a longing that manifests as a fundamental 'tons antolest' that can only be fulfilled in communion with God. This desire is itself a gift, a call to return.")
        ),
        footnotes = listOf(
            Footnote(1, "Romans 1:20", bookId = 52, chapter = 1, verse = 20),
            Footnote(2, "Acts 17:28", bookId = 51, chapter = 17, verse = 28),
            Footnote(3, "Vatican II, Lumen Gentium 16.")
        ),
        isRead = false
    )

    private val dummyParagraph28 = CccParagraphUiState(
        number = 28,
        title = "WAYS OF KNOWING GOD",
        tocPath = "PART ONE > SECTION ONE > CHAPTER ONE",
        elements = listOf(
            CccElement.Text("In many ways, throughout history down to the present day, men have given expression to their quest for God in their religious beliefs and behavior: in their prayers, sacrifices, rituals, meditations, and so forth. These forms of religious expression, despite the ambiguities they often bring with them, are so universal that one may well call man a "),
            CccElement.Text("religious being", i = true),
            CccElement.Text(".")
        ),
        footnotes = listOf(
            Footnote(1, "Acts 17:26-28")
        ),
        isRead = true
    )

    private val _uiState = MutableStateFlow<CccParagraphUiState?>(null)
    val uiState: StateFlow<CccParagraphUiState?> = _uiState.asStateFlow()

    private val _tocItems = MutableStateFlow<List<CccTocNode>>(
        listOf(
            CccTocNode("1", "Prologue", 0),
            CccTocNode("2", "PART ONE: THE PROFESSION OF FAITH", 0),
            CccTocNode("3", "SECTION ONE: 'I BELIEVE' - 'WE BELIEVE'", 1),
            CccTocNode("4", "CHAPTER ONE: MAN'S CAPACITY FOR GOD", 2),
            CccTocNode("5", "I. The Desire for God", 3, paragraphNumber = 27),
            CccTocNode("6", "II. Ways of Knowing God", 3, paragraphNumber = 28)
        )
    )
    val tocItems: StateFlow<List<CccTocNode>> = _tocItems.asStateFlow()

    fun selectParagraph(number: Int) {
        _uiState.value = when (number) {
            27 -> dummyParagraph27
            28 -> dummyParagraph28
            else -> dummyParagraph27 // Fallback for dummy
        }
    }

    fun toggleRead() {
        _uiState.value = _uiState.value?.let { it.copy(isRead = !it.isRead) }
    }

    fun navigateNext() {
        val current = _uiState.value?.number ?: return
        selectParagraph(current + 1)
    }

    fun navigatePrev() {
        val current = _uiState.value?.number ?: return
        if (current > 1) selectParagraph(current - 1)
    }
}
