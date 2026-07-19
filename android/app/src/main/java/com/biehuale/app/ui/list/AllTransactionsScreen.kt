package com.biehuale.app.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.ui.bill.components.FilterBottomSheet
import com.biehuale.app.ui.bill.components.SearchBar
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.TransactionActionSheet
import com.biehuale.app.ui.common.TransactionRow
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.ScreenTitleStyle
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 全部流水页（从账单 Tab「查看全部」/ 饼图类别进入）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    initialCategoryId: Long? = null,
    onBack: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AllTransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var actionForTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null && initialCategoryId > 0) {
            viewModel.applyInitialCategory(initialCategoryId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AllTransactionsEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("全部流水", style = ScreenTitleStyle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopToolbar(
                isSearchOpen = uiState.filter.isSearchOpen,
                keyword = uiState.filter.keyword.orEmpty(),
                hasFilter = uiState.filter.isFiltering,
                onOpenSearch = viewModel::onOpenSearch,
                onCloseSearch = viewModel::onCloseSearch,
                onKeywordChange = viewModel::onKeywordChange,
                onFilterClick = { coroutineScope.launch { showFilterSheet = true } }
            )

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("加载中…") }

                uiState.visibleTransactions.isEmpty() -> EmptyState(
                    title = if (uiState.filter.isFiltering || !uiState.filter.keyword.isNullOrBlank()) {
                        "没有匹配的流水"
                    } else {
                        "还没有记账"
                    },
                    subtitle = if (uiState.filter.isFiltering || !uiState.filter.keyword.isNullOrBlank()) {
                        "试着调整筛选或搜索"
                    } else {
                        "去记账 Tab 记一笔吧"
                    },
                    modifier = Modifier.fillMaxSize()
                )

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = AppSpacing.md,
                            vertical = AppSpacing.sm
                        )
                    ) {
                        if (uiState.filter.isFiltering) {
                            item(key = "filter_chip") {
                                TextButton(onClick = viewModel::onClearFilters) {
                                    val count = uiState.filter.accountIds.size +
                                        uiState.filter.categoryIds.size +
                                        uiState.filter.types.size
                                    Text("清除筛选（$count）")
                                }
                            }
                        }

                        val groups = uiState.visibleTransactions.groupBy { monthKey(it.occurredAt) }
                            .toList()
                            .sortedByDescending { it.first }
                        groups.forEach { (month, list) ->
                            item(key = "header_$month") {
                                Text(
                                    text = formatMonthLabel(month),
                                    style = MaterialTheme.typography.labelLarge,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        top = AppSpacing.sm + AppSpacing.xs,
                                        bottom = AppSpacing.xs
                                    )
                                )
                            }
                            items(list, key = { it.id }) { transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    category = uiState.categoryOf(transaction.categoryId),
                                    account = uiState.accountOf(transaction.accountId),
                                    toAccount = transaction.toAccountId?.let { uiState.accountOf(it) },
                                    onClick = { onItemClick(transaction.id) },
                                    onLongClick = { actionForTx = transaction }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = filterSheetState,
            onDismiss = {
                coroutineScope.launch { filterSheetState.hide() }
                    .invokeOnCompletion { showFilterSheet = false }
            },
            selectedTypes = uiState.filter.types,
            selectedAccountIds = uiState.filter.accountIds,
            selectedCategoryIds = uiState.filter.categoryIds,
            accounts = uiState.accounts,
            categories = uiState.categories.filter { !it.isArchived },
            onApply = viewModel::onApplyFilters,
            onClearApplied = viewModel::onClearFilters
        )
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
private fun TopToolbar(
    isSearchOpen: Boolean,
    keyword: String,
    hasFilter: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    if (isSearchOpen) {
        SearchBar(
            keyword = keyword,
            onKeywordChange = onKeywordChange,
            onClose = onCloseSearch
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "搜索")
            }
            Box {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "筛选",
                        tint = if (hasFilter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (hasFilter) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

private fun monthKey(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun formatMonthLabel(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.YEAR)} 年 ${cal.get(Calendar.MONTH) + 1} 月"
}
