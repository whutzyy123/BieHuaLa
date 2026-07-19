package com.biehuale.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.dao.TransactionDao
import com.biehuale.app.data.seed.DefaultCategories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Database Module
 *
 * 职责：提供 Room AppDatabase + 3 个 DAO 的注入
 *
 * 启动策略：
 *  - AppDatabase 单例（@Singleton）
 *  - 3 个 DAO 各自单例（Room 内部已单例管理，加 @Singleton 更显式）
 *
 * Seed 策略：
 *  - onCreate：首次创建数据库时同步插入内置类别（15 条 INSERT，通常 < 10ms）
 *  - 不用 CoroutineScope：Room Callback.onCreate 本身在 DB 打开路径上同步执行
 *
 * 详见 docs/DEV_PLAN.md §4 Task 1.4 + §4 Task 1.5 + §4 Task 1.6
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供 AppDatabase 单例
     *
     * 关键决策：
     *  - fallbackToDestructiveMigration 关闭——生产数据不允许被破坏性回滚
     *  - 升级 schema 必须显式写 Migration
     *  - 首次创建时通过 addCallback 注入 15 个内置类别
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .addCallback(SeedCallback())
        // TODO(Phase 5): 加 .setQueryCallback / .setQueryExecutor 性能优化
        .build()

    @Provides
    @Singleton
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
}

/**
 * 首次创建数据库时插入 seed 数据
 *
 * 注意：
 *  - onCreate 在数据库首次创建时调用
 *  - 同步执行（15 条 INSERT < 10ms，不影响启动）
 *  - 用 db.execSQL 直接插入（callback 在 Room 内部触发，Hilt DI 图未就绪，无法注入 CategoryDao）
 *  - createdAt / updatedAt 用真实当前时间（覆盖 DefaultCategories 中的 0L）
 */
private class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        // 默认账户「现金」——闭合 Demo 1 记账路径（设置页仍可再建账户）
        db.execSQL(
            """
            INSERT INTO accounts
                (name, icon, color, initial_balance, is_archived, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf("现金", "cash", "#FF9800", 0L, 0, now, now)
        )
        DefaultCategories.ALL.forEach { c ->
            db.execSQL(
                """
                INSERT INTO categories
                    (name, icon, color, type, is_builtin, sort_order, is_archived, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    c.name,
                    c.icon,
                    c.colorHex,
                    c.type.name, // Room bindArgs 不接受 enum
                    if (c.isBuiltin) 1 else 0,
                    c.sortOrder,
                    if (c.isArchived) 1 else 0,
                    now,
                    now
                )
            )
        }
    }
}
