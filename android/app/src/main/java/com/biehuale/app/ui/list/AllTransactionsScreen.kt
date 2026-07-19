package com.biehuale.app.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.FilterBottomSheet
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.ListSectionHeader
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.SearchBar
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.SubScreenScaffold
import com.biehuale.app.ui.common.TransactionActionSheet
import com.biehuale.app.ui.common.TransactionRow
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppMotion
import com.biehuale.app.ui.theme.AppSpacing
import kotlinx.coroutines.launch

/**
 * 全部流水页（从账单 Tab「查看全部」/ 饼图类别进入）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllTransactionsScreen(
    initialCategoryId: Long? = null,
    initialRangeStart: Long? = null,
    initialRangeEndExclusive: Long? = null,
    onBack: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AllTransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keywordInput by viewModel.keywordInput.collectAsStateWithLifecycle()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var actionForTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(initialCategoryId, initialRangeStart, initialRangeEndExclusive) {
        if (initialCategoryId != null && initialCategoryId > 0) {
            viewModel.applyInitialCategory(initialCategoryId)
        }
        if (initialRangeStart != null && initialRangeEndExclusive != null) {
            viewModel.applyInitialRange(initialRangeStart, initialRangeEndExclusive)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AllTransactionsEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SubScreenScaffold(
        title = "全部流水",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopToolbar(
                isSearchOpen = uiState.filter.isSearchOpen,
                keyword = keywordInput,
                hasFilter = uiState.filter.isFiltering,
                onOpenSearch = viewModel::onOpenSearch,
                onCloseSearch = viewModel::onCloseSearch,
                onKeywordChange = viewModel::onKeywordChange,
                onFilterClick = { coroutineScope.launch { showFilterSheet = true } }
            )

            when {
                uiState.isLoading -> LoadingState()

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = AppSpacing.sm)
                    ) {
                        if (uiState.filter.isFiltering) {
                            TextButton(
                                onClick = viewModel::onClearFilters,
                                modifier = Modifier.padding(horizontal = AppSpacing.md)
                            ) {
                                val count = uiState.filter.accountIds.size +
                                    uiState.filter.categoryIds.size +
                                    uiState.filter.types.size
                                Text("清除筛选（$count）")
                            }
                        }
                        SectionPanel(
                            contentPadding = 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = AppSpacing.xs)
                            ) {
                                uiState.monthSections.forEachIndexed { index, section ->
                                    if (index > 0) {
                                        item(key = "gap_${section.monthKey}") {
                                            Spacer(modifier = Modifier.height(AppSpacing.md))
                                        }
                                    }
                                    item(key = "header_${section.monthKey}") {
                                        ListSectionHeader(
                                            title = section.label,
                                            horizontalPadding = AppSpacing.md,
                                            verticalPadding = AppSpacing.md
                                        )
                                    }
                                    items(section.transactions, key = { it.id }) { transaction ->
                                        val id = transaction.id
                                        TransactionRow(
                                            transaction = transaction,
                                            category = uiState.categoryOf(transaction.categoryId),
                                            account = uiState.accountOf(transaction.accountId),
                                            toAccount = transaction.toAccountId?.let {
                                                uiState.accountOf(it)
                                            },
                                            onClick = { onItemClick(id) },
                                            onLongClick = { actionForTx = transaction },
                                            rowModifier = Modifier.animateItem(
                                                fadeInSpec = null,
                                                fadeOutSpec = AppMotion.list(),
                                                placementSpec = null
                                            )
                                        )
                                    }
                                }
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
            accounts = uiState.accounts.filter { !it.isArchived },
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
                Icon(BhlIcons.Search, contentDescription = "搜索")
            }
            Box {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = BhlIcons.Filter,
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
