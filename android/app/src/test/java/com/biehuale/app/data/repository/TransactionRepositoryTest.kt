package com.biehuale.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.entity.AccountEntity
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

/** TransactionRepository tests - save() validation */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TransactionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionRepo: TransactionRepository
    private var accountId = 0L
    private var account2Id = 0L
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
        transactionRepo = TransactionRepository(db.transactionDao(), db.accountDao(), db.categoryDao())
        val now = System.currentTimeMillis()
        accountId = db.accountDao().insert(
            AccountEntity(0, "\u73b0\u91d1", "cash", "#FF9800", 0L, false, now, now)
        )
        account2Id = db.accountDao().insert(
            AccountEntity(0, "\u94f6\u884c\u5361", "bank", "#2196F3", 0L, false, now, now)
        )
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
    fun amount_mustBeGreaterThanZero() = runTest {
        val tx = newTx(amount = 0L)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(ex?.message).contains("\u91d1\u989d\u5fc5\u987b\u5927\u4e8e 0")
    }

    @Test
    fun amount_rejectsAboveMaxCents() = runTest {
        val tx = newTx(amount = com.biehuale.app.util.Money.MAX_CENTS + 1)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u4e0a\u9650")
    }

    @Test
    fun save_rejectsArchivedAccount() = runTest {
        db.accountDao().archive(accountId, System.currentTimeMillis())
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u5f52\u6863")
    }

    @Test
    fun amount_negative_rejected() = runTest {
        val tx = newTx(amount = -1L)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun accountId_mustBePositive() = runTest {
        val tx = newTx(accountId = 0L)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u5fc5\u987b\u6307\u5b9a\u8d26\u6237")
    }

    @Test
    fun income_requiresCategoryId() = runTest {
        val tx = newTx(type = TransactionType.INCOME, categoryId = null)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u5fc5\u987b\u6307\u5b9a\u7c7b\u522b")
    }

    @Test
    fun income_shouldNotHaveToAccountId() = runTest {
        val tx = newTx(type = TransactionType.INCOME, categoryId = incomeCatId, toAccountId = account2Id)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u4e0d\u5e94\u8be5\u6709\u8f6c\u5165\u8d26\u6237")
    }

    @Test
    fun expense_requiresCategoryId() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = null)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u5fc5\u987b\u6307\u5b9a\u7c7b\u522b")
    }

    @Test
    fun transfer_requiresToAccountId() = runTest {
        val tx = newTx(type = TransactionType.TRANSFER, categoryId = null, toAccountId = null)
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u8f6c\u8d26\u5fc5\u987b\u6307\u5b9a\u8f6c\u5165\u8d26\u6237")
    }

    @Test
    fun transfer_toAccountId_cannotEqualAccountId() = runTest {
        val tx = newTx(
            accountId = accountId,
            type = TransactionType.TRANSFER,
            categoryId = null,
            toAccountId = accountId
        )
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u8f6c\u5165\u8d26\u6237\u5fc5\u987b\u4e0e\u8f6c\u51fa\u4e0d\u540c")
    }

    @Test
    fun transfer_shouldNotHaveCategoryId() = runTest {
        val tx = newTx(
            accountId = accountId,
            type = TransactionType.TRANSFER,
            categoryId = expenseCatId,
            toAccountId = account2Id
        )
        val ex = runCatching { transactionRepo.save(tx) }.exceptionOrNull()
        assertThat(ex?.message).contains("\u8f6c\u8d26\u4e0d\u5e94\u8be5\u6709\u7c7b\u522b")
    }

    @Test
    fun income_savesSuccessfully() = runTest {
        val tx = newTx(type = TransactionType.INCOME, categoryId = incomeCatId)
        val id = transactionRepo.save(tx)
        assertThat(id).isGreaterThan(0L)
    }

    @Test
    fun expense_savesSuccessfully() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val id = transactionRepo.save(tx)
        assertThat(id).isGreaterThan(0L)
    }

    @Test
    fun transfer_savesSuccessfully() = runTest {
        val tx = newTx(
            accountId = accountId,
            type = TransactionType.TRANSFER,
            categoryId = null,
            toAccountId = account2Id
        )
        val id = transactionRepo.save(tx)
        assertThat(id).isGreaterThan(0L)
    }

    @Test
    fun idZero_isInsert_nonzero_isUpdate() = runTest {
        val tx1 = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val newId = transactionRepo.save(tx1)
        assertThat(newId).isGreaterThan(0L)

        val tx2 = newTx(
            id = newId,
            amount = 200L,
            type = TransactionType.EXPENSE,
            categoryId = expenseCatId
        )
        val updatedId = transactionRepo.save(tx2)
        assertThat(updatedId).isEqualTo(newId)
    }

    @Test
    fun softDelete_excludesFromObserveAllActive() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val id = transactionRepo.save(tx)
        transactionRepo.softDelete(id)
        val active = transactionRepo.observeAllActive().first()
        assertThat(active).isEmpty()
    }

    @Test
    fun restore_clearsDeletedAt() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val id = transactionRepo.save(tx)
        transactionRepo.softDelete(id)
        transactionRepo.restore(id)
        val restored = transactionRepo.getById(id)
        assertThat(restored).isNotNull()
        assertThat(restored?.deletedAt).isNull()
    }

    @Test
    fun hardDelete_removesRow() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val id = transactionRepo.save(tx)
        transactionRepo.hardDelete(id)
        assertThat(transactionRepo.getById(id)).isNull()
    }

    @Test
    fun cleanupExpired_deletesOlderThanThreshold() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val id = transactionRepo.save(tx)
        transactionRepo.softDelete(id)

        // deleted_at < threshold 才清理：把删除时间回拨到 31 天前
        val now = System.currentTimeMillis()
        val deletedAt = now - 31L * 24 * 60 * 60 * 1000
        val soft = transactionRepo.getById(id)!!
        db.transactionDao().update(soft.copy(deletedAt = deletedAt, updatedAt = deletedAt))

        val threshold = now - 30L * 24 * 60 * 60 * 1000
        transactionRepo.cleanupExpired(threshold)

        assertThat(transactionRepo.getById(id)).isNull()
    }

    @Test
    fun cleanupExpired_keepsRecentSoftDeletes() = runTest {
        val tx = newTx(type = TransactionType.EXPENSE, categoryId = expenseCatId)
        val id = transactionRepo.save(tx)
        transactionRepo.softDelete(id)

        val threshold = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
        transactionRepo.cleanupExpired(threshold)

        assertThat(transactionRepo.getById(id)).isNotNull()
    }

    private fun newTx(
        id: Long = 0L,
        amount: Long = 100L,
        type: TransactionType = TransactionType.EXPENSE,
        accountId: Long? = null,
        categoryId: Long? = expenseCatId,
        toAccountId: Long? = null
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            accountId = accountId ?: this.accountId,
            toAccountId = toAccountId,
            description = null,
            occurredAt = now,
            createdAt = if (id == 0L) now else 0L,
            updatedAt = if (id == 0L) now else 0L,
            deletedAt = null
        )
    }
}
