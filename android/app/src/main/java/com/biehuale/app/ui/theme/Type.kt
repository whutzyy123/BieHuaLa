package com.biehuale.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 别花乐 (BieHuaLe) - Typography
 *
 * v0.1.0：使用系统默认字体 + Monospace 作为金额字体。
 * 关键决策：金额用等宽字体（避免"¥35.50"跳来跳去）。
 *
 * 详见 docs/PRD.md §7.1。
 *
 * 未来增强（v0.2+）：
 *  - 引入等宽字体资源（如 JetBrains Mono / Roboto Mono）
 *  - 微调 LineHeight / LetterSpacing
 */

// 金额专用：等宽字体（Phase 2 替换为真实字体）
val MoneyFontFamily: FontFamily = FontFamily.Monospace

val AppTypography = Typography(
    // 金额大数字（账单 Tab 顶部）
    displayLarge = TextStyle(
        fontFamily = MoneyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = MoneyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    // 金额中等（流水列表行）
    titleLarge = TextStyle(
        fontFamily = MoneyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MoneyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // 普通文字
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 标签 / 按钮
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
