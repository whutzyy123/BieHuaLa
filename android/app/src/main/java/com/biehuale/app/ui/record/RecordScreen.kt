package com.biehuale.app.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.ui.common.MoneyKeypad
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.BieHuaLeTheme
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.DateExt.toDateTimeString
import com.biehuale.app.util.Money.toDisplayString

/**
 * 记账 Tab - Screen
 *
 * Phase 1: 支出/收入
 * Phase 2: 加 TRANSFER 转账 + 编辑模式（loadForEdit）
 */
@Composable
fun RecordScreen(
    transactionId: Long? = null,
    onSavedAndExit: () -> Unit = {},
    onNavigateToBill: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 编辑模式：加载交易数据
    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId > 0) {
            viewModel.loadForEdit(transactionId)
        } else {
            viewModel.resetToNewMode()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordEvent.SaveSuccess -> {
                    val msg = if (event.wasEditing) "已更新" else "已保存"
                    snackbarHostState.showSnackbar(msg)
                    if (event.wasEditing) {
                        onSavedAndExit()
                    } else {
                        onNavigateToBill()
                    }
                }
                is RecordEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 编辑模式提示
            if (isEditing != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "编辑模式",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // 模式选择
            ModeSelector(
                mode = uiState.mode,
                onModeChange = viewModel::onModeChange
            )

            // 金额大字号
            AmountDisplay(
                amountDisplay = uiState.amountDisplay,
                mode = uiState.mode
            )

            HorizontalDivider()

            // 转账模式 vs 普通模式
            if (uiState.mode == RecordMode.TRANSFER) {
                TransferSection(
                    amountDisplay = uiState.amountDisplay,
                    fromAccountId = uiState.selectedAccountId,
                    toAccountId = uiState.toAccountId,
                    accounts = uiState.accounts,
                    onFromAccountSelect = viewModel::onAccountSelect,
                    onToAccountSelect = viewModel::onToAccountSelect
                )
            } else {
                CategoryGrid(
                    categories = uiState.currentCategories,
                    selectedId = uiState.selectedCategoryId,
                    onSelect = viewModel::onCategorySelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                AccountAndTimeRow(
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedAccountId,
                    occurredAt = uiState.occurredAt,
                    onAccountSelect = viewModel::onAccountSelect,
                    onTimeChange = viewModel::onTimeChange
                )
            }

            // 公共：时间
            if (uiState.mode != RecordMode.TRANSFER) {
                // 已经在 AccountAndTimeRow 中显示
            } else {
                TimeField(
                    occurredAt = uiState.occurredAt,
                    onTimeChange = viewModel::onTimeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            DescriptionField(
                description = uiState.description,
                onDescriptionChange = viewModel::onDescriptionChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 数字键盘 + 保存按钮
            BottomSection(
                amountDisplay = uiState.amountDisplay,
                canSave = uiState.canSave,
                isEditing = isEditing != null,
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDeleteChar,
                onClear = viewModel::onClear,
                onSave = viewModel::onSave
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ============================================================
// 子组件
// ============================================================

@Composable
private fun ModeSelector(
    mode: RecordMode,
    onModeChange: (RecordMode) -> Unit
) {
    val selectedIndex = mode.ordinal
    TabRow(selectedTabIndex = selectedIndex) {
        Tab(
            selected = mode == RecordMode.EXPENSE,
            onClick = { onModeChange(RecordMode.EXPENSE) },
            text = { Text("支出") }
        )
        Tab(
            selected = mode == RecordMode.INCOME,
            onClick = { onModeChange(RecordMode.INCOME) },
            text = { Text("收入") }
        )
        Tab(
            selected = mode == RecordMode.TRANSFER,
            onClick = { onModeChange(RecordMode.TRANSFER) },
            text = { Text("转账") }
        )
    }
}

@Composable
private fun AmountDisplay(
    amountDisplay: String,
    mode: RecordMode
) {
    val sign = when (mode) {
        RecordMode.EXPENSE -> "-"
        RecordMode.INCOME -> "+"
        RecordMode.TRANSFER -> ""
    }
    val color = when (mode) {
        RecordMode.EXPENSE -> AppSemanticColors.expense
        RecordMode.INCOME -> AppSemanticColors.income
        RecordMode.TRANSFER -> AppSemanticColors.transfer
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sign,
                style = MaterialTheme.typography.displaySmall,
                color = color,
                fontFamily = MoneyFontFamily
            )
            Text(
                text = if (amountDisplay == "0") "0.00" else amountDisplay,
                style = MaterialTheme.typography.displayLarge,
                color = color,
                fontFamily = MoneyFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TransferSection(
    amountDisplay: String,
    fromAccountId: Long?,
    toAccountId: Long?,
    accounts: List<AccountEntity>,
    onFromAccountSelect: (Long) -> Unit,
    onToAccountSelect: (Long) -> Unit
) {
    val fromName = accounts.firstOrNull { it.id == fromAccountId }?.name ?: "转出账户"
    val toName = accounts.firstOrNull { it.id == toAccountId }?.name ?: "转入账户"
    val amountText = run {
        val cents = com.biehuale.app.util.Money.parseToCents(
            if (amountDisplay.endsWith(".")) amountDisplay + "0" else amountDisplay
        ) ?: 0L
        if (cents > 0L) cents.toDisplayString() else "¥0.00"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountDropdown(
                label = "转出",
                accounts = accounts.filter { it.id != toAccountId },
                selectedId = fromAccountId,
                onSelect = onFromAccountSelect,
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
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
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
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
        MaterialTheme.colorScheme.surface
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
            .clip(RoundedCornerShape(8.dp)),
        color = containerColor,
        border = if (selected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AccountAndTimeRow(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    occurredAt: Long,
    onAccountSelect: (Long) -> Unit,
    onTimeChange: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccountDropdown(
            label = "账户",
            accounts = accounts,
            selectedId = selectedAccountId,
            onSelect = onAccountSelect,
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
private fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val displayName = selected?.name ?: if (accounts.isEmpty()) "无账户" else "选择账户"

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { expanded = true }) {
                    Text("▾")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (accounts.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("请到「设置」新建账户") },
                    onClick = { expanded = false }
                )
            } else {
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
}

@Composable
private fun TimeField(
    occurredAt: Long,
    onTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = occurredAt.toDateTimeString(),
        onValueChange = {},
        readOnly = true,
        label = { Text("时间") },
        modifier = modifier,
        trailingIcon = {
            TextButton(onClick = { onTimeChange(System.currentTimeMillis()) }) {
                Text("现在")
            }
        }
    )
}

@Composable
private fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("说明（可选）") },
        placeholder = { Text("写点什么吧…") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        maxLines = 1,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}

@Composable
private fun BottomSection(
    amountDisplay: String,
    canSave: Boolean,
    isEditing: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    Column {
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp)
        ) {
            Text(if (isEditing) "更新" else "保存", style = MaterialTheme.typography.titleMedium)
        }

        MoneyKeypad(
            onDigit = onDigit,
            onDelete = onDelete,
            onClear = onClear
        )
    }
}


@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecordScreenPreview() {
    BieHuaLeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFCFCFC)),
            contentAlignment = Alignment.Center
        ) {
            Text("RecordScreen Preview（需 Hilt）")
        }
    }
}
