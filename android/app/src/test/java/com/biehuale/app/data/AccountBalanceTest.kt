package com.biehuale.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.TransactionRepository
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

/** Task 2.3: balance / transfer in-memory Room unit tests */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountBalanceTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository

    private var wechatId = 0L
    private var bankId = 0L
    private var expenseCatId = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepository = AccountRepository(db.accountDao(), db.quickRecordDao())
        transactionRepository = TransactionRepository(db.transactionDao(), db.accountDao(), db.categoryDao())

        val now = System.currentTimeMillis()
        wechatId = db.accountDao().insert(
            AccountEntity(0, "\u5fae\u4fe1", "wechat", "#07C160", 0L, false, now, now)
        )
        bankId = db.accountDao().insert(
            AccountEntity(0, "\u94f6\u884c\u5361", "bank", "#2196F3", 0L, false, now, now)
        )
        expenseCatId = db.categoryDao().insert(
            CategoryEntity(
                0, "\u9910\u996e", "restaurant", "#FF5722", CategoryType.EXPENSE,
                true, 1, false, now, now
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun transfer_500_updatesBothBalances_andNotExpense() = runBlocking {
        val now = System.currentTimeMillis()
        transactionRepository.save(
            TransactionEntity(
                id = 0L,
                amount = 500_00L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                accountId = wechatId,
                toAccountId = bankId,
                description = null,
                occurredAt = now,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null
            )
        )

        assertThat(accountRepository.getBalance(wechatId)).isEqualTo(-500_00L)
        assertThat(accountRepository.getBalance(bankId)).isEqualTo(500_00L)

        val monthExpense = db.transactionDao().observeExpenseSum(
            now - 86_400_000L,
            now + 86_400_000L
        ).first()
        assertThat(monthExpense).isEqualTo(0L)
    }

    @Test
    fun softDelete_transfer_revertsBalance() = runBlocking {
        val now = System.currentTimeMillis()
        val id = transactionRepository.save(
            TransactionEntity(
                id = 0L,
                amount = 500_00L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                accountId = wechatId,
                toAccountId = bankId,
                description = null,
                occurredAt = now,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null
            )
        )
        transactionRepository.softDelete(id)
        assertThat(accountRepository.getBalance(wechatId)).isEqualTo(0L)
        assertThat(accountRepository.getBalance(bankId)).isEqualTo(0L)
    }

    @Test
    fun sameAccountTransfer_rejected() = runBlocking {
        val now = System.currentTimeMillis()
        try {
            transactionRepository.save(
                TransactionEntity(
                    id = 0L,
                    amount = 100_00L,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountId = wechatId,
                    toAccountId = wechatId,
                    description = null,
                    occurredAt = now,
                    createdAt = 0L,
                    updatedAt = 0L,
                    deletedAt = null
                )
            )
            error("should have thrown")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("\u8f6c\u5165\u8d26\u6237\u5fc5\u987b\u4e0e\u8f6c\u51fa\u4e0d\u540c")
        }
    }

    @Test
    fun expense_thenTransfer_balanceFormula() = runBlocking {
        val now = System.currentTimeMillis()
        transactionRepository.save(
            TransactionEntity(
                id = 0,
                amount = 50_00L,
                type = TransactionType.EXPENSE,
                categoryId = expenseCatId,
                accountId = wechatId,
                occurredAt = now,
                createdAt = 0,
                updatedAt = 0
            )
        )
        transactionRepository.save(
            TransactionEntity(
                id = 0,
                amount = 100_00L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                accountId = wechatId,
                toAccountId = bankId,
                occurredAt = now,
                createdAt = 0,
                updatedAt = 0
            )
        )
        assertThat(accountRepository.getBalance(wechatId)).isEqualTo(-150_00L)
        assertThat(accountRepository.getBalance(bankId)).isEqualTo(100_00L)
    }

    @Test
    fun transfer_withFee_deductsFromReceiver() = runBlocking {
        val now = System.currentTimeMillis()
        // 转 500，手续费 1 → 转出 -500，转入 +499
        transactionRepository.save(
            TransactionEntity(
                id = 0L,
                amount = 500_00L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                accountId = wechatId,
                toAccountId = bankId,
                fee = 1_00L,
                description = null,
                occurredAt = now,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null
            )
        )

        assertThat(accountRepository.getBalance(wechatId)).isEqualTo(-500_00L)
        assertThat(accountRepository.getBalance(bankId)).isEqualTo(499_00L)

        val monthExpense = db.transactionDao().observeExpenseSum(
            now - 86_400_000L,
            now + 86_400_000L
        ).first()
        assertThat(monthExpense).isEqualTo(0L)
    }

    @Test
    fun transfer_feeNotLessThanAmount_rejected() = runBlocking {
        val now = System.currentTimeMillis()
        try {
            transactionRepository.save(
                TransactionEntity(
                    id = 0L,
                    amount = 100_00L,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountId = wechatId,
                    toAccountId = bankId,
                    fee = 100_00L,
                    occurredAt = now,
                    createdAt = 0L,
                    updatedAt = 0L
                )
            )
            error("should have thrown")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("手续费必须小于转账金额")
        }
    }
}
