package com.biehuale.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.biehuale.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 交易 DAO（核心表）
 *
 * 默认查询条件：`deleted_at IS NULL`（排除软删除）
 *
 * 详见：
 *  - docs/PRD.md §5.2 / §6
 *  - docs/DEV_PLAN.md §5 Task 2.3-2.5 / §6 Task 3.2-3.6
 */
@Dao
interface TransactionDao {

    // ---------- 基础查询 ----------

    @Query("SELECT * FROM transactions WHERE deleted_at IS NULL ORDER BY occurred_at DESC")
    fun observeAllActive(): Flow<List<TransactionEntity>>

    /** 含软删除，供备份导入去重指纹 */
    @Query("SELECT * FROM transactions")
    suspend fun getAllIncludingDeleted(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TransactionEntity?>

    // ---------- 时间区间查询 ----------

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND occurred_at >= :start AND occurred_at < :end
        ORDER BY occurred_at DESC
    """)
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    // ---------- 聚合 ----------

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE'
          AND occurred_at >= :start AND occurred_at < :end
          AND deleted_at IS NULL
    """)
    fun observeExpenseSum(start: Long, end: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME'
          AND occurred_at >= :start AND occurred_at < :end
          AND deleted_at IS NULL
    """)
    fun observeIncomeSum(start: Long, end: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER'
          AND occurred_at >= :start AND occurred_at < :end
          AND deleted_at IS NULL
    """)
    fun observeTransferSum(start: Long, end: Long): Flow<Long>

    /**
     * 分类聚合 - 用于饼图
     * 返回 (categoryId, totalCents)
     */
    @Query("""
        SELECT category_id AS categoryId, SUM(amount) AS totalCents
        FROM transactions
        WHERE type = 'EXPENSE'
          AND occurred_at >= :start AND occurred_at < :end
          AND deleted_at IS NULL
          AND category_id IS NOT NULL
        GROUP BY category_id
        ORDER BY totalCents DESC
    """)
    fun observeCategoryBreakdown(start: Long, end: Long): Flow<List<CategoryTotal>>

    /**
     * 按日聚合 - 用于趋势折线图
     * 返回 (dayEpochMillis, totalCents)
     *
     * @param startDayMillis 当天 00:00:00 的 epoch millis
     * @param endDayMillis   下一天 00:00:00 的 epoch millis
     * @param dayInMillis    一天的毫秒数（默认 86_400_000 = 24h，调用方必须显式传以避免 Room 默认值歧义）
     */
    @Query("""
        SELECT
            (occurred_at - (occurred_at % :dayInMillis)) AS dayStart,
            SUM(amount) AS totalCents
        FROM transactions
        WHERE type = 'EXPENSE'
          AND occurred_at >= :startDayMillis AND occurred_at < :endDayMillis
          AND deleted_at IS NULL
        GROUP BY dayStart
        ORDER BY dayStart ASC
    """)
    fun observeDailyExpense(
        startDayMillis: Long,
        endDayMillis: Long,
        dayInMillis: Long
    ): Flow<List<DailyTotal>>

    // ---------- 账户/类别维度 ----------

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL
          AND (account_id = :accountId OR to_account_id = :accountId)
        ORDER BY occurred_at DESC
    """)
    fun observeByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NULL AND category_id = :categoryId
        ORDER BY occurred_at DESC
    """)
    fun observeByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    // ---------- 回收站 ----------

    @Query("SELECT * FROM transactions WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeRecycleBin(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE deleted_at IS NOT NULL
          AND deleted_at < :thresholdMillis
    """)
    suspend fun getExpiredForCleanup(thresholdMillis: Long): List<TransactionEntity>

    // ---------- 写入 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    /**
     * 软删除：设置 deleted_at = now
     * 不真删，30 天后由 WorkManager 清理（见 docs/DEV_PLAN.md §7 Task 4.5）
     */
    @Query("UPDATE transactions SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    /**
     * 恢复：从回收站恢复。返回受影响行数（0 = 无此 id 或已非软删状态视 SQL 而定）。
     */
    @Query(
        """
        UPDATE transactions
        SET deleted_at = NULL, updated_at = :now
        WHERE id = :id AND deleted_at IS NOT NULL
        """
    )
    suspend fun restore(id: Long, now: Long): Int

    /**
     * 永久删除单笔
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun hardDeleteIds(ids: List<Long>)

    /** 清空回收站（单条 SQL，原子） */
    @Query("DELETE FROM transactions WHERE deleted_at IS NOT NULL")
    suspend fun hardDeleteAllSoftDeleted()

    /** 清理过期软删（单条 SQL，原子） */
    @Query(
        """
        DELETE FROM transactions
        WHERE deleted_at IS NOT NULL AND deleted_at < :thresholdMillis
        """
    )
    suspend fun hardDeleteExpired(thresholdMillis: Long)
}

/**
 * 分类聚合结果 - 用于饼图
 */
data class CategoryTotal(
    val categoryId: Long,
    val totalCents: Long
)

/**
 * 按日聚合结果 - 用于趋势图
 */
data class DailyTotal(
    val dayStart: Long,
    val totalCents: Long
)
