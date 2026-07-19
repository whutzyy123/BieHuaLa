package com.biehuale.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biehuale.app.ui.theme.AppRadius
import com.biehuale.app.ui.theme.AppSpacing

/**
 * 区块面板：elevated 浅底圆角，用于模块分离（非行级 Card）。
 */
@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    horizontalMargin: Dp = AppSpacing.md,
    contentPadding: Dp = AppSpacing.md,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (contentPadding > 0.dp) Modifier.padding(contentPadding)
                else Modifier
            )
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
        }
        content()
    }
}
