package com.biehuale.app.ui.bill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.bill.MonthlySummary
import com.biehuale.app.ui.common.AmountRole
import com.biehuale.app.ui.common.AmountText
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.BrandInkDark
import com.biehuale.app.ui.theme.BrandInkLight
import com.biehuale.app.ui.theme.LocalIsDarkTheme
import com.biehuale.app.ui.theme.MoneyRowStyle
import com.biehuale.app.ui.theme.ScreenTitleStyle
import com.biehuale.app.util.Money.toDisplayString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * 账单首屏 Hero：月份居中 + 本月已花 + 收入/结余 + 细比例轨。
 */
@Composable
fun SummaryCard(
    summary: MonthlySummary,
    year: Int,
    month: Int,
    onShiftMonth: (Int) -> Unit,
    onPickCustomRange: (Long?, Long?) -> Unit,
    onClearCustomRange: () -> Unit,
    isCustomRange: Boolean,
    customRangeStart: Long?,
    customRangeEnd: Long?,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val ratio = summary.expenseIncomeRatioOrNull
    val ink = if (LocalIsDarkTheme.current) BrandInkDark else BrandInkLight
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xl)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧：chevron + 占位，与右侧 chevron+日历 同宽，月份几何居中
            Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.CenterStart) {
                IconButton(onClick = { onShiftMonth(-1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上一月"
                    )
                }
            }
            Text(
                text = if (isCustomRange && customRangeStart != null && customRangeEnd != null) {
                    "${formatDate(customRangeStart)} – ${formatDate(customRangeEnd)}"
                } else {
                    "${year}年${month}月"
                },
                style = ScreenTitleStyle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(
                modifier = Modifier.width(96.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onShiftMonth(1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下一月"
                    )
                }
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = "选择区间",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        Text(
            text = "本月已花",
            style = MaterialTheme.typography.labelLarge,
            color = secondary
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        AmountText(
            amountCents = summary.expenseCents,
            type = TransactionType.EXPENSE,
            role = AmountRole.Hero,
            color = ink,
            showSign = false,
            animateEntrance = true
        )

        Spacer(modifier = Modifier.height(AppSpacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "收入 ", style = MaterialTheme.typography.bodyMedium, color = secondary)
            Text(
                text = summary.incomeCents.toDisplayString(),
                style = MoneyRowStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = secondary
            )
            Text(text = "  ·  结余 ", style = MaterialTheme.typography.bodyMedium, color = secondary)
            Text(
                text = summary.netCents.toDisplayString(),
                style = MoneyRowStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = secondary
            )
        }
        if (summary.transferCents > 0) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "转账 ", style = MaterialTheme.typography.labelMedium, color = secondary)
                Text(
                    text = summary.transferCents.toDisplayString(),
                    style = MoneyRowStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                    color = secondary
                )
            }
        }

        if (ratio != null) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            val expenseShare = if (summary.incomeCents + summary.expenseCents <= 0L) {
                0f
            } else {
                summary.expenseCents.toFloat() /
                    (summary.incomeCents + summary.expenseCents).toFloat()
            }
            val progress = min(expenseShare.coerceIn(0f, 1f), 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppSemanticColors.expense.copy(alpha = 0.85f))
                )
            }
        }

        if (isCustomRange) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            TextButton(onClick = onClearCustomRange) {
                Text("清除自定义区间")
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialogWrapper(
            onConfirm = { start, end ->
                showDatePicker = false
                onPickCustomRange(start, end)
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun DateRangePickerDialogWrapper(
    onConfirm: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                onClick = {
                    onConfirm(state.selectedStartDateMillis, state.selectedEndDateMillis)
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DateRangePicker(
            state = state,
            title = { Text("选择起止日期") },
            showModeToggle = false
        )
    }
}

private fun formatDate(millis: Long): String {
    val fmt = SimpleDateFormat("MM-dd", Locale.CHINA)
    return fmt.format(Date(millis))
}
