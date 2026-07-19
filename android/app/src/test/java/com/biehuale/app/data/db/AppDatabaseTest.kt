package com.biehuale.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.data.seed.DefaultCategories
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AppDatabase integration tests - in-memory Room */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepo: AccountRepository
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var transactionRepo: TransactionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepo = AccountRepository(db.accountDao())
        categoryRepo = CategoryRepository(db.categoryDao())
        transactionRepo = TransactionRepository(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun database_canBeCreated_andDaosAccessible() = runBlocking {
        assertThat(db.openHelper.writableDatabase.isOpen).isTrue()
        assertThat(db.accountDao().countActive()).isEqualTo(0)
        assertThat(db.categoryDao().getAllBuiltin()).isEmpty()
    }

    @Test
    fun defaultCategories_seed_expenseAndIncomeInserted() = runBlocking {
        val all = categoryRepo.observeAll().first()
        assertThat(all).isEmpty()

        categoryRepo.resetBuiltinDefaults()

        val after = categoryRepo.observeAll().first()
        val expense = after.filter { it.type == CategoryType.EXPENSE }
        val income = after.filter { it.type == CategoryType.INCOME }

        assertThat(expense.size).isEqualTo(DefaultCategories.ALL.count { it.type == CategoryType.EXPENSE })
        assertThat(income.size).isEqualTo(DefaultCategories.ALL.count { it.type == CategoryType.INCOME })
        assertThat(after.all { it.isBuiltin }).isTrue()
        assertThat(after.all { !it.isArchived }).isTrue()
    }

    @Test
    fun fullWorkflow_accountCategoryTxBalanceSoftDeleteCleanup() = runBlocking {
        categoryRepo.resetBuiltinDefaults()
        val foodCategory = categoryRepo.observeAll().first()
            .first { it.type == CategoryType.EXPENSE && it.name == "\u9910\u996e" }
        val incomeCategory = categoryRepo.observeAll().first()
            .first { it.type == CategoryType.INCOME }

        val cashId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 1000_00L)
        val bankId = accountRepo.create(name = "\u94f6\u884c\u5361", initialBalance = 0L)

        val now = System.currentTimeMillis()
        val tx1 = transactionRepo.save(
            newTx(amount = 50_00L, type = TransactionType.EXPENSE, categoryId = foodCategory.id, accountId = cashId, occurredAt = now)
        )
        transactionRepo.save(
            newTx(amount = 200_00L, type = TransactionType.INCOME, categoryId = incomeCategory.id, accountId = cashId, occurredAt = now)
        )
        val tx3 = transactionRepo.save(
            newTx(amount = 100_00L, type = TransactionType.TRANSFER, categoryId = null, accountId = cashId, toAccountId = bankId, occurredAt = now)
        )

        assertThat(accountRepo.getBalance(cashId)).isEqualTo(1050_00L)
        assertThat(accountRepo.getBalance(bankId)).isEqualTo(100_00L)

        transactionRepo.softDelete(tx1)
        assertThat(accountRepo.getBalance(cashId)).isEqualTo(1100_00L)

        val recycled = transactionRepo.observeRecycleBin().first()
        assertThat(recycled).hasSize(1)
        assertThat(recycled.first().id).isEqualTo(tx1)

        transactionRepo.restore(tx1)
        assertThat(accountRepo.getBalance(cashId)).isEqualTo(1050_00L)

        transactionRepo.hardDelete(tx3)
        assertThat(accountRepo.getBalance(bankId)).isEqualTo(0L)
    }

    @Test
    fun balanceCalculation_allTypeCombinations() = runBlocking {
        val accId = accountRepo.create(name = "\u6d4b\u8bd5", initialBalance = 1000_00L)
        val now = System.currentTimeMillis()
        val catId = categoryRepo.create(name = "\u6d4b\u8bd5\u7c7b", type = CategoryType.EXPENSE)
        val acc2 = accountRepo.create(name = "\u76ee\u6807", initialBalance = 0L)
        val incomeCatId = categoryRepo.create(name = "\u6d4b\u8bd5\u6536\u5165", type = CategoryType.INCOME)

        transactionRepo.save(newTx(amount = 100_00L, type = TransactionType.INCOME, categoryId = incomeCatId, accountId = accId, occurredAt = now))
        transactionRepo.save(newTx(amount = 50_00L, type = TransactionType.EXPENSE, categoryId = catId, accountId = accId, occurredAt = now))
        transactionRepo.save(newTx(amount = 200_00L, type = TransactionType.TRANSFER, categoryId = null, accountId = accId, toAccountId = acc2, occurredAt = now))
        transactionRepo.save(newTx(amount = 30_00L, type = TransactionType.TRANSFER, categoryId = null, accountId = acc2, toAccountId = accId, occurredAt = now))

        assertThat(accountRepo.getBalance(accId)).isEqualTo(880_00L)
        assertThat(accountRepo.getBalance(acc2)).isEqualTo(170_00L)
    }

    @Test
    fun archiveCategory_keepsTransactionCategoryId() = runBlocking {
        val catId = categoryRepo.create(name = "\u9910\u996e", type = CategoryType.EXPENSE)
        val accId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)
        val now = System.currentTimeMillis()
        val txId = transactionRepo.save(
            newTx(amount = 50_00L, type = TransactionType.EXPENSE, categoryId = catId, accountId = accId, occurredAt = now)
        )
        categoryRepo.archive(catId)
        val tx = transactionRepo.getById(txId)
        assertThat(tx).isNotNull()
        assertThat(tx?.categoryId).isEqualTo(catId)
    }

    @Test
    fun archiveAccount_keepsTransactionAccountId() = runBlocking {
        val accId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)
        val now = System.currentTimeMillis()
        val catId = categoryRepo.create(name = "\u6d4b\u8bd5", type = CategoryType.EXPENSE)
        val txId = transactionRepo.save(
            newTx(amount = 50_00L, type = TransactionType.EXPENSE, categoryId = catId, accountId = accId, occurredAt = now)
        )
        accountRepo.archive(accId)
        val tx = transactionRepo.getById(txId)
        assertThat(tx).isNotNull()
        assertThat(tx?.accountId).isEqualTo(accId)
        assertThat(accountRepo.observeActive().first()).isEmpty()
        assertThat(accountRepo.observeAll().first()).hasSize(1)
    }

    @Test
    fun cleanupExpired_deletesSoftDeletedOlderThan30Days() = runBlocking {
        val accId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)
        val now = System.currentTimeMillis()
        val catId = categoryRepo.create(name = "\u6d4b\u8bd5", type = CategoryType.EXPENSE)
        val oldTxId = transactionRepo.save(
            newTx(amount = 50_00L, type = TransactionType.EXPENSE, categoryId = catId, accountId = accId, occurredAt = now)
        )
        transactionRepo.softDelete(oldTxId)

        val deletedAt = now - 31L * 24 * 60 * 60 * 1000
        val soft = transactionRepo.getById(oldTxId)!!
        db.transactionDao().update(soft.copy(deletedAt = deletedAt, updatedAt = deletedAt))

        val threshold = now - 30L * 24 * 60 * 60 * 1000
        transactionRepo.cleanupExpired(threshold)

        assertThat(transactionRepo.getById(oldTxId)).isNull()
    }

    private fun newTx(
        id: Long = 0L,
        amount: Long,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long,
        toAccountId: Long? = null,
        occurredAt: Long
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            toAccountId = toAccountId,
            description = null,
            occurredAt = occurredAt,
            createdAt = if (id == 0L) now else 0L,
            updatedAt = if (id == 0L) now else 0L,
            deletedAt = null
        )
    }
}
