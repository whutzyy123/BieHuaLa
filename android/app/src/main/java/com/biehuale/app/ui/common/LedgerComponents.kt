package com.biehuale.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.biehuale.app.ui.theme.AppMotion
import com.biehuale.app.ui.theme.AppRadius
import com.biehuale.app.ui.theme.AppSpacing
import kotlinx.coroutines.delay

/**
 * 圆润填充字段：无灰边 Outlined 脸。
 */
@Composable
fun LedgerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AppSpacing.xs)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = AppSpacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClick != null || readOnly) {
                Text(
                    text = value.ifEmpty { " " },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE
                )
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = keyboardOptions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.weight(1f)
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                trailing()
            }
        }
    }
}

/**
 * 纸感圆润 BottomSheet 壳。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = AppRadius.lg, topEnd = AppRadius.lg),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = AppSpacing.sm, bottom = AppSpacing.xs)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md)
                .padding(bottom = AppSpacing.lg),
            content = content
        )
    }
}

/**
 * 圆润确认面板，替代默认 AlertDialog 脸。
 */
@Composable
fun LedgerConfirm(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "取消",
    confirmIsDestructive: Boolean = false
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shown = false
        delay(16)
        shown = true
    }
    val enterProgress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = AppMotion.sheet(),
        label = "confirmEnter"
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = enterProgress
                    scaleX = 0.96f + 0.04f * enterProgress
                    scaleY = 0.96f + 0.04f * enterProgress
                }
                .clip(RoundedCornerShape(AppRadius.md))
                .background(MaterialTheme.colorScheme.surface)
                .padding(AppSpacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(dismissText)
                }
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                if (confirmIsDestructive) {
                    TextButton(onClick = onConfirm) {
                        Text(confirmText, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(onClick = onConfirm) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}
