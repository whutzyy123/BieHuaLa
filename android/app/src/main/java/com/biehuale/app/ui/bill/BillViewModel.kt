package com.biehuale.app.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.dao.CategoryTotal
import com.biehuale.app.data.db.dao.DailyTotal
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.TransactionType
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
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * 账单 Tab - ViewModel
 *
 * 详见 docs/DEV_PLAN.md §6 Task 3.1-3.6
 */
@HiltViewModel
class BillViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<BillEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BillEvent> = _events.asSharedFlow()

    private val _filter = MutableStateFlow(BillFilter())
    val filter: StateFlow<BillFilter> = _filter.asStateFlow()

    val uiState: StateFlow<BillUiState> = combine(
        transactionRepository.observeAllActive(),
        categoryRepository.observeAll(), // 含归档：列表/饼图能解析名称
        accountRepository.observeActive(),
        _filter
    ) { transactions, categories, accounts, filter ->
        BillAggregator.buildState(transactions, categories, accounts, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillUiState(isLoading = true)
    )

    fun onMonthChange(year: Int, month: Int) {
        _filter.update { it.copy(year = year, month = month) }
    }

    fun onShiftMonth(delta: Int) {
        _filter.update {
            val cal = Calendar.getInstance().apply { set(it.year, it.month - 1, 1) }
            cal.add(Calendar.MONTH, delta)
            it.copy(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                customRangeStart = null,
                customRangeEnd = null
            )
        }
    }

    fun onCustomRange(start: Long?, end: Long?) {
        if (start == null || end == null) {
            _filter.update { it.copy(customRangeStart = null, customRangeEnd = null) }
            return
        }
        val startDay = BillAggregator.utcPickerMillisToLocalDayStart(start)
        val endDay = BillAggregator.utcPickerMillisToLocalDayStart(end)
        _filter.update {
            it.copy(customRangeStart = startDay, customRangeEnd = endDay)
        }
    }

    fun onClearCustomRange() {
        _filter.update { it.copy(customRangeStart = null, customRangeEnd = null) }
    }

    fun onOpenSearch() {
        _filter.update { it.copy(isSearchOpen = true) }
    }

    fun onCloseSearch() {
        _filter.update { it.copy(isSearchOpen = false, keyword = null) }
    }

    fun onKeywordChange(keyword: String) {
        _filter.update { it.copy(keyword = keyword) }
    }

    fun onTrendGranularityChange(granularity: TrendGranularity) {
        _filter.update { it.copy(trendGranularity = granularity) }
    }

    fun onAccountToggle(id: Long) {
        _filter.update { f ->
            val newSet = f.accountIds.toMutableSet().apply {
                if (!add(id)) remove(id)
            }
            f.copy(accountIds = newSet)
        }
    }

    fun onCategoryToggle(id: Long) {
        _filter.update { f ->
            val newSet = f.categoryIds.toMutableSet().apply {
                if (!add(id)) remove(id)
            }
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

    fun softDelete(id: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.softDelete(id)
            } catch (e: Exception) {
                _events.emit(BillEvent.Error("删除失败：${e.message ?: "未知错误"}"))
            }
        }
    }
}

sealed interface BillEvent {
    data class Error(val message: String) : BillEvent
}

enum class TrendGranularity {
    DAY, WEEK, MONTH
}

