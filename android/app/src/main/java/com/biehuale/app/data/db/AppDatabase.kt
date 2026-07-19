package com.biehuale.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.dao.QuickRecordDao
import com.biehuale.app.data.db.dao.TransactionDao
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.QuickRecordEntity
import com.biehuale.app.data.db.entity.TransactionEntity

/**
 * 别花乐 (BieHuaLe) - Room AppDatabase
 *
 * 职责：聚合所有 Entity + DAO，App 单例
 *
 * 字段、外键、索引详见 docs/PRD.md §5.2
 *
 * Schema 版本演进：
 *  - version = 1：初始 schema（type 为 TEXT）
 *  - version = 2：type 字段由 String 升级为 TransactionType / CategoryType
 *    枚举（DB schema 不变，仅 Kotlin 侧）
 *  - version = 3：transactions.to_account_id 加索引
 *  - version = 4：新增 quick_records（快速记账模板）
 *  - version = 5：transactions (account_id, deleted_at, type) 余额热路径索引
 *  - version = 6：transactions.fee 转账手续费（分）
 *
 * exportSchema = true：编译时导出 schema JSON 到 $projectDir/schemas/
 * 用于 Migration 测试做 diff 验证。
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        QuickRecordEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun quickRecordDao(): QuickRecordDao

    companion object {
        const val DATABASE_NAME = "biehuale.db"
        const val SCHEMA_VERSION = 6

        /**
         * Migration 1 → 2：v0.2 升级（schema 无结构变更，显式 no-op）
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // no-op
            }
        }

        /**
         * Migration 2 → 3：为 to_account_id 外键补索引（消除全表扫描风险）
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_to_account_id` " +
                        "ON `transactions` (`to_account_id`)"
                )
            }
        }

        /**
         * Migration 3 → 4：快速记账模板表
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category_id` INTEGER NOT NULL,
                        `account_id` INTEGER NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `description` TEXT,
                        `sort_order` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quick_records_sort_order` " +
                        "ON `quick_records` (`sort_order`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quick_records_category_id` " +
                        "ON `quick_records` (`category_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quick_records_account_id` " +
                        "ON `quick_records` (`account_id`)"
                )
            }
        }

        /**
         * Migration 4 → 5：余额聚合索引
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_account_id_deleted_at_type` " +
                        "ON `transactions` (`account_id`, `deleted_at`, `type`)"
                )
            }
        }

        /**
         * Migration 5 → 6：转账手续费（默认 0，旧数据行为不变）
         */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `fee` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
