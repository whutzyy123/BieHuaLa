package com.biehuale.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppMotion
import com.biehuale.app.ui.theme.AppRadius
import com.biehuale.app.ui.theme.AppSpacing

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(".", "0", "⌫")
)

/**
 * 自定义数字键盘 — 轻表面、按压缩微反馈；键高 48、边距由外层统一。
 */
@Composable
fun MoneyKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        KEYPAD_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                row.forEach { key ->
                    val onClick = remember(key, onDigit, onDelete) {
                        {
                            when (key) {
                                "⌫" -> onDelete()
                                "." -> onDigit('.')
                                else -> onDigit(key[0])
                            }
                        }
                    }
                    val onLongClick = remember(key, onClear) {
                        if (key == "⌫") onClear else null
                    }
                    KeypadButton(
                        key = key,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.xs))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isBackspace = key == "⌫"
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = AppMotion.keyPress(),
        label = "keyScale"
    )
    val bg = MaterialTheme.colorScheme.surfaceContainerLow
    val fg = MaterialTheme.colorScheme.onSurface
    val tag = remember(key) {
        when (key) {
            "⌫" -> "keypad_backspace"
            "." -> "keypad_dot"
            else -> "keypad_$key"
        }
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(AppRadius.sm))
            .testTag(tag)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            color = if (isBackspace) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                bg
            },
            shape = RoundedCornerShape(AppRadius.sm),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {}
        if (isBackspace) {
            Icon(
                imageVector = BhlIcons.Backspace,
                contentDescription = "删除",
                tint = fg
            )
        } else {
            Text(
                text = key,
                style = MaterialTheme.typography.titleLarge,
                color = fg
            )
        }
    }
}
