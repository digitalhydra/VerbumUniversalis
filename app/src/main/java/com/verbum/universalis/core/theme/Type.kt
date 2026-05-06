package com.verbum.universalis.core.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.verbum.universalis.R

// Bundled Fonts
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val SourceSerifProFontFamily = FontFamily(
    Font(R.font.sourceserif_regular, FontWeight.Normal),
    Font(R.font.sourceserif_bold, FontWeight.Bold)
)

// Typography Specs
val ContentTypography = androidx.compose.material3.Typography(
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SourceSerifProFontFamily,
        fontWeight = FontWeight.Normal,
        lineHeight = 1.6.em, // 1.6x line height
        letterSpacing = 0.5.sp
    )
)

val UITypography = androidx.compose.material3.Typography(
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold
    )
)
