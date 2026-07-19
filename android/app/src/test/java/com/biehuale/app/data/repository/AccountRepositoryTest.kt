package com.biehuale.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AccountRepository tests - balance calculation */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepo: AccountRepository
    private lateinit var transactionRepo: TransactionRepository
    private var expenseCatId = 0L
    private var incomeCatId = 0L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        accountRepo = AccountRepository(db.accountDao(), db.quickRecordDao())
        transactionRepo = TransactionRepository(db.transactionDao(), db.accountDao(), db.categoryDao())
        val now = System.currentTimeMillis()
        expenseCatId = db.categoryDao().insert(
            CategoryEntity(0, "\u9910\u996e", "restaurant", "#FF5722", CategoryType.EXPENSE, true, 1, false, now, now)
        )
        incomeCatId = db.categoryDao().insert(
            CategoryEntity(0, "\u5de5\u8d44", "payments", "#4CAF50", CategoryType.INCOME, true, 1, false, now, now)
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun initialBalance_equalsInitialBalance() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 100_00L)
        assertThat(accountRepo.getBalance(id)).isEqualTo(100_00L)
    }

    @Test
    fun initialBalance_zero() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)
        assertThat(accountRepo.getBalance(id)).isEqualTo(0L)
    }

    @Test
    fun income_addsToBalance() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 100_00L)
        saveTx(accountId = id, amount = 500_00L, type = TransactionType.INCOME)
        assertThat(accountRepo.getBalance(id)).isEqualTo(600_00L)
    }

    @Test
    fun expense_subtractsFromBalance() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 100_00L)
        saveTx(accountId = id, amount = 30_00L, type = TransactionType.EXPENSE)
        assertThat(accountRepo.getBalance(id)).isEqualTo(70_00L)
    }

    @Test
    fun transfer_fromAccount_decreasesBalance() = runTest {
        val fromId = accountRepo.create(name = "\u5fae\u4fe1", initialBalance = 1000_00L)
        val toId = accountRepo.create(name = "\u94f6\u884c\u5361", initialBalance = 0L)
        saveTx(
            accountId = fromId,
            amount = 500_00L,
            type = TransactionType.TRANSFER,
            toAccountId = toId
        )
        assertThat(accountRepo.getBalance(fromId)).isEqualTo(500_00L)
    }

    @Test
    fun transfer_toAccount_increasesBalance() = runTest {
        val fromId = accountRepo.create(name = "\u5fae\u4fe1", initialBalance = 1000_00L)
        val toId = accountRepo.create(name = "\u94f6\u884c\u5361", initialBalance = 0L)
        saveTx(
            accountId = fromId,
            amount = 500_00L,
            type = TransactionType.TRANSFER,
            toAccountId = toId
        )
        assertThat(accountRepo.getBalance(toId)).isEqualTo(500_00L)
    }

    @Test
    fun transfer_conservesTotalAssets() = runTest {
        val fromId = accountRepo.create(name = "\u5fae\u4fe1", initialBalance = 1000_00L)
        val toId = accountRepo.create(name = "\u94f6\u884c\u5361", initialBalance = 500_00L)
        val beforeTotal = (accountRepo.getBalance(fromId) ?: 0L) + (accountRepo.getBalance(toId) ?: 0L)

        saveTx(
            accountId = fromId,
            amount = 300_00L,
            type = TransactionType.TRANSFER,
            toAccountId = toId
        )

        val afterTotal = (accountRepo.getBalance(fromId) ?: 0L) + (accountRepo.getBalance(toId) ?: 0L)
        assertThat(afterTotal).isEqualTo(beforeTotal)
    }

    @Test
    fun complexScene_multipleTransactions() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 1000_00L)
        saveTx(id, 500_00L, TransactionType.INCOME)
        saveTx(id, 200_00L, TransactionType.EXPENSE)
        saveTx(id, 50_00L, TransactionType.EXPENSE)
        assertThat(accountRepo.getBalance(id)).isEqualTo(1250_00L)
    }

    @Test
    fun softDeleted_transaction_excludedFromBalance() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 1000_00L)
        val txId = saveTx(id, 200_00L, TransactionType.EXPENSE)
        assertThat(accountRepo.getBalance(id)).isEqualTo(800_00L)

        transactionRepo.softDelete(txId)
        assertThat(accountRepo.getBalance(id)).isEqualTo(1000_00L)
    }

    @Test
    fun restore_softDeleted_updatesBalance() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 1000_00L)
        val txId = saveTx(id, 200_00L, TransactionType.EXPENSE)
        transactionRepo.softDelete(txId)
        assertThat(accountRepo.getBalance(id)).isEqualTo(1000_00L)

        transactionRepo.restore(txId)
        assertThat(accountRepo.getBalance(id)).isEqualTo(800_00L)
    }

    @Test
    fun missingAccount_getBalance_returnsNull() = runTest {
        assertThat(accountRepo.getBalance(999L)).isNull()
    }

    @Test
    fun archivedAccount_getBalance_stillWorks() = runTest {
        val id = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 1000_00L)
        accountRepo.archive(id)
        assertThat(accountRepo.getBalance(id)).isEqualTo(1000_00L)
    }

    @Test
    fun observeActive_excludesArchived() = runTest {
        val id1 = accountRepo.create(name = "\u73b0\u91d1")
        val id2 = accountRepo.create(name = "\u5fae\u4fe1")
        accountRepo.archive(id1)
        val active = accountRepo.observeActive().first()
        assertThat(active.map { it.id }).containsExactly(id2)
    }

    @Test
    fun archive_deletesDependentQuickRecords() = runTest {
        val accountId = accountRepo.create(name = "\u73b0\u91d1")
        val now = System.currentTimeMillis()
        val categoryId = db.categoryDao().insert(
            com.biehuale.app.data.db.entity.CategoryEntity(
                0, "\u4ea4\u901a", null, null,
                com.biehuale.app.domain.model.CategoryType.EXPENSE,
                true, 1, false, now, now
            )
        )
        db.quickRecordDao().insert(
            com.biehuale.app.data.db.entity.QuickRecordEntity(
                id = 0L,
                categoryId = categoryId,
                accountId = accountId,
                amount = 400L,
                description = "metro",
                sortOrder = 1,
                createdAt = now,
                updatedAt = now
            )
        )
        assertThat(db.quickRecordDao().getAll()).hasSize(1)
        accountRepo.archive(accountId)
        assertThat(db.quickRecordDao().getAll()).isEmpty()
    }

    private suspend fun saveTx(
        accountId: Long,
        amount: Long,
        type: TransactionType,
        toAccountId: Long? = null,
        categoryId: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        val resolvedCategoryId = when (type) {
            TransactionType.TRANSFER -> null
            TransactionType.INCOME -> categoryId ?: incomeCatId
            TransactionType.EXPENSE -> categoryId ?: expenseCatId
        }
        val tx = TransactionEntity(
            id = 0L,
            amount = amount,
            type = type,
            categoryId = resolvedCategoryId,
            accountId = accountId,
            toAccountId = toAccountId,
            description = null,
            occurredAt = now,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        return transactionRepo.save(tx)
    }
}
