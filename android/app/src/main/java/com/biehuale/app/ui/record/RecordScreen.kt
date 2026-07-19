package com.biehuale.app.ui.record

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.ui.common.MoneyKeypad
import com.biehuale.app.ui.record.components.AccountAndTimeRow
import com.biehuale.app.ui.record.components.CategoryGrid
import com.biehuale.app.ui.record.components.DescriptionField
import com.biehuale.app.ui.record.components.TimeField
import com.biehuale.app.ui.record.components.TransferSection
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
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val editing = isEditing != null

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
                mode = uiState.mode,
                enabled = !editing,
                onModeChange = viewModel::onModeChange
            )

            AmountStage(
                amountDisplay = uiState.amountDisplay,
                mode = uiState.mode
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.mode == RecordMode.TRANSFER) {
                    TransferSection(
                        amountDisplay = uiState.amountDisplay,
                        fromAccountId = uiState.selectedAccountId,
                        toAccountId = uiState.toAccountId,
                        accounts = uiState.accounts,
                        onFromAccountSelect = viewModel::onAccountSelect,
                        onToAccountSelect = viewModel::onToAccountSelect,
                        onManageAccounts = onManageAccounts
                    )
                    TimeField(
                        occurredAt = uiState.occurredAt,
                        onTimeChange = viewModel::onTimeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    )
                } else {
                    CategoryGrid(
                        categories = uiState.currentCategories,
                        selectedId = uiState.selectedCategoryId,
                        onSelect = viewModel::onCategorySelect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                    )
                    AccountAndTimeRow(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        occurredAt = uiState.occurredAt,
                        onAccountSelect = viewModel::onAccountSelect,
                        onTimeChange = viewModel::onTimeChange,
                        onManageAccounts = onManageAccounts
                    )
                }

                DescriptionField(
                    description = uiState.description,
                    onDescriptionChange = viewModel::onDescriptionChange
                )
            }

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
    enabled: Boolean = true,
    onModeChange: (RecordMode) -> Unit
) {
    val modes = listOf(
        RecordMode.EXPENSE to "支出",
        RecordMode.INCOME to "收入",
        RecordMode.TRANSFER to "转账"
    )
    val selectedIndex = modes.indexOfFirst { it.first == mode }.coerceAtLeast(0)
    val accent by animateColorAsState(
        targetValue = when (mode) {
            RecordMode.EXPENSE -> AppSemanticColors.expense
            RecordMode.INCOME -> AppSemanticColors.income
            RecordMode.TRANSFER -> AppSemanticColors.transfer
        },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "modeAccent"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / 3
            val barWidth = tabWidth * 0.36f
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex + (tabWidth - barWidth) / 2,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
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
                            color = if (selected) accent
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
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(accent)
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
    val color by animateColorAsState(
        targetValue = when (mode) {
            RecordMode.EXPENSE -> AppSemanticColors.expense
            RecordMode.INCOME -> AppSemanticColors.income
            RecordMode.TRANSFER -> AppSemanticColors.transfer
        },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "amountColor"
    )
    val sign = when (mode) {
        RecordMode.EXPENSE -> "-"
        RecordMode.INCOME -> "+"
        RecordMode.TRANSFER -> ""
    }
    val glow = color.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.lg)
            .background(
                Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    radius = with(LocalDensity.current) { 360.dp.toPx() }
                )
            )
            .padding(horizontal = AppSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                style = MoneyHeroStyle.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = color.copy(alpha = 0.85f),
                fontFamily = MoneyFontFamily,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            if (sign.isNotEmpty()) {
                Text(
                    text = sign,
                    style = MoneyHeroStyle,
                    color = color,
                    fontFamily = MoneyFontFamily
                )
            }
            Text(
                text = if (amountDisplay == "0") "0.00" else amountDisplay,
                style = MoneyHeroStyle,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(bottom = AppSpacing.sm)
    ) {
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(AppRadius.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                .height(52.dp)
        ) {
            Text(
                if (isEditing) "更新" else "保存",
                style = MaterialTheme.typography.titleLarge
            )
        }

        MoneyKeypad(
            onDigit = onDigit,
            onDelete = onDelete,
            onClear = onClear,
            modifier = Modifier.padding(horizontal = AppSpacing.md)
        )
    }
}
