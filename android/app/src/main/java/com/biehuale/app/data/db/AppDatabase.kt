package com.biehuale.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.dao.TransactionDao
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
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
 *
 * exportSchema = true：编译时导出 schema JSON 到 $projectDir/schemas/
 * 用于 Migration 测试做 diff 验证。
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "biehuale.db"
        const val SCHEMA_VERSION = 3

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
    }
}
