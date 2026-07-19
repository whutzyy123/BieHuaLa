package com.biehuale.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 最小 androidTest 烟雾：in-memory Room 插入一笔后可 observe 到。
 *
 * 详见 docs/DEV_PLAN.md Task 5.6（务实子集，非 Compose 全链路 E2E）
 */
@RunWith(AndroidJUnit4::class)
class TransactionRoomSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertThenObserve_activeTransactionVisible() = runBlocking {
        val now = System.currentTimeMillis()
        db.accountDao().insert(
            AccountEntity(
                id = 1L,
                name = "\u73b0\u91d1",
                icon = "cash",
                colorHex = null,
                initialBalance = 0L,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
        )
        db.categoryDao().insert(
            CategoryEntity(
                id = 1L,
                name = "\u9910\u996e",
                icon = null,
                colorHex = "#FF0000",
                type = CategoryType.EXPENSE,
                isBuiltin = true,
                sortOrder = 1,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                id = 0L,
                amount = 12_34L,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                accountId = 1L,
                toAccountId = null,
                description = "smoke",
                occurredAt = now,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            )
        )

        val active = db.transactionDao().observeAllActive().first()
        assertThat(active).hasSize(1)
        assertThat(active.first().amount).isEqualTo(12_34L)
        assertThat(active.first().type).isEqualTo(TransactionType.EXPENSE)
        assertThat(active.first().description).isEqualTo("smoke")
    }
}
