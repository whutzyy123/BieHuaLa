package com.biehuale.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mist Teal Ledger — 设计 Token（见 docs/UI_DESIGN.md §3.1）
 *
 * Brand Fallback：青绿色；语义色不随 Dynamic Color 漂移。
 */

// ---------- Brand ----------
val Brand40 = Color(0xFF006C5C)
val Brand80 = Color(0xFF6FF7DD)
val Brand20 = Color(0xFF005047)

val BrandPrimaryLight = Brand40
val BrandOnPrimaryLight = Color(0xFFFFFFFF)
val BrandContainerLight = Color(0xFFD0F5EE)
val BrandInkLight = Color(0xFF0E1F1C)

val BrandPrimaryDark = Brand80
val BrandOnPrimaryDark = Brand20
val BrandContainerDark = Brand20
val BrandInkDark = Color(0xFFE6F2EF)

// ---------- Semantic（账本方向色）----------
val SemanticExpenseLight = Color(0xFFB42318)
val SemanticIncomeLight = Color(0xFF027A48)
val SemanticTransferLight = Color(0xFF175CD3)

val SemanticExpenseDark = Color(0xFFF97066)
val SemanticIncomeDark = Color(0xFF6CE9A6)
val SemanticTransferDark = Color(0xFF84CAFF)

// ---------- Surface / atmosphere ----------
val SurfaceBaseLight = Color(0xFFF3F7F5)
val SurfaceBaseLightEnd = Color(0xFFEAF3F0)
val SurfaceElevatedLight = Color(0xEBFFFFFF) // ~92% white
val SurfaceMutedLight = Color(0xFFDDE8E4)
val StrokeSubtleLight = Color(0xFFC5D5D0)
val TextSecondaryLight = Color(0xFF5B6B66)

val SurfaceBaseDark = Color(0xFF0C1412)
val SurfaceBaseDarkEnd = Color(0xFF121A18)
val SurfaceElevatedDark = Color(0xFF1A2421)
val SurfaceMutedDark = Color(0xFF24302C)
val StrokeSubtleDark = Color(0xFF2F3D39)
val TextSecondaryDark = Color(0xFFA3B4AF)
