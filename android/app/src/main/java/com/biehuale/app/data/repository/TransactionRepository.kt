package com.biehuale.app.data.repository

import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.dao.TransactionDao
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.util.Money
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易 Repository（核心）
 *
 * 职责：CRUD、转账校验、软删/回收站；账单聚合由 UI 层 BillAggregator 完成。
 *
 * 详见 docs/PRD.md §6
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) {

    fun observeAllActive(): Flow<List<TransactionEntity>> = transactionDao.observeAllActive()

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    fun observeById(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    fun observeRecycleBin(): Flow<List<TransactionEntity>> =
        transactionDao.observeRecycleBin()

    /**
     * 保存交易（新建或更新）
     *
     * 关键校验（详见 docs/PRD.md §6.1 转账逻辑）：
     *  - 金额必须 > 0
     *  - 收入/支出必须选类别
     *  - TRANSFER 必须有 toAccountId 且 != accountId
     *  - TRANSFER 不应有 categoryId（强制 null）
     *  - TRANSFER 手续费 0 ≤ fee < amount；其它类型 fee 强制为 0
     *
     * 返回新行的 id（insert 时）或原 id（update 时）
     */
    suspend fun save(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "金额必须大于 0" }
        require(transaction.amount <= Money.MAX_CENTS) { "金额超出上限" }
        require(transaction.type in VALID_TYPES) { "type 必须是 INCOME / EXPENSE / TRANSFER" }
        require(transaction.accountId > 0) { "必须指定账户" }

        val account = accountDao.getById(transaction.accountId)
            ?: throw IllegalArgumentException("账户不存在")
        require(!account.isArchived) { "账户已归档，请另选" }

        val normalized = when (transaction.type) {
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                val categoryId = requireNotNull(transaction.categoryId) {
                    "${transaction.type} 必须指定类别"
                }
                require(transaction.toAccountId == null) { "${transaction.type} 不应该有转入账户" }
                val category = categoryDao.getById(categoryId)
                    ?: throw IllegalArgumentException("类别不存在")
                require(!category.isArchived) { "类别已归档，请另选" }
                val expected = when (transaction.type) {
                    TransactionType.INCOME -> CategoryType.INCOME
                    else -> CategoryType.EXPENSE
                }
                require(category.type == expected) { "类别类型与交易类型不匹配" }
                transaction.copy(fee = 0L)
            }
            TransactionType.TRANSFER -> {
                val toId = requireNotNull(transaction.toAccountId) { "转账必须指定转入账户" }
                require(toId != transaction.accountId) { "转入账户必须与转出不同" }
                require(transaction.categoryId == null) { "转账不应该有类别" }
                require(transaction.fee >= 0L) { "手续费不能为负" }
                require(transaction.fee < transaction.amount) { "手续费必须小于转账金额" }
                val toAccount = accountDao.getById(toId)
                    ?: throw IllegalArgumentException("转入账户不存在")
                require(!toAccount.isArchived) { "转入账户已归档，请另选" }
                transaction
            }
        }

        val now = System.currentTimeMillis()
        return if (normalized.id == 0L) {
            transactionDao.insert(
                normalized.copy(
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null
                )
            )
        } else {
            val existing = transactionDao.getById(normalized.id)
                ?: throw IllegalStateException("原交易不存在")
            require(existing.deletedAt == null) { "已删除的记录不可编辑，请先在回收站恢复" }
            transactionDao.update(normalized.copy(updatedAt = now, deletedAt = null))
            normalized.id
        }
    }

    suspend fun softDelete(id: Long) {
        val now = System.currentTimeMillis()
        transactionDao.softDelete(id, now)
    }

    /** 从回收站恢复。成功（至少更新 1 行）返回 true。 */
    suspend fun restore(id: Long): Boolean {
        val now = System.currentTimeMillis()
        return transactionDao.restore(id, now) > 0
    }

    suspend fun hardDelete(id: Long): Boolean =
        transactionDao.hardDelete(id) > 0

    suspend fun emptyBin() {
        transactionDao.hardDeleteAllSoftDeleted()
    }

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
