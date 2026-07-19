package com.biehuale.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 类别/账户色圆 + 中心图标（UI_DESIGN §3.5）。
 */
@Composable
fun CategoryIconCircle(
    iconKey: String?,
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.55f,
    fallbackColor: Color = MaterialTheme.colorScheme.primary,
    backgroundAlpha: Float = 0.18f,
    icon: ImageVector? = null
) {
    val circle = remember(colorHex, fallbackColor) {
        CategoryIconMap.circleColor(colorHex, fallbackColor)
    }
    val image = icon ?: CategoryIconMap.iconFor(iconKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(circle.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = image,
            contentDescription = null,
            tint = circle,
            modifier = Modifier.size(iconSize)
        )
    }
}
