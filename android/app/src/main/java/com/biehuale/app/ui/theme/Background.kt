package com.biehuale.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Clarity Teal 全局底：近白 / 近黑实色，无雾青渐变与噪点。
 */
@Composable
fun AppScaffoldBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) SurfaceBaseDark else SurfaceBaseLight),
        content = content
    )
}
