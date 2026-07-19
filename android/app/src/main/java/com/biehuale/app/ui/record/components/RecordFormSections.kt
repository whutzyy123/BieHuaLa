package com.biehuale.app.ui.record.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.ui.theme.AppRadius
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.util.DateExt.toDateTimeString
import com.biehuale.app.util.Money
import com.biehuale.app.util.Money.toDisplayString
import java.util.Calendar
import java.util.TimeZone

@Composable
fun TransferSection(
    amountDisplay: String,
    fromAccountId: Long?,
    toAccountId: Long?,
    accounts: List<AccountEntity>,
    onFromAccountSelect: (Long) -> Unit,
    onToAccountSelect: (Long) -> Unit,
    onManageAccounts: () -> Unit = {}
) {
    val fromName = accounts.firstOrNull { it.id == fromAccountId }?.name ?: "转出账户"
    val toName = accounts.firstOrNull { it.id == toAccountId }?.name ?: "转入账户"
    val amountText = run {
        val cents = Money.parseToCents(
            if (amountDisplay.endsWith(".")) amountDisplay + "0" else amountDisplay
        ) ?: 0L
        if (cents > 0L) cents.toDisplayString() else "¥0.00"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm + AppSpacing.xs)
    ) {
        Text(
            text = "$amountText → $fromName -$amountText → $toName +$amountText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "转账不影响本月总支出",
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
    }
}

@Composable
fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier
                .height(120.dp)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无类别，请到「设置」添加",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.heightIn(max = 200.dp),
        contentPadding = PaddingValues(AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryChip(
                category = category,
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(AppRadius.sm)),
        color = containerColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else Color.Transparent
        ),
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.xs)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
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
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            shape = RoundedCornerShape(AppRadius.sm),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = { Text("▾") }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
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
            OutlinedTextField(
                value = occurredAt.toDateTimeString(),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("时间") },
                shape = RoundedCornerShape(AppRadius.sm),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
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
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("说明（可选）") },
        placeholder = { Text("写点什么吧…") },
        shape = RoundedCornerShape(AppRadius.sm),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        singleLine = true,
        maxLines = 1,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}
