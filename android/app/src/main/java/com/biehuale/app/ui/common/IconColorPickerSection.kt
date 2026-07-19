package com.biehuale.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 账户/类别编辑共用的颜色条 + 图标行。
 */
@Composable
fun IconColorPickerSection(
    colorHex: String,
    onColorChange: (String) -> Unit,
    icon: String,
    onIconChange: (String) -> Unit,
    icons: List<String>,
    modifier: Modifier = Modifier
) {
    val circle = CategoryIconMap.circleColor(colorHex, MaterialTheme.colorScheme.primary)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("颜色", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconColorPresets.COLORS.forEach { c ->
                val selected = c.equals(colorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parseColorOrDefault(c, MaterialTheme.colorScheme.primary))
                        .clickable { onColorChange(c) }
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            }
                        )
                )
            }
        }
        Text("图标", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icons.forEach { key ->
                val selected = icon == key
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) circle.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { onIconChange(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconMap.iconFor(key),
                        contentDescription = key,
                        tint = if (selected) circle else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

fun parseColorOrDefault(hex: String?, fallback: Color): Color = try {
    if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    fallback
}
