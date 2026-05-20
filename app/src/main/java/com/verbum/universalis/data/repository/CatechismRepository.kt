package com.verbum.universalis.data.repository

import android.content.Context
import com.verbum.universalis.data.db.CatechismRawDatabase
import com.verbum.universalis.data.db.CccBibleRefEntity
import com.verbum.universalis.data.db.CccFootnoteBibleRefEntity
import com.verbum.universalis.data.db.CccFootnoteEntity
import com.verbum.universalis.data.db.CccParagraphEntity
import com.verbum.universalis.ui.catechism.CccTocNode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatechismRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun db() = CatechismRawDatabase.getDatabase(context)

    fun getParagraph(number: Int): CccParagraphEntity? {
        return db().getParagraph(number)
    }

    fun getBibleRefsForParagraph(number: Int): List<CccBibleRefEntity> {
        return db().getBibleRefsForParagraph(number)
    }

    fun getFootnotesForParagraph(number: Int): List<CccFootnoteEntity> {
        return db().getFootnotesForParagraph(number)
    }

    fun getFootnoteBibleRefs(footnoteId: Int): List<CccFootnoteBibleRefEntity> {
        return db().getFootnoteBibleRefs(footnoteId)
    }

    fun getTocTree(): List<CccTocNode> {
        val paragraphs = db().getAllParagraphs()
        if (paragraphs.isEmpty()) return emptyList()

        val roots = mutableListOf<CccTocNode>()
        val pathMap = mutableMapOf<String, CccTocNode>()
        
        paragraphs.forEach { p ->
            val path = p.tocPath
            val parts = path.split(" > ")
            
            var parent: CccTocNode? = null
            var currentPath = ""
            
            parts.forEachIndexed { index, title ->
                currentPath += if (currentPath.isEmpty()) title else " > $title"
                
                if (!pathMap.containsKey(currentPath)) {
                    val newNode = CccTocNode(
                        id = currentPath,
                        title = title,
                        indentLevel = index,
                        paragraphNumber = p.number, // Every node gets the first paragraph that hits it
                        children = mutableListOf()
                    )
                    pathMap[currentPath] = newNode
                    
                    if (parent == null) {
                        roots.add(newNode)
                    } else {
                        (parent!!.children as MutableList<CccTocNode>).add(newNode)
                    }
                }
                parent = pathMap[currentPath]
            }
        }
        
        // Flatten the tree into a list for the UI
        val result = mutableListOf<CccTocNode>()
        fun flatten(nodes: List<CccTocNode>) {
            nodes.forEach { node ->
                result.add(node)
                flatten(node.children)
            }
        }
        flatten(roots)
        return result
    }
}
