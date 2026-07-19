package com.biehuale.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.SubScreenScaffold
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.DateExt.toDateTimeString
import com.biehuale.app.util.Money.toDisplayString

/**
 * 回收站 Screen（docs/PRD.md §6.2）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RecycleBinItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecycleBinEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is RecycleBinEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SubScreenScaffold(
        title = "回收站",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        actions = {
            if (uiState.items.isNotEmpty()) {
                TextButton(onClick = { showEmptyConfirm = true }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.items.isEmpty() -> EmptyState(
                    title = "回收站是空的",
                    subtitle = "删除的账会在 30 天内保留，可在此恢复",
                    icon = BhlIcons.Restore,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = AppSpacing.sm)
                ) {
                    Text(
                        text = "共 ${uiState.items.size} 项，30 天后自动清理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    )
                    SectionPanel(
                        contentPadding = 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.items, key = { it.transaction.id }) { item ->
                                RecycleBinItemCard(
                                    item = item,
                                    onRestore = { viewModel.restore(item.transaction.id) },
                                    onHardDelete = { pendingDelete = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        LedgerConfirm(
            title = "清空回收站？",
            message = "将永久删除所有 ${uiState.items.size} 项，无法恢复。",
            confirmText = "清空",
            confirmIsDestructive = true,
            onConfirm = {
                showEmptyConfirm = false
                viewModel.emptyBin()
            },
            onDismiss = { showEmptyConfirm = false }
        )
    }

    pendingDelete?.let { item ->
        LedgerConfirm(
            title = "永久删除？",
            message = "将彻底删除这笔账，无法恢复。",
            confirmText = "删除",
            confirmIsDestructive = true,
            onConfirm = {
                viewModel.hardDelete(item.transaction.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun RecycleBinItemCard(
    item: RecycleBinItem,
    onRestore: () -> Unit,
    onHardDelete: () -> Unit
) {
    val tx = item.transaction
    val amountColor = when (tx.type) {
        TransactionType.INCOME -> AppSemanticColors.income
        TransactionType.EXPENSE -> AppSemanticColors.expense
        TransactionType.TRANSFER -> AppSemanticColors.transfer
    }
    val sign = when (tx.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }
    val title = when (tx.type) {
        TransactionType.TRANSFER -> "转账"
        else -> item.category?.name ?: "未分类"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.account?.name ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!tx.description.isNullOrBlank()) {
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "删除于 ${tx.deletedAt?.toDateTimeString() ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${tx.amount.toDisplayString()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontFamily = MoneyFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.daysUntilCleanup} 天后清理",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onHardDelete) {
                Icon(BhlIcons.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("永久删除", color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onRestore) {
                Icon(BhlIcons.Restore, contentDescription = null)
                Text("恢复")
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
