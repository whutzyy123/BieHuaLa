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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 全部流水页 - ViewModel
 *
 * 详见 docs/PRD.md §4.1
 */
@OptIn(FlowPreview::class)
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

    /** 搜索框即时文案；列表过滤用 debounce 后的关键字 */
    private val _keywordInput = MutableStateFlow("")
    val keywordInput: StateFlow<String> = _keywordInput.asStateFlow()

    private val debouncedKeyword = _keywordInput
        .debounce(150)
        .distinctUntilChanged()

    val uiState: StateFlow<AllTransactionsUiState> = combine(
        transactionRepository.observeAllActive(),
        categoryRepository.observeAll(),
        accountRepository.observeAll(),
        _filter,
        debouncedKeyword
    ) { transactions, categories, accounts, filter, keyword ->
        val filtered = transactions.filter { tx ->
            matchesBoundRange(tx, filter) &&
                (filter.accountIds.isEmpty() ||
                    tx.accountId in filter.accountIds ||
                    tx.toAccountId in filter.accountIds) &&
                (filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds) &&
                (filter.types.isEmpty() || tx.type in filter.types) &&
                (keyword.isBlank() ||
                    tx.description?.contains(keyword, ignoreCase = true) == true)
        }
        val groups = filtered
            .groupBy { monthKey(it.occurredAt) }
            .toList()
            .sortedByDescending { it.first }
            .map { (month, list) ->
                MonthSection(
                    monthKey = month,
                    label = formatMonthLabel(month),
                    transactions = list
                )
            }
        AllTransactionsUiState(
            filter = filter.copy(keyword = keyword.ifBlank { null }),
            monthSections = groups,
            categories = categories,
            accounts = accounts,
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AllTransactionsUiState(isLoading = true)
        )

    fun onOpenSearch() {
        _filter.update { it.copy(isSearchOpen = true) }
    }

    fun onCloseSearch() {
        _keywordInput.value = ""
        _filter.update { it.copy(isSearchOpen = false, keyword = null) }
    }

    fun onKeywordChange(keyword: String) {
        _keywordInput.value = keyword
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

    fun applyInitialCategory(categoryId: Long) {
        _filter.update {
            it.copy(categoryIds = setOf(categoryId))
        }
    }

    /** 账单「本月流水」/ 饼图深链：半开区间 [start, endExclusive) */
    fun applyInitialRange(rangeStart: Long, rangeEndExclusive: Long) {
        if (rangeStart < 0L || rangeEndExclusive < 0L || rangeEndExclusive <= rangeStart) return
        _filter.update {
            it.copy(
                boundRangeStart = rangeStart,
                boundRangeEndExclusive = rangeEndExclusive
            )
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

    companion object {
        internal fun matchesBoundRange(tx: TransactionEntity, filter: BillFilter): Boolean {
            val start = filter.boundRangeStart
            val end = filter.boundRangeEndExclusive
            if (start == null || end == null) return true
            return tx.occurredAt >= start && tx.occurredAt < end
        }

        internal fun monthKey(timestamp: Long): Long {
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

        internal fun formatMonthLabel(timestamp: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            return "${cal.get(Calendar.YEAR)} 年 ${cal.get(Calendar.MONTH) + 1} 月"
        }
    }
}

sealed interface AllTransactionsEvent {
    data class Error(val message: String) : AllTransactionsEvent
}

data class MonthSection(
    val monthKey: Long,
    val label: String,
    val transactions: List<TransactionEntity>
)

data class AllTransactionsUiState(
    val filter: BillFilter = BillFilter(),
    val monthSections: List<MonthSection> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true
) {
    val visibleTransactions: List<TransactionEntity>
        get() = monthSections.flatMap { it.transactions }

    fun categoryOf(id: Long?): CategoryEntity? =
        if (id == null) null else categories.firstOrNull { it.id == id }

    fun accountOf(id: Long?): AccountEntity? =
        if (id == null) null else accounts.firstOrNull { it.id == id }
}
