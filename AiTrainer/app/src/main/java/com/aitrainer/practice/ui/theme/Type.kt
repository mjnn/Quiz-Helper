package com.aitrainer.practice.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 不设死颜色，便于按钮 / Snackbar 等深色容器继承 contentColor */
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        color = Color.Unspecified,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        color = Color.Unspecified,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Unspecified,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Unspecified,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Unspecified,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Unspecified,
        letterSpacing = 0.3.sp,
    ),
)
