package com.biehuale.app.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.dao.AccountBalanceRow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
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
        accountRepository.observeAll(), // 含归档：最近列表能解析账户名
        accountRepository.observeActiveBalances(), // 总资产（时点，与月份无关）
        _filter
    ) { transactions, categories, accounts, balanceRows, filter ->
        BillAggregator.buildState(
            transactions = transactions,
            categories = categories,
            accounts = accounts,
            balanceRows = balanceRows,
            filter = filter
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChangedBy { it.contentSignature() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BillUiState(isLoading = true)
        )

    fun onMonthChange(year: Int, month: Int) {
        _filter.update { it.copy(year = year, month = month) }
    }

    fun onShiftMonth(delta: Int) {
        _filter.update { prev ->
            if (delta > 0 && !prev.isCustomRange && prev.isAtOrAfterCurrentMonth()) {
                return@update prev
            }
            val cal = Calendar.getInstance().apply { set(prev.year, prev.month - 1, 1) }
            cal.add(Calendar.MONTH, delta)
            val nextYear = cal.get(Calendar.YEAR)
            val nextMonth = cal.get(Calendar.MONTH) + 1
            val nextGranularity =
                if (!prev.isCustomRange && prev.trendGranularity == TrendGranularity.MONTH) {
                    TrendGranularity.DAY
                } else {
                    prev.trendGranularity
                }
            prev.copy(
                year = nextYear,
                month = nextMonth,
                customRangeStart = null,
                customRangeEnd = null,
                trendGranularity = nextGranularity
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

    fun onTrendGranularityChange(granularity: TrendGranularity) {
        _filter.update { prev ->
            val resolved =
                if (!prev.isCustomRange && granularity == TrendGranularity.MONTH) {
                    TrendGranularity.DAY
                } else {
                    granularity
                }
            prev.copy(trendGranularity = resolved)
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
    val trendGranularity: TrendGranularity = TrendGranularity.DAY,
    /** 全部流水深链：半开区间；null 表示不限日期 */
    val boundRangeStart: Long? = null,
    val boundRangeEndExclusive: Long? = null
) {
    val isCustomRange: Boolean get() = customRangeStart != null && customRangeEnd != null
    val isFiltering: Boolean
        get() = accountIds.isNotEmpty() || categoryIds.isNotEmpty() || types.isNotEmpty()

    fun isAtOrAfterCurrentMonth(): Boolean {
        val nowY = currentYear()
        val nowM = currentMonth1Indexed()
        return year > nowY || (year == nowY && month >= nowM)
    }

    /** 半开区间 [start, endExclusive) —— 账单 Hero / 饼图统计用 */
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

data class AccountAssetLine(
    val accountId: Long,
    val name: String,
    val balanceCents: Long
)

data class BillUiState(
    val filter: BillFilter = BillFilter(),
    val visibleTransactions: List<TransactionEntity> = emptyList(),
    val summary: MonthlySummary = MonthlySummary(),
    val pieData: List<CategoryTotal> = emptyList(),
    val lineData: List<DailyTotal> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    /** 活跃账户余额合计（时点净值，不受账单月份/区间影响） */
    val totalAssetsCents: Long = 0L,
    val assetBreakdown: List<AccountAssetLine> = emptyList(),
    /** 全库是否有任意活跃流水（与当前区间无关） */
    val hasAnyActiveTransaction: Boolean = false,
    val isLoading: Boolean = true
) {
    fun categoryOf(id: Long?): CategoryEntity? =
        if (id == null) null else categories.firstOrNull { it.id == id }

    fun accountOf(id: Long?): AccountEntity? =
        if (id == null) null else accounts.firstOrNull { it.id == id }

    /** 全库从未记过账 */
    val isTrulyEmpty: Boolean
        get() = !hasAnyActiveTransaction &&
            summary.expenseCents == 0L &&
            summary.incomeCents == 0L

    /** 有历史账，但当前区间无可见流水 */
    val isRangeEmpty: Boolean
        get() = hasAnyActiveTransaction &&
            visibleTransactions.isEmpty() &&
            summary.expenseCents == 0L &&
            summary.incomeCents == 0L &&
            summary.transferCents == 0L

    /**
     * 用于 distinct：summary / 最近 id / 饼图趋势签名，减少无关字段抖动导致整屏重组。
     */
    fun contentSignature(): List<Any?> = listOf(
        isLoading,
        filter,
        summary,
        totalAssetsCents,
        hasAnyActiveTransaction,
        visibleTransactions.map { it.id to it.updatedAt },
        pieData,
        lineData,
        assetBreakdown,
        categories.map { Triple(it.id, it.name, it.isArchived) },
        accounts.map { Triple(it.id, it.name, it.isArchived) }
    )
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

    fun buildAssetBreakdown(
        accounts: List<AccountEntity>,
        balanceRows: List<AccountBalanceRow>
    ): List<AccountAssetLine> {
        val balanceMap = balanceRows.associate { it.accountId to it.balance }
        return accounts
            .filter { !it.isArchived }
            .map { account ->
                AccountAssetLine(
                    accountId = account.id,
                    name = account.name,
                    balanceCents = balanceMap[account.id] ?: 0L
                )
            }
    }

    fun totalAssetsCents(breakdown: List<AccountAssetLine>): Long =
        breakdown.sumOf { it.balanceCents }

    fun buildState(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        filter: BillFilter,
        balanceRows: List<AccountBalanceRow> = emptyList()
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

        val assetBreakdown = buildAssetBreakdown(accounts, balanceRows)

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
            totalAssetsCents = totalAssetsCents(assetBreakdown),
            assetBreakdown = assetBreakdown,
            hasAnyActiveTransaction = transactions.isNotEmpty(),
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
