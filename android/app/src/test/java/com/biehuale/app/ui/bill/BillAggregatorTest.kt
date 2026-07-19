package com.biehuale.app.ui.bill

import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.TimeZone

class BillAggregatorTest {

    private val categories = listOf(
        CategoryEntity(
            id = 1L, name = "餐饮", icon = null, colorHex = "#FF0000",
            type = CategoryType.EXPENSE, isBuiltin = true, sortOrder = 1,
            isArchived = false, createdAt = 1L, updatedAt = 1L
        ),
        CategoryEntity(
            id = 2L, name = "交通", icon = null, colorHex = "#00FF00",
            type = CategoryType.EXPENSE, isBuiltin = true, sortOrder = 2,
            isArchived = false, createdAt = 1L, updatedAt = 1L
        )
    )
    private val accounts = listOf(
        AccountEntity(
            id = 10L, name = "现金", icon = "cash", colorHex = null,
            initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
        ),
        AccountEntity(
            id = 11L, name = "银行", icon = "bank", colorHex = null,
            initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
        )
    )

    @Test
    fun transfer_notCountedInExpenseOrIncome() {
        val day = localDay(2026, 7, 10, 12, 0)
        val txs = listOf(
            tx(1, 100_00L, TransactionType.EXPENSE, 1L, 10L, null, day),
            tx(2, 200_00L, TransactionType.INCOME, null, 10L, null, day),
            tx(3, 500_00L, TransactionType.TRANSFER, null, 10L, 11L, day)
        )
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(year = 2026, month = 7)
        )
        assertThat(state.summary.expenseCents).isEqualTo(100_00L)
        assertThat(state.summary.incomeCents).isEqualTo(200_00L)
        assertThat(state.summary.transferCents).isEqualTo(500_00L)
        assertThat(state.summary.netCents).isEqualTo(100_00L)
    }

    @Test
    fun ratio_hiddenWhenIncomeZero() {
        val day = localDay(2026, 7, 10, 12, 0)
        val txs = listOf(tx(1, 50_00L, TransactionType.EXPENSE, 1L, 10L, null, day))
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(year = 2026, month = 7)
        )
        assertThat(state.summary.expenseIncomeRatioOrNull).isNull()
    }

    @Test
    fun ratio_computedWhenIncomePositive() {
        val day = localDay(2026, 7, 10, 12, 0)
        val txs = listOf(
            tx(1, 50_00L, TransactionType.EXPENSE, 1L, 10L, null, day),
            tx(2, 100_00L, TransactionType.INCOME, null, 10L, null, day)
        )
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(year = 2026, month = 7)
        )
        assertThat(state.summary.expenseIncomeRatioOrNull).isEqualTo(0.5f)
    }

    @Test
    fun search_onlyFiltersList_notSummary() {
        val day = localDay(2026, 7, 10, 12, 0)
        val txs = listOf(
            tx(1, 30_00L, TransactionType.EXPENSE, 1L, 10L, null, day, "星巴克"),
            tx(2, 70_00L, TransactionType.EXPENSE, 1L, 10L, null, day, "地铁")
        )
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(year = 2026, month = 7, keyword = "星巴克", isSearchOpen = true)
        )
        assertThat(state.summary.expenseCents).isEqualTo(100_00L)
        assertThat(state.visibleTransactions).hasSize(1)
        assertThat(state.visibleTransactions.first().description).isEqualTo("星巴克")
        assertThat(state.pieData.sumOf { it.totalCents }).isEqualTo(100_00L)
    }

    @Test
    fun customRange_includesEndDayTransactions() {
        val startDay = localDay(2026, 6, 1, 0, 0)
        val endDay = localDay(2026, 6, 15, 0, 0)
        val onEndAfternoon = localDay(2026, 6, 15, 18, 30)
        val afterEnd = localDay(2026, 6, 16, 1, 0)
        val txs = listOf(
            tx(1, 10_00L, TransactionType.EXPENSE, 1L, 10L, null, onEndAfternoon, "末日"),
            tx(2, 20_00L, TransactionType.EXPENSE, 1L, 10L, null, afterEnd, "次日")
        )
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(
                customRangeStart = startDay,
                customRangeEnd = endDay
            )
        )
        assertThat(state.summary.expenseCents).isEqualTo(10_00L)
        assertThat(state.visibleTransactions.map { it.id }).containsExactly(1L)
    }

    @Test
    fun trend_weekAndMonth_bucketCounts() {
        val start = localDay(2026, 7, 1, 0, 0)
        val endExclusive = localDay(2026, 8, 1, 0, 0)
        val expenses = listOf(
            tx(1, 10_00L, TransactionType.EXPENSE, 1L, 10L, null, localDay(2026, 7, 2, 10, 0)),
            tx(2, 20_00L, TransactionType.EXPENSE, 1L, 10L, null, localDay(2026, 7, 20, 10, 0))
        )
        val week = BillAggregator.buildLineSeries(
            expenses, start, endExclusive, TrendGranularity.WEEK
        )
        val month = BillAggregator.buildLineSeries(
            expenses, start, endExclusive, TrendGranularity.MONTH
        )
        assertThat(week.size).isAtLeast(4)
        assertThat(week.sumOf { it.totalCents }).isEqualTo(30_00L)
        assertThat(month).hasSize(1)
        assertThat(month.first().totalCents).isEqualTo(30_00L)
    }

    @Test
    fun utcPickerMillis_toLocalDayStart() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.JUNE, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val local = BillAggregator.utcPickerMillisToLocalDayStart(utc)
        val cal = Calendar.getInstance().apply { timeInMillis = local }
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(Calendar.JUNE)
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(15)
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
    }

    @Test
    fun filter_typeAccountCategory_combo() {
        val day = localDay(2026, 7, 10, 12, 0)
        val txs = listOf(
            tx(1, 10_00L, TransactionType.EXPENSE, 1L, 10L, null, day, "餐"),
            tx(2, 20_00L, TransactionType.EXPENSE, 2L, 11L, null, day, "车"),
            tx(3, 30_00L, TransactionType.INCOME, null, 10L, null, day, "薪"),
            tx(4, 40_00L, TransactionType.TRANSFER, null, 10L, 11L, day, "转")
        )
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(
                year = 2026,
                month = 7,
                types = setOf(TransactionType.EXPENSE),
                accountIds = setOf(10L),
                categoryIds = setOf(1L)
            )
        )
        assertThat(state.visibleTransactions.map { it.id }).containsExactly(1L)
        assertThat(state.summary.expenseCents).isEqualTo(10_00L)
        assertThat(state.summary.incomeCents).isEqualTo(0L)
    }

    @Test
    fun filter_searchDoesNotChangeSummary_withTypeFilter() {
        val day = localDay(2026, 7, 10, 12, 0)
        val txs = listOf(
            tx(1, 10_00L, TransactionType.EXPENSE, 1L, 10L, null, day, "A咖啡"),
            tx(2, 20_00L, TransactionType.EXPENSE, 1L, 10L, null, day, "B地铁")
        )
        val state = BillAggregator.buildState(
            txs, categories, accounts,
            BillFilter(
                year = 2026,
                month = 7,
                types = setOf(TransactionType.EXPENSE),
                keyword = "咖啡",
                isSearchOpen = true
            )
        )
        assertThat(state.summary.expenseCents).isEqualTo(30_00L)
        assertThat(state.visibleTransactions).hasSize(1)
        assertThat(state.visibleTransactions.first().id).isEqualTo(1L)
    }

    private fun localDay(y: Int, m: Int, d: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(y, m - 1, d, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun tx(
        id: Long,
        amount: Long,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long,
        toAccountId: Long?,
        occurredAt: Long,
        description: String? = null
    ) = TransactionEntity(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        toAccountId = toAccountId,
        description = description,
        occurredAt = occurredAt,
        createdAt = occurredAt,
        updatedAt = occurredAt,
        deletedAt = null
    )
}
