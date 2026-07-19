package com.biehuale.app.ui.bill.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biehuale.app.ui.bill.MonthlySummary
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.Money.toDisplayString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/** Monthly summary card with expense/income ratio bar. */
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onShiftMonth(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "\u4e0a\u4e00\u6708")
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isCustomRange && customRangeStart != null && customRangeEnd != null) {
                    Text(
                        text = "${formatDate(customRangeStart)} - ${formatDate(customRangeEnd)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = "${year}\u5e74${month}\u6708",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onShiftMonth(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "\u4e0b\u4e00\u6708")
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "\u9009\u62e9\u533a\u95f4")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\u652f\u51fa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = summary.expenseCents.toDisplayString(),
                style = MaterialTheme.typography.displaySmall,
                color = AppSemanticColors.expense,
                fontFamily = MoneyFontFamily,
                fontWeight = FontWeight.SemiBold
            )

            if (ratio != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = min(ratio, 1f)
                val pctText = "${(ratio * 100f).toInt()}%"
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AppSemanticColors.expense,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (ratio > 1f) {
                        "\u652f\u51fa\u5df2\u8d85\u8fc7\u6536\u5165 $pctText"
                    } else {
                        "\u652f\u51fa\u5360\u6536\u5165 $pctText"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatColumn(
                    label = "\u6536\u5165",
                    value = summary.incomeCents.toDisplayString(),
                    color = AppSemanticColors.income,
                    modifier = Modifier.weight(1f)
                )
                StatColumn(
                    label = "\u7ed3\u4f59",
                    value = summary.netCents.toDisplayString(),
                    color = if (summary.netCents >= 0) AppSemanticColors.income else AppSemanticColors.expense,
                    modifier = Modifier.weight(1f)
                )
                StatColumn(
                    label = "\u8f6c\u8d26",
                    value = summary.transferCents.toDisplayString(),
                    color = AppSemanticColors.transfer,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isCustomRange) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = onClearCustomRange,
                    label = { Text("\u6e05\u9664\u81ea\u5b9a\u4e49\u533a\u95f4") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
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
private fun StatColumn(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontFamily = MoneyFontFamily,
            fontWeight = FontWeight.SemiBold
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
            ) { Text("\u786e\u5b9a") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") }
        }
    ) {
        DateRangePicker(
            state = state,
            title = { Text("\u9009\u62e9\u8d77\u6b62\u65e5\u671f") },
            showModeToggle = false
        )
    }
}

private fun formatDate(millis: Long): String {
    val fmt = SimpleDateFormat("MM-dd", Locale.CHINA)
    return fmt.format(Date(millis))
}
