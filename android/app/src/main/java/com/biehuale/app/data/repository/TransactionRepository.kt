package com.biehuale.app.data.repository

import com.biehuale.app.data.db.dao.CategoryTotal
import com.biehuale.app.data.db.dao.DailyTotal
import com.biehuale.app.data.db.dao.TransactionDao
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易 Repository（核心）
 *
 * 职责：
 *  - CRUD（observeAll / insert / update / softDelete）
 *  - 转账业务（Phase 2 实装完整版）
 *  - 聚合查询（按月/周/日、饼图、趋势）
 *  - 搜索 + 筛选
 *
 * v0.2 升级：type 字段从 String 改为 TransactionType 枚举
 *
 * 详见 docs/DEV_PLAN.md §5 Task 2.3-2.5 / §6 Task 3.2-3.6
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    // ---------- 基础查询 ----------

    /**
     * 观察所有活跃交易（按时间倒序）
     */
    fun observeAllActive(): Flow<List<TransactionEntity>> = transactionDao.observeAllActive()

    /**
     * 获取单笔交易
     */
    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    /**
     * 观察单笔交易（用于详情页实时更新）
     */
    fun observeById(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    /**
     * 按时间区间观察活跃交易
     */
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(start, end)

    // ---------- 聚合 ----------

    /**
     * 区间内支出总额
     */
    fun observeExpenseSum(start: Long, end: Long): Flow<Long> =
        transactionDao.observeExpenseSum(start, end)

    /**
     * 区间内收入总额
     */
    fun observeIncomeSum(start: Long, end: Long): Flow<Long> =
        transactionDao.observeIncomeSum(start, end)

    /**
     * 区间内分类聚合（用于饼图）
     */
    fun observeCategoryBreakdown(start: Long, end: Long): Flow<List<CategoryTotal>> =
        transactionDao.observeCategoryBreakdown(start, end)

    /**
     * 区间内按日聚合（用于趋势图）
     */
    fun observeDailyExpense(
        startDayMillis: Long,
        endDayMillis: Long,
        dayInMillis: Long = 86_400_000L
    ): Flow<List<DailyTotal>> =
        transactionDao.observeDailyExpense(startDayMillis, endDayMillis, dayInMillis)

    // ---------- 筛选 ----------

    /**
     * 按账户观察
     */
    fun observeByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeByAccount(accountId)

    /**
     * 按类别观察
     */
    fun observeByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeByCategory(categoryId)

    // ---------- 回收站 ----------

    /**
     * 观察回收站
     */
    fun observeRecycleBin(): Flow<List<TransactionEntity>> =
        transactionDao.observeRecycleBin()

    // ---------- 写入 ----------

    /**
     * 保存交易（新建或更新）
     *
     * 关键校验（详见 docs/PRD.md §6.1 转账逻辑）：
     *  - 金额必须 > 0
     *  - 收入/支出必须选类别
     *  - TRANSFER 必须有 toAccountId 且 != accountId
     *  - TRANSFER 不应有 categoryId（强制 null）
     *
     * 返回新行的 id（insert 时）或原 id（update 时）
     */
    suspend fun save(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "金额必须大于 0" }
        require(transaction.type in VALID_TYPES) { "type 必须是 INCOME / EXPENSE / TRANSFER" }
        require(transaction.accountId > 0) { "必须指定账户" }

        when (transaction.type) {
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                requireNotNull(transaction.categoryId) { "${transaction.type} 必须指定类别" }
                require(transaction.toAccountId == null) { "${transaction.type} 不应该有转入账户" }
            }
            TransactionType.TRANSFER -> {
                // 转账业务规则（最易错，详见 PRD §6.1）
                requireNotNull(transaction.toAccountId) { "转账必须指定转入账户" }
                require(transaction.toAccountId != transaction.accountId) { "转入账户必须与转出不同" }
                require(transaction.categoryId == null) { "转账不应该有类别" }
            }
        }

        val now = System.currentTimeMillis()
        return if (transaction.id == 0L) {
            transactionDao.insert(
                transaction.copy(
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null
                )
            )
        } else {
            val existing = transactionDao.getById(transaction.id)
                ?: throw IllegalStateException("原交易不存在")
            require(existing.deletedAt == null) { "已删除的记录不可编辑，请先在回收站恢复" }
            transactionDao.update(transaction.copy(updatedAt = now, deletedAt = null))
            transaction.id
        }
    }

    /**
     * 软删除（进回收站）
     */
    suspend fun softDelete(id: Long) {
        val now = System.currentTimeMillis()
        transactionDao.softDelete(id, now)
    }

    /**
     * 从回收站恢复。成功（至少更新 1 行）返回 true。
     */
    suspend fun restore(id: Long): Boolean {
        val now = System.currentTimeMillis()
        return transactionDao.restore(id, now) > 0
    }

    /**
     * 永久删除单笔
     */
    suspend fun hardDelete(id: Long) {
        transactionDao.hardDelete(id)
    }

    /** 清空回收站（单条 SQL） */
    suspend fun emptyBin() {
        transactionDao.hardDeleteAllSoftDeleted()
    }

    /**
     * 30 天过期清理（WorkManager 调用，单条 SQL）
     */
    suspend fun cleanupExpired(thresholdMillis: Long) {
        transactionDao.hardDeleteExpired(thresholdMillis)
    }

    companion object {
        private val VALID_TYPES = setOf(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.TRANSFER
        )
    }
}
