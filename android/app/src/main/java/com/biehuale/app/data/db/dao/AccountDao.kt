package com.biehuale.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.biehuale.app.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账户 DAO
 *
 * 余额计算（关键业务，最易错）：
 *  balance = initial_balance
 *          + Σ(INCOME  where account_id = :id AND deleted_at IS NULL)
 *          - Σ(EXPENSE where account_id = :id AND deleted_at IS NULL)
 *          - Σ(TRANSFER where account_id = :id AND deleted_at IS NULL)
 *          + Σ(TRANSFER where to_account_id = :id AND deleted_at IS NULL)
 *
 * 详见 docs/DEV_PLAN.md §5 Task 2.3
 */
@Dao
interface AccountDao {

    // ---------- 查询 ----------

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY id ASC")
    fun observeActive(): Flow<List<AccountEntity>>

    /** 含已归档，供账单/详情展示「（已归档）」 */
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE is_archived = 0 AND name = :name LIMIT 1")
    suspend fun findActiveByName(name: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE is_archived = 0")
    suspend fun countActive(): Int

    /**
     * 单账户余额（含转账两端）
     * 返回 Long 分
     */
    @Query("""
        SELECT
            a.initial_balance
            + COALESCE((SELECT SUM(amount) FROM transactions
                WHERE account_id = a.id AND type = 'INCOME' AND deleted_at IS NULL), 0)
            - COALESCE((SELECT SUM(amount) FROM transactions
                WHERE account_id = a.id AND type = 'EXPENSE' AND deleted_at IS NULL), 0)
            - COALESCE((SELECT SUM(amount) FROM transactions
                WHERE account_id = a.id AND type = 'TRANSFER' AND deleted_at IS NULL), 0)
            + COALESCE((SELECT SUM(amount) FROM transactions
                WHERE to_account_id = a.id AND type = 'TRANSFER' AND deleted_at IS NULL), 0)
        FROM accounts a WHERE a.id = :id
    """)
    suspend fun getBalance(id: Long): Long?

    // ---------- 写入 ----------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    /**
     * 软删除：is_archived = 1
     * 不真删，因为 transactions.account_id 是 ON DELETE RESTRICT
     */
    @Query("UPDATE accounts SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun archive(id: Long, now: Long)

    @Query("UPDATE accounts SET is_archived = 0, updated_at = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long)
}
