package com.biehuale.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.biehuale.app.ui.theme.AppSpacing

/**
 * 列表分区小标题（账单「最近」、类别支出/收入、筛选分区等）。
 */
@Composable
fun ListSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalPadding: Dp = AppSpacing.md,
    verticalPadding: Dp = AppSpacing.sm
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 0.8.sp,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
    )
}
