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
 * 详见 docs/PRD.md §5.2 / §6
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

    /**
     * 区间内支出总额（测试与校验用；账单屏走内存聚合 BillAggregator）
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE'
          AND occurred_at >= :start AND occurred_at < :end
          AND deleted_at IS NULL
    """)
    fun observeExpenseSum(start: Long, end: Long): Flow<Long>

    // ---------- 回收站 ----------

    @Query("SELECT * FROM transactions WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeRecycleBin(): Flow<List<TransactionEntity>>

    // ---------- 写入 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    /**
     * 软删除：仅尚未软删的行；避免重置 30 天清理计时。
     */
    @Query(
        """
        UPDATE transactions
        SET deleted_at = :now, updated_at = :now
        WHERE id = :id AND deleted_at IS NULL
        """
    )
    suspend fun softDelete(id: Long, now: Long): Int

    /**
     * 恢复：从回收站恢复。返回受影响行数。
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
     * 永久删除单笔。返回受影响行数。
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: Long): Int

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
 * 分类聚合结果 - 用于饼图（由 BillAggregator 内存计算）
 */
data class CategoryTotal(
    val categoryId: Long,
    val totalCents: Long
)

/**
 * 按日聚合结果 - 用于趋势图（由 BillAggregator 内存计算）
 */
data class DailyTotal(
    val dayStart: Long,
    val totalCents: Long
)
