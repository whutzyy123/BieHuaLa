package com.biehuale.app.ui.bill

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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.bill.components.CategoryPieChart
import com.biehuale.app.ui.bill.components.DailyLineChart
import com.biehuale.app.ui.bill.components.FilterBottomSheet
import com.biehuale.app.ui.bill.components.SearchBar
import com.biehuale.app.ui.bill.components.SummaryCard
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.BieHuaLeTheme
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.DateExt.toDateString
import com.biehuale.app.util.Money.toDisplayString
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillScreen(
    onItemClick: (Long) -> Unit = {},
    onGoToRecord: () -> Unit = {},
    onViewAll: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel()
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
                is BillEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                onFilterClick = { coroutineScope.launch { showFilterSheet = true } },
                onViewAll = onViewAll
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    content = { Text(text = "\u52a0\u8f7d\u4e2d\u2026") }
                )
            } else if (
                uiState.visibleTransactions.isEmpty() &&
                !uiState.filter.isFiltering &&
                uiState.filter.keyword.isNullOrBlank() &&
                uiState.summary.expenseCents == 0L &&
                uiState.summary.incomeCents == 0L
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SummaryCardBlock(uiState, viewModel)
                    EmptyState(
                        title = "\u8fd8\u6ca1\u6709\u8bb0\u8d26",
                        subtitle = "\u70b9\u4e0b\u65b9\u6309\u94ae\u5f00\u59cb\u7b2c\u4e00\u7b14",
                        actionText = "\u53bb\u8bb0\u8d26",
                        onAction = onGoToRecord,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "summary") { SummaryCardBlock(uiState, viewModel) }
                    item(key = "pie") {
                        if (uiState.pieData.isNotEmpty()) {
                            CategoryPieChart(
                                data = uiState.pieData,
                                categories = uiState.categories,
                                selectedCategoryId = uiState.filter.categoryIds.singleOrNull(),
                                onCategoryToggle = viewModel::onCategoryToggle
                            )
                        }
                    }
                    item(key = "line") {
                        if (uiState.lineData.isNotEmpty()) {
                            DailyLineChart(
                                data = uiState.lineData,
                                granularity = uiState.filter.trendGranularity,
                                onGranularityChange = viewModel::onTrendGranularityChange
                            )
                        }
                    }
                    if (uiState.filter.isFiltering) {
                        item(key = "filter_chip") {
                            val count = uiState.filter.accountIds.size +
                                uiState.filter.categoryIds.size +
                                uiState.filter.types.size
                            AssistChip(
                                onClick = viewModel::onClearFilters,
                                label = { Text("\u5df2\u5e94\u7528 $count \u9879\u7b5b\u9009") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                    if (uiState.visibleTransactions.isEmpty()) {
                        item(key = "empty_filtered") {
                            Text(
                                text = "\u5f53\u524d\u6761\u4ef6\u4e0b\u6ca1\u6709\u8bb0\u5f55",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        val groups = uiState.visibleTransactions
                            .groupBy { dayKey(it.occurredAt) }
                            .toList()
                            .sortedByDescending { it.first }
                        groups.forEach { (day, list) ->
                            item(key = "header_$day") {
                                Text(
                                    text = day.toDateString(),
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

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("\u5220\u9664\u8fd9\u7b14\u8d26\uff1f") },
            text = { Text("\u5df2\u79fb\u51fa\u5217\u8868\uff08\u8f6f\u5220\u9664\uff09\u3002\u53ef\u5728\u8bbe\u7f6e \u2192 \u56de\u6536\u7ad9\u6062\u590d\u3002") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.softDelete(tx.id)
                    pendingDelete = null
                }) { Text("\u5220\u9664") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("\u53d6\u6d88") }
            }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = filterSheetState,
            onDismiss = {
                coroutineScope.launch {
                    filterSheetState.hide()
                    showFilterSheet = false
                }
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

@Composable
private fun TopToolbar(
    isSearchOpen: Boolean,
    keyword: String,
    hasFilter: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onViewAll: () -> Unit
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
            TextButton(onClick = onViewAll) {
                Text("\u67e5\u770b\u5168\u90e8")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "\u641c\u7d22")
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = if (hasFilter) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                    contentDescription = "\u7b5b\u9009",
                    tint = if (hasFilter) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
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
        TransactionType.TRANSFER ->
            "${account?.name ?: "?"} \u2192 ${toAccount?.name ?: "?"}"
        else -> {
            val name = category?.name ?: "\u672a\u5206\u7c7b"
            if (category?.isArchived == true) "$name\uff08\u5df2\u5f52\u6863\uff09" else name
        }
    }
    val accountLabel = account?.let {
        if (it.isArchived) "${it.name}\uff08\u5df2\u5f52\u6863\uff09" else it.name
    } ?: "\u672a\u6307\u5b9a\u8d26\u6237"
    val subtitle = buildString {
        append(accountLabel)
        if (!transaction.description.isNullOrBlank()) {
            append(" \u00b7 ")
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
                    text = "$subtitle \u00b7 ${transaction.occurredAt.toDateString()}",
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

private fun dayKey(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

@Preview(showBackground = true)
@Composable
private fun BillScreenPreview() {
    BieHuaLeTheme {
        Text("BillScreen Preview")
    }
}