data class BillFilter(
    val year: Int = currentYear(),
    val month: Int = currentMonth1Indexed(),
    val customRangeStart: Long? = null,
    val customRangeEnd: Long? = null,
    val accountIds: Set<Long> = emptySet(),
    val categoryIds: Set<Long> = emptySet(),
    val types: Set<TransactionType> = emptySet(),
    val keyword: String? = null,
    val isSearchOpen: Boolean = false,
    val trendGranularity: TrendGranularity = TrendGranularity.DAY
) {
    val isCustomRange: Boolean get() = customRangeStart != null && customRangeEnd != null
    val isFiltering: Boolean
        get() = accountIds.isNotEmpty() || categoryIds.isNotEmpty() || types.isNotEmpty()

    /** 半开区间 [start, endExclusive) */
    fun currentRange(): Pair<Long, Long> {
        if (customRangeStart != null && customRangeEnd != null) {
            val endExclusive = Calendar.getInstance().apply {
                timeInMillis = customRangeEnd
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
            return customRangeStart to endExclusive
        }
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }
}

private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
private fun currentMonth1Indexed(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1

data class MonthlySummary(
    val expenseCents: Long = 0L,
    val incomeCents: Long = 0L,
    val transferCents: Long = 0L
) {
    val netCents: Long get() = incomeCents - expenseCents

    /** 支出/收入；收入为 0 时 null（UI 隐藏比例条） */
    val expenseIncomeRatioOrNull: Float?
        get() = if (incomeCents <= 0L) null else expenseCents.toFloat() / incomeCents.toFloat()
}

data class BillUiState(
    val filter: BillFilter = BillFilter(),
    val visibleTransactions: List<TransactionEntity> = emptyList(),
    val summary: MonthlySummary = MonthlySummary(),
    val pieData: List<CategoryTotal> = emptyList(),
    val lineData: List<DailyTotal> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true
) {
    fun categoryOf(id: Long?): CategoryEntity? =
        if (id == null) null else categories.firstOrNull { it.id == id }

    fun accountOf(id: Long?): AccountEntity? =
        if (id == null) null else accounts.firstOrNull { it.id == id }
}

/**
 * 可单测的纯聚合逻辑（内存过滤 + 汇总）
 */
object BillAggregator {

    fun utcPickerMillisToLocalDayStart(utcDayMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcDayMillis
        }
        return Calendar.getInstance().apply {
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun buildState(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        filter: BillFilter
    ): BillUiState {
        val (start, endExclusive) = filter.currentRange()

        fun inRange(t: TransactionEntity): Boolean =
            t.occurredAt >= start && t.occurredAt < endExclusive

        fun matchAccount(t: TransactionEntity): Boolean =
            filter.accountIds.isEmpty() ||
                t.accountId in filter.accountIds ||
                t.toAccountId in filter.accountIds

        fun matchCategory(t: TransactionEntity): Boolean =
            filter.categoryIds.isEmpty() || t.categoryId in filter.categoryIds

        fun matchType(t: TransactionEntity): Boolean =
            filter.types.isEmpty() || t.type in filter.types

        // 统计：时间 + 账户/类别/类型（不含搜索词）
        val statsVisible = transactions.filter { t ->
            inRange(t) && matchAccount(t) && matchCategory(t) && matchType(t)
        }

        // 列表：统计条件 + 说明搜索
        val keyword = filter.keyword
        val listVisible = if (keyword.isNullOrBlank()) {
            statsVisible
        } else {
            statsVisible.filter { t ->
                t.description?.contains(keyword, ignoreCase = true) == true
            }
        }

        val expense = statsVisible.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val income = statsVisible.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val transfer = statsVisible.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount }

        val pieData = statsVisible
            .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .map { (id, list) -> CategoryTotal(categoryId = id, totalCents = list.sumOf { it.amount }) }
            .sortedByDescending { it.totalCents }

        val expenses = statsVisible.filter { it.type == TransactionType.EXPENSE }
        val lineData = buildLineSeries(expenses, start, endExclusive, filter.trendGranularity)

        return BillUiState(
            filter = filter,
            visibleTransactions = listVisible,
            summary = MonthlySummary(
                expenseCents = expense,
                incomeCents = income,
                transferCents = transfer
            ),
            pieData = pieData,
            lineData = lineData,
            categories = categories,
            accounts = accounts,
            isLoading = false
        )
    }

    fun buildLineSeries(
        expenses: List<TransactionEntity>,
        start: Long,
        endExclusive: Long,
        granularity: TrendGranularity
    ): List<DailyTotal> {
        return when (granularity) {
            TrendGranularity.DAY -> buildBuckets(expenses, start, endExclusive, Calendar.DAY_OF_MONTH, 1)
            TrendGranularity.WEEK -> buildWeekBuckets(expenses, start, endExclusive)
            TrendGranularity.MONTH -> buildBuckets(expenses, start, endExclusive, Calendar.MONTH, 1)
        }
    }

    private fun startOfLocalDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfWeek(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfLocalDay(millis)
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return cal.timeInMillis
    }

    private fun startOfMonth(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun bucketKey(millis: Long, granularity: TrendGranularity): Long = when (granularity) {
        TrendGranularity.DAY -> startOfLocalDay(millis)
        TrendGranularity.WEEK -> startOfWeek(millis)
        TrendGranularity.MONTH -> startOfMonth(millis)
    }

    private fun buildBuckets(
        expenses: List<TransactionEntity>,
        start: Long,
        endExclusive: Long,
        field: Int,
        amount: Int
    ): List<DailyTotal> {
        val granularity = if (field == Calendar.MONTH) TrendGranularity.MONTH else TrendGranularity.DAY
        val byBucket = expenses.groupBy { bucketKey(it.occurredAt, granularity) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val out = mutableListOf<DailyTotal>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (field == Calendar.MONTH) startOfMonth(start) else startOfLocalDay(start)
        }
        while (cal.timeInMillis < endExclusive) {
            val key = cal.timeInMillis
            out.add(DailyTotal(dayStart = key, totalCents = byBucket[key] ?: 0L))
            cal.add(field, amount)
        }
        return out
    }

    private fun buildWeekBuckets(
        expenses: List<TransactionEntity>,
        start: Long,
        endExclusive: Long
    ): List<DailyTotal> {
        val byBucket = expenses.groupBy { startOfWeek(it.occurredAt) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val out = mutableListOf<DailyTotal>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfWeek(start)
            firstDayOfWeek = Calendar.MONDAY
        }
        while (cal.timeInMillis < endExclusive) {
            val key = cal.timeInMillis
            out.add(DailyTotal(dayStart = key, totalCents = byBucket[key] ?: 0L))
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return out
    }
}
