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
 *    枚举（DB schema 不变，仅 Kotlin 侧 + 单元测试）
 *  - 后续升级：version += 1，加 Migration 对象
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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "biehuale.db"
        const val SCHEMA_VERSION = 2

        /**
         * Migration 1 → 2：v0.2 升级
         *
         * 实际无 schema 变更（type 列在 v1 已存 "INCOME"/"EXPENSE"/"TRANSFER" 字符串），
         * 仅 Kotlin 侧的字段类型由 String 改成 enum，Converters 自动用 .name 互转。
         *
         * 此 Migration 留作"显式声明"——避免 Room 误判 schema hash 变化要求 destructiveMigration。
         * 也可以 no-op（空实现）但要保证 Room 不抛 "Migration didn't properly handle"。
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 显式 no-op：表结构未变，仅 Kotlin 侧字段类型升级
                // 数据格式完全兼容（"INCOME"/"EXPENSE"/"TRANSFER" 字符串继续可用）
            }
        }
    }
}
