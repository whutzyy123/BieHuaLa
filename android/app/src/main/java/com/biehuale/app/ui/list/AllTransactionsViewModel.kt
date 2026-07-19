package com.biehuale.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.bill.BillFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 全部流水页 - ViewModel
 *
 * 详见 docs/PRD.md §4.1
 *
 * 与 BillViewModel 的区别：
 *  - 没有月份切换 / 自定义区间 / 比例条
 *  - 默认按全部时间显示，列表更长（不分组，每笔独立行）
 *  - 复用 BillFilter（搜索/筛选）
 */
@HiltViewModel
class AllTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<AllTransactionsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AllTransactionsEvent> = _events.asSharedFlow()

    private val _filter = MutableStateFlow(BillFilter())
    val filter: StateFlow<BillFilter> = _filter.asStateFlow()

    val uiState: StateFlow<AllTransactionsUiState> = combine(
        transactionRepository.observeAllActive(),
        categoryRepository.observeAll(), // 含归档：列表能解析名称
        accountRepository.observeActive(),
        _filter
    ) { transactions, categories, accounts, filter ->
        // 应用过滤（只列表维度，统计不算）
        val filtered = transactions.filter { tx ->
            (filter.accountIds.isEmpty() || tx.accountId in filter.accountIds || tx.toAccountId in filter.accountIds) &&
            (filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds) &&
            (filter.types.isEmpty() || tx.type in filter.types) &&
            (filter.keyword.isNullOrBlank() || tx.description?.contains(filter.keyword, ignoreCase = true) == true)
        }
        AllTransactionsUiState(
            filter = filter,
            visibleTransactions = filtered,
            categories = categories,
            accounts = accounts,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AllTransactionsUiState(isLoading = true)
    )

    fun onOpenSearch() {
        _filter.update { it.copy(isSearchOpen = true) }
    }

    fun onCloseSearch() {
        _filter.update { it.copy(isSearchOpen = false, keyword = null) }
    }

    fun onKeywordChange(keyword: String) {
        _filter.update { it.copy(keyword = keyword) }
    }

    fun onAccountToggle(id: Long) {
        _filter.update { f ->
            val newSet = f.accountIds.toMutableSet().apply { if (!add(id)) remove(id) }
            f.copy(accountIds = newSet)
        }
    }

    fun onCategoryToggle(id: Long) {
        _filter.update { f ->
            val newSet = f.categoryIds.toMutableSet().apply { if (!add(id)) remove(id) }
            f.copy(categoryIds = newSet)
        }
    }

    fun onTypeToggle(type: TransactionType) {
        _filter.update { f ->
            val newSet = f.types.toMutableSet().apply {
                if (!add(type)) remove(type)
            }
            f.copy(types = newSet)
        }
    }

    fun onApplyFilters(
        types: Set<TransactionType>,
        accountIds: Set<Long>,
        categoryIds: Set<Long>
    ) {
        _filter.update {
            it.copy(types = types, accountIds = accountIds, categoryIds = categoryIds)
        }
    }

    fun onClearFilters() {
        _filter.update {
            it.copy(
                accountIds = emptySet(),
                categoryIds = emptySet(),
                types = emptySet()
            )
        }
    }

    /** 从账单饼图等入口带入的初始类别筛选 */
    fun applyInitialCategory(categoryId: Long) {
        _filter.update {
            it.copy(categoryIds = setOf(categoryId))
        }
    }

    fun softDelete(id: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.softDelete(id)
            } catch (e: Exception) {
                _events.emit(AllTransactionsEvent.Error("删除失败：${e.message ?: "未知错误"}"))
            }
        }
    }
}

sealed interface AllTransactionsEvent {
    data class Error(val message: String) : AllTransactionsEvent
}

data class AllTransactionsUiState(
    val filter: BillFilter = BillFilter(),
    val visibleTransactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true
) {
    fun categoryOf(id: Long?): CategoryEntity? =
        if (id == null) null else categories.firstOrNull { it.id == id }

    fun accountOf(id: Long?): AccountEntity? =
        if (id == null) null else accounts.firstOrNull { it.id == id }
}
