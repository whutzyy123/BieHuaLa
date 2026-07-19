package com.biehuale.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room Migration Test - v1 → v2
 *
 * 详见 docs/PRD.md §9.4 / docs/DEV_PLAN.md §8 Task 5.6
 *
 * 跑在 androidTest 目录下（需要 InstrumentationRegistry）。
 *
 * 测试场景：
 *  - v1 schema 数据 → v2 schema 后所有数据保留
 *  - type 字段（"INCOME"/"EXPENSE"/"TRANSFER" 字符串）正确保留（Converters 自动转 enum）
 *  - 余额计算仍然正确（验证 SQL 仍能查到正确结果）
 *
 * 跑命令：`.\gradlew connectedAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1to2_preservesDataAndTypeValues() {
        // 1. 准备 v1 schema
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO accounts (id, name, icon, color, initial_balance, is_archived, created_at, updated_at)
                VALUES (1, '现金', 'cash', '#FF9800', 100000, 0, 1000, 1000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO categories (id, name, icon, color, type, is_builtin, sort_order, is_archived, created_at, updated_at)
                VALUES (1, '餐饮', 'restaurant', '#FF5722', 'EXPENSE', 1, 1, 0, 1000, 1000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO categories (id, name, icon, color, type, is_builtin, sort_order, is_archived, created_at, updated_at)
                VALUES (2, '工资', 'payments', '#4CAF50', 'INCOME', 1, 1, 0, 1000, 1000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO transactions (id, amount, type, category_id, account_id, to_account_id, description, occurred_at, created_at, updated_at, deleted_at)
                VALUES (1, 5000, 'EXPENSE', 1, 1, NULL, '午餐', 2000, 2000, 2000, NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO transactions (id, amount, type, category_id, account_id, to_account_id, description, occurred_at, created_at, updated_at, deleted_at)
                VALUES (2, 100000, 'INCOME', 2, 1, NULL, '7月薪资', 3000, 3000, 3000, NULL)
                """.trimIndent()
            )
            close()
        }

        // 2. 升级到 v2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        // 3. 验证数据保留
        db.query("SELECT name, initial_balance FROM accounts WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("现金")
            assertThat(cursor.getLong(1)).isEqualTo(100_000L)
        }

        db.query("SELECT name, type FROM categories WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("餐饮")
            assertThat(cursor.getString(1)).isEqualTo("EXPENSE")
        }

        db.query("SELECT name, type FROM categories WHERE id = 2").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("工资")
            assertThat(cursor.getString(1)).isEqualTo("INCOME")
        }

        db.query("SELECT type, amount, description FROM transactions ORDER BY id").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("EXPENSE")
            assertThat(cursor.getLong(1)).isEqualTo(5000L)
            assertThat(cursor.getString(2)).isEqualTo("午餐")
            cursor.moveToNext()
            assertThat(cursor.getString(0)).isEqualTo("INCOME")
            assertThat(cursor.getLong(1)).isEqualTo(100_000L)
            assertThat(cursor.getString(2)).isEqualTo("7月薪资")
        }

        db.close()
    }

    @Test
    fun migrate1to2_isNoOpForEmptyDatabase() {
        helper.createDatabase(TEST_DB, 1).apply { close() }
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
        db.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(0)
        }
        db.close()
    }

    @Test
    fun migrate2to3_addsToAccountIdIndex() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO accounts (id, name, icon, color, initial_balance, is_archived, created_at, updated_at)
                VALUES (1, '现金', NULL, NULL, 0, 0, 1000, 1000),
                       (2, '微信', NULL, NULL, 0, 0, 1000, 1000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO transactions (id, amount, type, category_id, account_id, to_account_id, description, occurred_at, created_at, updated_at, deleted_at)
                VALUES (1, 1000, 'TRANSFER', NULL, 1, 2, NULL, 2000, 2000, 2000, NULL)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 3, true,
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3
        )

        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_transactions_to_account_id'"
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("index_transactions_to_account_id")
        }
        db.query("SELECT to_account_id FROM transactions WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getLong(0)).isEqualTo(2L)
        }
        db.close()
    }

    @Test
    fun migrate3to4_createsQuickRecordsTable() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO accounts (id, name, icon, color, initial_balance, is_archived, created_at, updated_at)
                VALUES (1, '现金', NULL, NULL, 0, 0, 1000, 1000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO categories (id, name, icon, color, type, is_builtin, sort_order, is_archived, created_at, updated_at)
                VALUES (1, '交通', 'directions_transit', '#2196F3', 'EXPENSE', 1, 1, 0, 1000, 1000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 4, true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        )

        db.execSQL(
            """
            INSERT INTO quick_records
                (id, category_id, account_id, amount, description, sort_order, created_at, updated_at)
            VALUES (1, 1, 1, 400, '坐地铁', 1, 2000, 2000)
            """.trimIndent()
        )
        db.query("SELECT amount, description FROM quick_records WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getLong(0)).isEqualTo(400L)
            assertThat(cursor.getString(1)).isEqualTo("坐地铁")
        }
        db.close()
    }

    @Test
    fun migrate4to5_addsBalanceHotPathIndex() {
        helper.createDatabase(TEST_DB, 4).apply { close() }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 5, true,
            AppDatabase.MIGRATION_4_5
        )

        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' " +
                "AND name='index_transactions_account_id_deleted_at_type'"
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
        }
        db.close()
    }

    @Test
    fun migrate5to6_addsTransferFeeColumn() {
        helper.createDatabase(TEST_DB, 5).apply { close() }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 6, true,
            AppDatabase.MIGRATION_5_6
        )

        db.query("PRAGMA table_info(transactions)").use { cursor ->
            val names = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertThat(names).contains("fee")
        }
        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
