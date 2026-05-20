package com.verbum.universalis.ui.catechism

sealed class CccElement {
    data class Text(val text: String, val b: Boolean = false, val i: Boolean = false) : CccElement()
    data class BibleRef(
        val refText: String,
        val bookId: Int,
        val chapter: Int,
        val verseStart: Int,
        val verseEnd: Int? = null
    ) : CccElement()
    data class CccRef(val refNumber: Int) : CccElement()
}

data class Footnote(
    val id: Int,
    val text: String,
    val bookId: Int? = null,
    val chapter: Int? = null,
    val verse: Int? = null
)

data class CccTocNode(
    val id: String,
    val title: String,
    val indentLevel: Int,
    val paragraphNumber: Int? = null,
    val children: List<CccTocNode> = emptyList()
)

data class CccParagraphUiState(
    val number: Int,
    val title: String,
    val tocPath: String,
    val elements: List<CccElement>,
    val footnotes: List<Footnote>,
    val isRead: Boolean = false
)
