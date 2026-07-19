package com.biehuale.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.common.AmountRole
import com.biehuale.app.ui.common.AmountText
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.SubScreenScaffold
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.util.DateExt.toDateTimeString
import com.biehuale.app.util.Money.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TransactionDetailEvent.Deleted -> onBack()
                is TransactionDetailEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SubScreenScaffold(
        title = "交易详情",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.notFound -> CenterText("交易不存在或已删除")
                else -> DetailContent(
                    state = uiState,
                    onEdit = { uiState.transaction?.let { onEdit(it.id) } },
                    onDelete = { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        LedgerConfirm(
            title = "删除这笔交易？",
            message = "将移出列表（软删除）。可在设置 → 回收站恢复。",
            confirmText = "删除",
            confirmIsDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.softDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailContent(
    state: TransactionDetailUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tx = state.transaction ?: return
    val typeLabel = when (tx.type) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
        TransactionType.TRANSFER -> "转账"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        SectionPanel {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                AmountText(
                    amountCents = tx.amount,
                    type = tx.type,
                    role = AmountRole.Detail,
                    animateEntrance = true
                )
            }
        }

        SectionPanel(contentPadding = 0.dp) {
            DetailItem(
                label = "类别",
                value = when {
                    tx.type == TransactionType.TRANSFER -> "—"
                    state.category == null -> "未分类"
                    state.category.isArchived -> "${state.category.name}（已归档）"
                    else -> state.category.name
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            DetailItem(
                label = if (tx.type == TransactionType.TRANSFER) "转出账户" else "账户",
                value = state.account?.let {
                    if (it.isArchived) "${it.name}（已归档）" else it.name
                } ?: "—"
            )
            if (tx.type == TransactionType.TRANSFER && state.toAccount != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                DetailItem(
                    label = "转入账户",
                    value = state.toAccount.let {
                        if (it.isArchived) "${it.name}（已归档）" else it.name
                    }
                )
                if (tx.fee > 0L) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                    DetailItem(label = "手续费", value = tx.fee.toDisplayString())
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                    DetailItem(
                        label = "到账金额",
                        value = (tx.amount - tx.fee).toDisplayString()
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            DetailItem(label = "时间", value = tx.occurredAt.toDateTimeString())
            if (!tx.description.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                DetailItem(label = "说明", value = tx.description)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onEdit) { Text("编辑") }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = AppSpacing.md)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
