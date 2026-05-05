package com.verbum.universalis.core.theme

import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.compose.ui.graphics.Color

class ThemeTest {
    @Test
    fun `test color palette values`() {
        assertEquals(Color(0xFFD9CFCC), LightGold)
        assertEquals(Color(0xFF121212), DeepCharcoal)
        assertEquals(Color(0xFFFFFFFF), White)
    }
}
