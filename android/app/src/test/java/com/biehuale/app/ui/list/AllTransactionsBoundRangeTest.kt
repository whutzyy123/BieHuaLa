package com.biehuale.app.ui.list

import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.bill.BillFilter
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AllTransactionsBoundRangeTest {

    @Test
    fun noBound_allowsAll() {
        val tx = sample(occurredAt = 1_000L)
        assertThat(
            AllTransactionsViewModel.matchesBoundRange(tx, BillFilter())
        ).isTrue()
    }

    @Test
    fun boundRange_halfOpenInclusiveStart() {
        val filter = BillFilter(
            boundRangeStart = 100L,
            boundRangeEndExclusive = 200L
        )
        assertThat(
            AllTransactionsViewModel.matchesBoundRange(sample(100L), filter)
        ).isTrue()
        assertThat(
            AllTransactionsViewModel.matchesBoundRange(sample(199L), filter)
        ).isTrue()
        assertThat(
            AllTransactionsViewModel.matchesBoundRange(sample(200L), filter)
        ).isFalse()
        assertThat(
            AllTransactionsViewModel.matchesBoundRange(sample(99L), filter)
        ).isFalse()
    }

    private fun sample(occurredAt: Long) = TransactionEntity(
        id = 1L,
        amount = 100L,
        type = TransactionType.EXPENSE,
        categoryId = 1L,
        accountId = 1L,
        toAccountId = null,
        description = null,
        occurredAt = occurredAt,
        createdAt = occurredAt,
        updatedAt = occurredAt,
        deletedAt = null
    )
}
