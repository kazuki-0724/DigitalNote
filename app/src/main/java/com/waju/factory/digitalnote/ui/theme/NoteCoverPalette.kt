package com.waju.factory.digitalnote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val NoteCoverColors = listOf(
    Color(0xFF4F8CFF),
    Color(0xFF6366F1),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFFF59E0B),
    Color(0xFF84CC16),
    Color(0xFF14B8A6),
    Color(0xFF06B6D4)
)

fun contentColorForCover(color: Color): Color {
    return if (color.luminance() > 0.55f) Color.Black else Color.White
}

