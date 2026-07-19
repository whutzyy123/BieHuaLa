package com.biehuale.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 别花乐 (BieHuaLe) - Color palette
 *
 * Brand Fallback：青绿色（暗合"乐"字，健康积极）
 * Android 12+ 走 Dynamic Color，Android 11- 走这里的 fallback。
 *
 * 收入/支出色在 Theme.kt 的 ColorScheme 扩展中定义；
 * 类别色以 hex 字符串存库，经 IconColorPresets 解析。
 *
 * 详见 docs/PRD.md §7.1。
 */

val Brand40 = Color(0xFF006C5C)
val Brand80 = Color(0xFF6FF7DD)
val Brand20 = Color(0xFF005047)
