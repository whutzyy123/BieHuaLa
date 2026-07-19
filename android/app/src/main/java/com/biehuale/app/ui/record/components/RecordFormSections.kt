package com.biehuale.app.ui.record.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.ui.common.CategoryIconCircle
import com.biehuale.app.ui.common.LedgerField
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.record.QuickRecordOption
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.util.DateExt.toDateTimeString
import com.biehuale.app.util.Money
import com.biehuale.app.util.Money.toDisplayString
import java.util.Calendar
import java.util.TimeZone

@Composable
fun TransferSection(
    amountDisplay: String,
    feeDisplay: String,
    fromAccountId: Long?,
    toAccountId: Long?,
    accounts: List<AccountEntity>,
    onFromAccountSelect: (Long) -> Unit,
    onToAccountSelect: (Long) -> Unit,
    onFeeChange: (String) -> Unit,
    onManageAccounts: () -> Unit = {}
) {
    val fromName = accounts.firstOrNull { it.id == fromAccountId }?.name ?: "转出账户"
    val toName = accounts.firstOrNull { it.id == toAccountId }?.name ?: "转入账户"
    val amountCents = Money.parseToCents(
        if (amountDisplay.endsWith(".")) amountDisplay + "0" else amountDisplay
    ) ?: 0L
    val feeCents = Money.parseToCents(
        feeDisplay.trim().ifEmpty { "0" }.let {
            if (it.endsWith(".")) it + "0" else it
        }
    ) ?: 0L
    val receiveCents = (amountCents - feeCents).coerceAtLeast(0L)
    val amountText = if (amountCents > 0L) amountCents.toDisplayString() else "¥0.00"
    val receiveText = if (amountCents > 0L) receiveCents.toDisplayString() else "¥0.00"
    val preview = if (feeCents > 0L) {
        "$amountText → $fromName -$amountText → $toName +$receiveText"
    } else {
        "$amountText → $fromName -$amountText → $toName +$amountText"
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm + AppSpacing.xs)
    ) {
        Text(
            text = preview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (feeCents > 0L) {
                "手续费从到账中扣除；转账本金不计入本月支出"
            } else {
                "转账不影响本月总支出；可选填手续费"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            AccountDropdown(
                label = "转出",
                accounts = accounts.filter { it.id != toAccountId },
                selectedId = fromAccountId,
                onSelect = onFromAccountSelect,
                onManageAccounts = onManageAccounts,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = BhlIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AccountDropdown(
                label = "转入",
                accounts = accounts.filter { it.id != fromAccountId },
                selectedId = toAccountId,
                onSelect = onToAccountSelect,
                onManageAccounts = onManageAccounts,
                modifier = Modifier.weight(1f)
            )
        }

        LedgerField(
            value = feeDisplay,
            onValueChange = onFeeChange,
            label = "手续费（可选）",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            trailing = {
                Text(
                    text = "元",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun QuickRecordDropdown(
    options: List<QuickRecordOption>,
    enabled: Boolean = true,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = when {
        options.isEmpty() -> "暂无 · 去设置"
        else -> "点选即可记账（${options.size}）"
    }

    Box(modifier = modifier) {
        LedgerField(
            value = displayName,
            onValueChange = {},
            label = "快速记账",
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailing = { Text("▾") },
            onClick = {
                if (!enabled) return@LedgerField
                if (options.isEmpty()) {
                    onManage()
                } else {
                    expanded = true
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        QuickRecordMenuRow(option = option)
                    },
                    onClick = {
                        onSelect(option.entity.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickRecordMenuRow(option: QuickRecordOption) {
    val category = option.category
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIconCircle(
            iconKey = category?.icon,
            colorHex = category?.colorHex,
            size = 28.dp
        )
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Column {
            Text(
                text = buildString {
                    append(category?.name ?: "类别")
                    val desc = option.entity.description?.takeIf { it.isNotBlank() }
                    if (desc != null) {
                        append(" · ")
                        append(desc)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.entity.amount.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onManageCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    val displayName = selected?.name
        ?: if (categories.isEmpty()) "无类别 · 去管理" else "选择类别"

    Box(modifier = modifier) {
        LedgerField(
            value = displayName,
            onValueChange = {},
            label = "类别",
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailing = { Text("▾") },
            onClick = {
                if (categories.isEmpty()) {
                    onManageCategories()
                } else {
                    expanded = true
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        CategoryDropdownRow(category = category)
                    },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryDropdownRow(category: CategoryEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIconCircle(
            iconKey = category.icon,
            colorHex = category.colorHex,
            size = 28.dp
        )
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AccountAndTimeRow(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    occurredAt: Long,
    onAccountSelect: (Long) -> Unit,
    onTimeChange: (Long) -> Unit,
    onManageAccounts: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        AccountDropdown(
            label = "账户",
            accounts = accounts,
            selectedId = selectedAccountId,
            onSelect = onAccountSelect,
            onManageAccounts = onManageAccounts,
            modifier = Modifier.weight(1f)
        )
        TimeField(
            occurredAt = occurredAt,
            onTimeChange = onTimeChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onManageAccounts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val displayName = selected?.name ?: if (accounts.isEmpty()) "无账户 · 去管理" else "选择账户"

    Box(modifier = modifier) {
        LedgerField(
            value = displayName,
            onValueChange = {},
            label = label,
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailing = { Text("▾") },
            onClick = {
                if (accounts.isEmpty()) {
                    onManageAccounts()
                } else {
                    expanded = true
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onSelect(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    occurredAt: Long,
    onTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDayMillis by remember { mutableStateOf(occurredAt) }

    val calendar = remember(occurredAt) {
        Calendar.getInstance().apply { timeInMillis = occurredAt }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.run {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LedgerField(
                value = occurredAt.toDateTimeString(),
                onValueChange = {},
                label = "时间",
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true }
            )
        }
        TextButton(onClick = { onTimeChange(System.currentTimeMillis()) }) {
            Text("现在")
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            // DatePicker 返回 UTC 日界；转本地日再进时间选择
                            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selected
                            }
                            pendingDayMillis = Calendar.getInstance().apply {
                                set(
                                    utc.get(Calendar.YEAR),
                                    utc.get(Calendar.MONTH),
                                    utc.get(Calendar.DAY_OF_MONTH),
                                    0, 0, 0
                                )
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val day = Calendar.getInstance().apply {
                            timeInMillis = pendingDayMillis
                        }
                        val combined = Calendar.getInstance().apply {
                            set(Calendar.YEAR, day.get(Calendar.YEAR))
                            set(Calendar.MONTH, day.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        onTimeChange(combined)
                        showTimePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    LedgerField(
        value = description,
        onValueChange = onDescriptionChange,
        label = "说明（可选）",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}
