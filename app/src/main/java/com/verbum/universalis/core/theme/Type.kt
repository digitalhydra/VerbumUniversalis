package com.verbum.universalis.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Note: Fonts will be loaded from res/font in actual implementation
val SourceSerifPro = FontFamily.Serif
val Inter = FontFamily.SansSerif

val VerbumTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = SourceSerifPro,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp // ~1.6x
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    )
)
