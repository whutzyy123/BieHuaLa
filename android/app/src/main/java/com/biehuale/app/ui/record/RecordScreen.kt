package com.biehuale.app.ui.record

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.ui.common.MoneyKeypad
import com.biehuale.app.ui.common.PrimaryButton
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.record.components.AccountAndTimeRow
import com.biehuale.app.ui.record.components.CategoryDropdown
import com.biehuale.app.ui.record.components.DescriptionField
import com.biehuale.app.ui.record.components.QuickRecordDropdown
import com.biehuale.app.ui.record.components.TimeField
import com.biehuale.app.ui.record.components.TransferSection
import com.biehuale.app.ui.theme.AppMotion
import com.biehuale.app.ui.theme.AppRadius
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.ui.theme.MoneyHeroStyle

@Composable
fun RecordScreen(
    transactionId: Long? = null,
    onSavedAndExit: () -> Unit = {},
    onManageAccounts: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onManageQuickRecords: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val amountDisplay by viewModel.amountDisplay.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val editing = isEditing != null

    // 仅编辑进入 / 从编辑退回新建时 reset；Tab restoreState 回切不整树重写
    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId > 0) {
            viewModel.loadForEdit(transactionId)
        } else if (viewModel.isEditing.value != null) {
            viewModel.resetToNewMode()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordEvent.SaveSuccess -> {
                    if (event.wasEditing) {
                        snackbarHostState.showSnackbar("已更新")
                        onSavedAndExit()
                    } else {
                        snackbarHostState.showSnackbar("已保存")
                        viewModel.resetToNewMode()
                    }
                }
                is RecordEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (editing) {
                Text(
                    text = "编辑账单",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                )
            }

            ModeSelector(
                mode = formState.mode,
                enabled = !editing,
                onModeChange = viewModel::onModeChange
            )

            AmountStage(
                amountDisplay = amountDisplay,
                mode = formState.mode
            )

            RecordFormBody(
                formState = formState,
                editing = editing,
                isSaving = isSaving,
                onManageAccounts = onManageAccounts,
                onManageCategories = onManageCategories,
                onManageQuickRecords = onManageQuickRecords,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )

            BottomSection(
                canSave = canSave,
                isEditing = editing,
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
private fun RecordFormBody(
    formState: RecordFormState,
    editing: Boolean,
    isSaving: Boolean,
    onManageAccounts: () -> Unit,
    onManageCategories: () -> Unit,
    onManageQuickRecords: () -> Unit,
    viewModel: RecordViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = AppSpacing.sm)
    ) {
        SectionPanel {
            if (formState.mode == RecordMode.TRANSFER) {
                val transferAmount by viewModel.amountDisplay.collectAsStateWithLifecycle()
                TransferSection(
                    amountDisplay = transferAmount,
                    feeDisplay = formState.feeDisplay,
                    fromAccountId = formState.selectedAccountId,
                    toAccountId = formState.toAccountId,
                    accounts = formState.accounts,
                    onFromAccountSelect = viewModel::onAccountSelect,
                    onToAccountSelect = viewModel::onToAccountSelect,
                    onFeeChange = viewModel::onFeeChange,
                    onManageAccounts = onManageAccounts
                )
                TimeField(
                    occurredAt = formState.occurredAt,
                    onTimeChange = viewModel::onTimeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.xs)
                )
            } else {
                if (formState.mode == RecordMode.EXPENSE && !editing) {
                    QuickRecordDropdown(
                        options = formState.quickRecords,
                        enabled = !isSaving,
                        onSelect = viewModel::onQuickRecord,
                        onManage = onManageQuickRecords,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.xs)
                    )
                }
                CategoryDropdown(
                    categories = formState.currentCategories,
                    selectedId = formState.selectedCategoryId,
                    onSelect = viewModel::onCategorySelect,
                    onManageCategories = onManageCategories,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.xs)
                )
                AccountAndTimeRow(
                    accounts = formState.accounts,
                    selectedAccountId = formState.selectedAccountId,
                    occurredAt = formState.occurredAt,
                    onAccountSelect = viewModel::onAccountSelect,
                    onTimeChange = viewModel::onTimeChange,
                    onManageAccounts = onManageAccounts
                )
            }

            DescriptionField(
                description = formState.description,
                onDescriptionChange = viewModel::onDescriptionChange
            )
        }
    }
}

@Composable
private fun ModeSelector(
    mode: RecordMode,
    enabled: Boolean = true,
    onModeChange: (RecordMode) -> Unit
) {
    val modes = listOf(
        RecordMode.EXPENSE to "支出",
        RecordMode.INCOME to "收入",
        RecordMode.TRANSFER to "转账"
    )
    val selectedIndex = modes.indexOfFirst { it.first == mode }.coerceAtLeast(0)
    // 字重 / 选中色即时；仅指示条做 180ms 位移动画
    val selectedAccent = when (mode) {
        RecordMode.EXPENSE -> AppSemanticColors.expense
        RecordMode.INCOME -> AppSemanticColors.income
        RecordMode.TRANSFER -> AppSemanticColors.transfer
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / 3
            val barWidth = tabWidth * 0.36f
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex + (tabWidth - barWidth) / 2,
                animationSpec = AppMotion.mode(),
                label = "modeIndicator"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    modes.forEach { (m, label) ->
                        val selected = mode == m
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) selectedAccent
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (enabled) 1f else 0.45f
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (enabled) Modifier.clickable { onModeChange(m) }
                                    else Modifier
                                )
                                .padding(vertical = AppSpacing.sm + AppSpacing.xs),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(barWidth)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(selectedAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountStage(
    amountDisplay: String,
    mode: RecordMode
) {
    val accent by animateColorAsState(
        targetValue = when (mode) {
            RecordMode.EXPENSE -> AppSemanticColors.expense
            RecordMode.INCOME -> AppSemanticColors.income
            RecordMode.TRANSFER -> AppSemanticColors.transfer
        },
        animationSpec = AppMotion.mode(),
        label = "amountColor"
    )
    val ink = MaterialTheme.colorScheme.onSurface
    val sign = when (mode) {
        RecordMode.EXPENSE -> "-"
        RecordMode.INCOME -> "+"
        RecordMode.TRANSFER -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.lg + AppSpacing.xs)
            .padding(horizontal = AppSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                style = MoneyHeroStyle.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = accent,
                fontFamily = MoneyFontFamily,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            if (sign.isNotEmpty()) {
                Text(
                    text = sign,
                    style = MoneyHeroStyle,
                    color = accent,
                    fontFamily = MoneyFontFamily
                )
            }
            Text(
                text = if (amountDisplay == "0") "0.00" else amountDisplay,
                style = MoneyHeroStyle,
                color = ink,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = AppSpacing.sm)
    ) {
        PrimaryButton(
            text = if (isEditing) "更新" else "保存",
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
        )

        MoneyKeypad(
            onDigit = onDigit,
            onDelete = onDelete,
            onClear = onClear,
            modifier = Modifier.padding(horizontal = AppSpacing.md)
        )
    }
}
