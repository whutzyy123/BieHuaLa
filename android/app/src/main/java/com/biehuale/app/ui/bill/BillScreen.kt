package com.biehuale.app.ui.bill

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.ui.bill.components.CategoryPieChart
import com.biehuale.app.ui.bill.components.DailyLineChart
import com.biehuale.app.ui.bill.components.SummaryCard
import com.biehuale.app.ui.bill.components.TotalAssetsSheet
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.TransactionActionSheet
import com.biehuale.app.ui.common.TransactionRow
import com.biehuale.app.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    onItemClick: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onGoToRecord: () -> Unit = {},
    onViewMonthFlow: (rangeStart: Long, rangeEndExclusive: Long) -> Unit = { _, _ -> },
    onViewAll: () -> Unit = {},
    onCategoryFlow: (categoryId: Long, rangeStart: Long, rangeEndExclusive: Long) -> Unit =
        { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var actionForTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showTotalAssets by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BillEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val (rangeStart, rangeEndExclusive) = uiState.filter.currentRange()
    val monthFlowLabel = if (uiState.filter.isCustomRange) "区间流水" else "本月流水"

    // 外层 AppNav Scaffold 已处理 statusBar / 底栏 insets，此处勿再叠一层顶部空白
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else if (uiState.isTrulyEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                SectionPanel {
                    SummaryCardBlock(
                        uiState = uiState,
                        viewModel = viewModel,
                        onTotalAssetsClick = { showTotalAssets = true }
                    )
                }
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
                contentPadding = PaddingValues(
                    top = AppSpacing.sm,
                    bottom = AppSpacing.xl
                )
            ) {
                item(key = "hero") {
                    SectionPanel {
                        SummaryCardBlock(
                            uiState = uiState,
                            viewModel = viewModel,
                            onTotalAssetsClick = { showTotalAssets = true }
                        )
                        TextButton(
                            onClick = { onViewMonthFlow(rangeStart, rangeEndExclusive) },
                            modifier = Modifier.padding(top = AppSpacing.xs)
                        ) {
                            Text(
                                text = monthFlowLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item(key = "gap_after_hero") {
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }

                if (uiState.isRangeEmpty) {
                    item(key = "range_empty") {
                        EmptyState(
                            title = if (uiState.filter.isCustomRange) {
                                "该区间暂无记录"
                            } else {
                                "本月暂无记录"
                            },
                            subtitle = "可左右切月，或记一笔新账",
                            actionText = "去记账",
                            onAction = onGoToRecord,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.lg)
                        )
                    }
                } else {
                    if (uiState.pieData.isNotEmpty()) {
                        item(key = "pie") {
                            SectionPanel(title = "花在哪") {
                                CategoryPieChart(
                                    data = uiState.pieData,
                                    categories = uiState.categories,
                                    selectedCategoryId = null,
                                    onCategoryClick = { categoryId ->
                                        onCategoryFlow(categoryId, rangeStart, rangeEndExclusive)
                                    }
                                )
                            }
                        }
                        item(key = "gap_after_pie") {
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                        }
                    }

                    if (uiState.lineData.isNotEmpty()) {
                        item(key = "line") {
                            SectionPanel(title = "趋势") {
                                DailyLineChart(
                                    data = uiState.lineData,
                                    granularity = uiState.filter.trendGranularity,
                                    onGranularityChange = viewModel::onTrendGranularityChange,
                                    showMonthGranularity = uiState.filter.isCustomRange
                                )
                            }
                        }
                        item(key = "gap_after_line") {
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                        }
                    }

                    item(key = "recent") {
                        val recent = uiState.visibleTransactions.take(5)
                        SectionPanel(title = "最近") {
                            if (recent.isEmpty()) {
                                Text(
                                    text = "当前条件下没有记录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = AppSpacing.sm)
                                )
                            } else {
                                recent.forEach { transaction ->
                                    TransactionRow(
                                        transaction = transaction,
                                        category = uiState.categoryOf(transaction.categoryId),
                                        account = uiState.accountOf(transaction.accountId),
                                        toAccount = transaction.toAccountId?.let {
                                            uiState.accountOf(it)
                                        },
                                        onClick = { onItemClick(transaction.id) },
                                        onLongClick = { actionForTx = transaction }
                                    )
                                }
                            }
                            TextButton(
                                onClick = onViewAll,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppSpacing.xs)
                            ) {
                                Text("查看全部")
                            }
                        }
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
        LedgerConfirm(
            title = "删除这笔账？",
            message = "已移出列表（软删除）。可在设置 → 回收站恢复。",
            confirmText = "删除",
            confirmIsDestructive = true,
            onConfirm = {
                viewModel.softDelete(tx.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showTotalAssets) {
        TotalAssetsSheet(
            totalAssetsCents = uiState.totalAssetsCents,
            breakdown = uiState.assetBreakdown,
            onDismiss = { showTotalAssets = false }
        )
    }
}

@Composable
private fun SummaryCardBlock(
    uiState: BillUiState,
    viewModel: BillViewModel,
    onTotalAssetsClick: () -> Unit
) {
    SummaryCard(
        summary = uiState.summary,
        year = uiState.filter.year,
        month = uiState.filter.month,
        onShiftMonth = viewModel::onShiftMonth,
        onPickCustomRange = { start, end -> viewModel.onCustomRange(start, end) },
        onClearCustomRange = viewModel::onClearCustomRange,
        isCustomRange = uiState.filter.isCustomRange,
        customRangeStart = uiState.filter.customRangeStart,
        customRangeEnd = uiState.filter.customRangeEnd,
        canShiftForward = uiState.filter.isCustomRange || !uiState.filter.isAtOrAfterCurrentMonth(),
        totalAssetsCents = uiState.totalAssetsCents,
        onTotalAssetsClick = onTotalAssetsClick
    )
}
