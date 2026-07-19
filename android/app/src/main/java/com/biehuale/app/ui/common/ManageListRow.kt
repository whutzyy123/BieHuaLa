package com.biehuale.app.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biehuale.app.ui.theme.AppSpacing

/**
 * 管理列表行：色圆图标 + 标题/副标题 + 可选 trailing（⋮ 等）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManageListRow(
    title: String,
    subtitle: String? = null,
    iconKey: String?,
    colorHex: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    titleAccessory: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = AppSpacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconCircle(
                iconKey = iconKey,
                colorHex = colorHex
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm + AppSpacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleAccessory != null) {
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        titleAccessory()
                    }
                }
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
                Spacer(modifier = Modifier.width(AppSpacing.xs))
            }
            if (trailing != null) {
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
