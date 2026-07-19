package com.biehuale.app.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.bill.components.FilterBottomSheet
import com.biehuale.app.ui.bill.components.SearchBar
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.DateExt.toDateString
import com.biehuale.app.util.Money.toDisplayString
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 全部流水页（从账单 Tab「查看全部」进入）
 *
 * 详见 docs/PRD.md §4.1
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllTransactionsScreen(
    onBack: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AllTransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AllTransactionsEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("全部流水") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.filter.isFiltering) {
                            item(key = "filter_chip") {
                                FilterActiveChip(
                                    count = (
                                        uiState.filter.accountIds.size +
                                            uiState.filter.categoryIds.size +
                                            uiState.filter.types.size
                                        ),
                                    onClear = viewModel::onClearFilters
                                )
                            }
                        }

                        val groups = uiState.visibleTransactions.groupBy { monthKey(it.occurredAt) }
                            .toList()
                            .sortedByDescending { it.first }
                        groups.forEach { (month, list) ->
                            item(key = "header_$month") {
                                Text(
                                    text = formatMonthLabel(month),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(list, key = { it.id }) { transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    category = uiState.categoryOf(transaction.categoryId),
                                    account = uiState.accountOf(transaction.accountId),
                                    toAccount = transaction.toAccountId?.let { uiState.accountOf(it) },
                                    onClick = { onItemClick(transaction.id) },
                                    onLongClick = { pendingDelete = transaction }
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
            onApply = { types, accountIds, categoryIds ->
                viewModel.onApplyFilters(types, accountIds, categoryIds)
                coroutineScope.launch { filterSheetState.hide() }
                    .invokeOnCompletion { showFilterSheet = false }
            },
            onClearApplied = viewModel::onClearFilters
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
                }) { Text("删除") }
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
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "搜索")
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = if (hasFilter) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                    contentDescription = "筛选",
                    tint = if (hasFilter) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FilterActiveChip(
    count: Int,
    onClear: () -> Unit
) {
    AssistChip(
        onClick = onClear,
        label = { Text("已应用 $count 项筛选") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    toAccount: AccountEntity?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> AppSemanticColors.income
        TransactionType.EXPENSE -> AppSemanticColors.expense
        TransactionType.TRANSFER -> AppSemanticColors.transfer
    }
    val sign = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }
    val title = when (transaction.type) {
        TransactionType.TRANSFER -> "${account?.name ?: "?"} → ${toAccount?.name ?: "?"}"
        else -> {
            val name = category?.name ?: "未分类"
            if (category?.isArchived == true) "$name（已归档）" else name
        }
    }
    val accountLabel = account?.let {
        if (it.isArchived) "${it.name}（已归档）" else it.name
    } ?: "未指定账户"
    val subtitle = buildString {
        append(accountLabel)
        if (!transaction.description.isNullOrBlank()) {
            append(" · ")
            append(transaction.description)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$subtitle · ${transaction.occurredAt.toDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$sign${transaction.amount.toDisplayString()}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontFamily = MoneyFontFamily,
                fontWeight = FontWeight.SemiBold
            )
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
