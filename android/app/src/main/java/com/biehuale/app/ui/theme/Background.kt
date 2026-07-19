package com.biehuale.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 纸感氛围底：竖向微渐变（浅色晨雾青 / 深色墨绿炭）。
 */
@Composable
fun AppScaffoldBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val brush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(SurfaceBaseDark, SurfaceBaseDarkEnd)
        } else {
            listOf(SurfaceBaseLight, SurfaceBaseLightEnd)
        }
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
            .background(
                // 2–3% 冷青噪点感（极淡叠色，避免塑料纯色）
                if (isDark) Color(0x08006C5C) else Color(0x0A006C5C)
            ),
        content = content
    )
}
