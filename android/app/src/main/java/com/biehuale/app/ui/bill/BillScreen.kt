package com.biehuale.app.ui.bill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.ui.bill.components.CategoryPieChart
import com.biehuale.app.ui.bill.components.DailyLineChart
import com.biehuale.app.ui.bill.components.SummaryCard
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.TransactionActionSheet
import com.biehuale.app.ui.common.TransactionRow
import com.biehuale.app.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    onItemClick: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onGoToRecord: () -> Unit = {},
    onViewAll: () -> Unit = {},
    onCategoryFlow: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var actionForTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BillEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val isTrulyEmpty = uiState.visibleTransactions.isEmpty() &&
        !uiState.filter.isFiltering &&
        uiState.filter.keyword.isNullOrBlank() &&
        uiState.summary.expenseCents == 0L &&
        uiState.summary.incomeCents == 0L

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "加载中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isTrulyEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SummaryCardBlock(uiState, viewModel)
                EmptyState(
                    title = "还没有记账",
                    subtitle = "点下方按钮开始第一笔",
                    actionText = "去记账",
                    onAction = onGoToRecord,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = AppSpacing.xl)
            ) {
                item(key = "hero") {
                    SummaryCardBlock(uiState, viewModel)
                }

                item(key = "month_flow_link") {
                    TextButton(
                        onClick = onViewAll,
                        modifier = Modifier.padding(horizontal = AppSpacing.sm)
                    ) {
                        Text(
                            text = "本月流水",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item(key = "after_hero") {
                    Box(modifier = Modifier.height(AppSpacing.sm))
                }

                if (uiState.pieData.isNotEmpty()) {
                    item(key = "pie") {
                        CategoryPieChart(
                            data = uiState.pieData,
                            categories = uiState.categories,
                            selectedCategoryId = null,
                            onCategoryClick = onCategoryFlow
                        )
                    }
                }

                if (uiState.lineData.isNotEmpty()) {
                    item(key = "line") {
                        DailyLineChart(
                            data = uiState.lineData,
                            granularity = uiState.filter.trendGranularity,
                            onGranularityChange = viewModel::onTrendGranularityChange
                        )
                    }
                }

                item(key = "recent_header") {
                    Text(
                        text = "最近",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.md,
                            vertical = AppSpacing.sm
                        )
                    )
                }

                val recent = uiState.visibleTransactions.take(5)
                if (recent.isEmpty()) {
                    item(key = "empty_filtered") {
                        Text(
                            text = "当前条件下没有记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = AppSpacing.md,
                                vertical = AppSpacing.lg
                            )
                        )
                    }
                } else {
                    items(recent, key = { it.id }) { transaction ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(150)) + expandVertically(tween(180)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                        ) {
                            TransactionRow(
                                transaction = transaction,
                                category = uiState.categoryOf(transaction.categoryId),
                                account = uiState.accountOf(transaction.accountId),
                                toAccount = transaction.toAccountId?.let { uiState.accountOf(it) },
                                onClick = { onItemClick(transaction.id) },
                                onLongClick = { actionForTx = transaction },
                                rowModifier = Modifier.padding(horizontal = AppSpacing.md)
                            )
                        }
                    }
                }

                item(key = "view_all") {
                    TextButton(
                        onClick = onViewAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.sm)
                    ) {
                        Text("查看全部")
                    }
                }
            }
        }
    }

    actionForTx?.let { tx ->
        TransactionActionSheet(
            onEdit = { onEdit(tx.id) },
            onDelete = { pendingDelete = tx },
            onDismiss = { actionForTx = null }
        )
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这笔账？") },
            text = { Text("已移出列表（软删除）。可在设置 → 回收站恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDelete(tx.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryCardBlock(uiState: BillUiState, viewModel: BillViewModel) {
    SummaryCard(
        summary = uiState.summary,
        year = uiState.filter.year,
        month = uiState.filter.month,
        onShiftMonth = viewModel::onShiftMonth,
        onPickCustomRange = { start, end -> viewModel.onCustomRange(start, end) },
        onClearCustomRange = viewModel::onClearCustomRange,
        isCustomRange = uiState.filter.isCustomRange,
        customRangeStart = uiState.filter.customRangeStart,
        customRangeEnd = uiState.filter.customRangeEnd
    )
}
