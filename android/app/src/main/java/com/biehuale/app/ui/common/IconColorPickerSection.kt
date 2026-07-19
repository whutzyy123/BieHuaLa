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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("颜色", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconColorPresets.COLORS.forEach { c ->
                val selected = c.equals(colorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(parseColorOrDefault(c, MaterialTheme.colorScheme.primary))
                        .clickable { onColorChange(c) }
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
        Text("图标：$icon", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            icons.forEach { key ->
                FilterChip(
                    selected = icon == key,
                    onClick = { onIconChange(key) },
                    label = { Text(key, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

fun parseColorOrDefault(hex: String?, fallback: Color): Color = try {
    if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    fallback
}
