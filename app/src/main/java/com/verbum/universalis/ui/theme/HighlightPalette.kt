package com.verbum.universalis.ui.theme

import androidx.compose.ui.graphics.Color

// 20 Desaturated Colors for "Bear" Aesthetic
// Muted Pastels and Earth Tones (Sage, Dusty Rose, Steel Blue, Ochre)
object HighlightPalette {
    val all = listOf(
        Color(0xFFE9F5DB), // Sage Green
        Color(0xFFF5E6E8), // Dusty Rose
        Color(0xFFD8E2DC), // Steel Blue
        Color(0xFFFAE1DD), // Ochre
        Color(0xFFECE4DB), // Mauve
        Color(0xFFFFE5D9), // Pale Aqua
        Color(0xFFFFCAD4), // Wheat
        Color(0xFFF4ACB7), // Dusty Lavender
        Color(0xFF9D8189), // Sand
        Color(0xFFFFE5B4), // Muted Teal
        Color(0xFFE0E0E0), // Peach
        Color(0xFFD3D3D3), // Rose Taupe
        Color(0xFFB0C4DE), // Seafoam
        Color(0xFFADD8E6), // Bisque
        Color(0xFFF0E68C), // Muted Mint
        Color(0xFFE6E6FA), // Apricot
        Color(0xFFFAF0E6), // Grey Lavender
        Color(0xFFFFF5EE), // Muted Salmon
        Color(0xFFF5F5DC), // Celadon
        Color(0xFFFDF5E6)  // Straw
    )
}

// Helper to get color by ID (0-19)
fun getHighlightColor(colorId: Int): Color {
    return HighlightPalette.all[colorId % HighlightPalette.all.size]
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