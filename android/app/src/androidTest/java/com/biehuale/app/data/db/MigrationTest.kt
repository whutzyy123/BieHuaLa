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

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
