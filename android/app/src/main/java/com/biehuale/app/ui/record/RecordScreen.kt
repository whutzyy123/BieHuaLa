package com.biehuale.app.ui.record

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.ui.common.MoneyKeypad
import com.biehuale.app.ui.record.components.AccountAndTimeRow
import com.biehuale.app.ui.record.components.CategoryGrid
import com.biehuale.app.ui.record.components.DescriptionField
import com.biehuale.app.ui.record.components.TimeField
import com.biehuale.app.ui.record.components.TransferSection
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.BieHuaLeTheme
import com.biehuale.app.ui.theme.MoneyFontFamily

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

            ModeSelector(
                mode = uiState.mode,
                onModeChange = viewModel::onModeChange
            )

            AmountDisplay(
                amountDisplay = uiState.amountDisplay,
                mode = uiState.mode
            )

            HorizontalDivider()

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

            if (uiState.mode == RecordMode.TRANSFER) {
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

            BottomSection(
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
private fun BottomSection(
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("RecordScreen Preview（需 Hilt）")
        }
    }
}
