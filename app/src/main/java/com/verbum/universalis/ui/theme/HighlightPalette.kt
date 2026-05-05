package com.verbum.universalis.ui.theme

import androidx.compose.ui.graphics.Color

// 20 Desaturated Colors for "Bear" Aesthetic
// Muted Pastels and Earth Tones (Sage, Dusty Rose, Steel Blue, Ochre)
val HighlightPalette = listOf(
    Color(0xFF93B7A), // Sage Green
    Color(0xFFC6A7A), // Dusty Rose
    Color(0xFF7EA8C), // Steel Blue
    Color(0xFFD4A373), // Ochre
    Color(0xFFB8A9C), // Mauve
    Color(0xFF8DAEAD), // Pale Aqua
    Color(0xFFE8D5B), // Wheat
    Color(0xFFA8B5B), // Dusty Lavender
    Color(0xFFC4A882), // Sand
    Color(0xFF88A098), // Muted Teal
    Color(0xFFD4A08), // Peach
    Color(0xFFB0A8A0), // Rose Taupe
    Color(0xFF9CB8A8), // Seafoam
    Color(0xFFC0B0A0), // Bisque
    Color(0xFF8CB4A8), // Muted Mint
    Color(0xFFD0B8A8), // Apricot
    Color(0xFFA0A0B0), // Grey Lavender
    Color(0xFFC8B09A), // Muted Salmon
    Color(0xFF90A8B0), // Celadon
    Color(0xFFD8C8B0)  // Straw
)

// Helper to get color by ID (0-19)
fun getHighlightColor(colorId: Int): Color {
    return HighlightPalette[colorId % HighlightPalette.size]
}

// Check contrast against background
fun getHighlightBackground(colorId: Int, isDarkMode: Boolean): Color {
    val baseColor = getHighlightColor(colorId)
    return if (isDarkMode) {
        baseColor.copy(alpha = 0.3f) // More transparent on dark
    } else {
        baseColor.copy(alpha = 0.2f) // Subtle tint on light
    }
}
