package com.verbum.universalis.ui.reader

import org.junit.Assert.*
import org.junit.Test

class PassageTest {

    @Test
    fun testPassageIsVerseVisibleWithRange() {
        val passage = Passage(56, 1, "17-23")
        
        // Verses within range
        assertTrue(passage.isVerseVisible(17))
        assertTrue(passage.isVerseVisible(20))
        assertTrue(passage.isVerseVisible(23))
        
        // Verses outside range
        assertFalse(passage.isVerseVisible(1))
        assertFalse(passage.isVerseVisible(16))
        assertFalse(passage.isVerseVisible(24))
    }

    @Test
    fun testPassageIsVerseVisibleWithMultipleRanges() {
        val passage = Passage(56, 1, "1-3, 5, 7-9")
        
        assertTrue(passage.isVerseVisible(1))
        assertTrue(passage.isVerseVisible(2))
        assertTrue(passage.isVerseVisible(3))
        assertFalse(passage.isVerseVisible(4))
        assertTrue(passage.isVerseVisible(5))
        assertFalse(passage.isVerseVisible(6))
        assertTrue(passage.isVerseVisible(7))
        assertTrue(passage.isVerseVisible(8))
        assertTrue(passage.isVerseVisible(9))
        assertFalse(passage.isVerseVisible(10))
    }

    @Test
    fun testPassageIsVerseVisibleNoFilter() {
        val passage = Passage(56, 1, null)
        assertTrue(passage.isVerseVisible(1))
        assertTrue(passage.isVerseVisible(100))
    }

    @Test
    fun testPassageFirstVerse() {
        assertEquals(17, Passage(56, 1, "17-23").firstVerse)
        assertEquals(1, Passage(56, 1, "1-3, 5").firstVerse)
        assertEquals(5, Passage(56, 1, "5").firstVerse)
        assertNull(Passage(56, 1, null).firstVerse)
    }
}
