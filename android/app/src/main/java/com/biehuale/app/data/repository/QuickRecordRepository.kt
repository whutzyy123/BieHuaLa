package com.biehuale.app.data.repository

import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.dao.QuickRecordDao
import com.biehuale.app.data.db.entity.QuickRecordEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.util.Money
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickRecordRepository @Inject constructor(
    private val quickRecordDao: QuickRecordDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao
) {

    fun observeAll(): Flow<List<QuickRecordEntity>> = quickRecordDao.observeAll()

    suspend fun getById(id: Long): QuickRecordEntity? = quickRecordDao.getById(id)

    suspend fun create(
        categoryId: Long,
        accountId: Long,
        amountCents: Long,
        description: String?
    ): Long {
        validate(categoryId, accountId, amountCents, description)
        val now = System.currentTimeMillis()
        val sort = quickRecordDao.maxSortOrder() + 1
        return quickRecordDao.insert(
            QuickRecordEntity(
                id = 0L,
                categoryId = categoryId,
                accountId = accountId,
                amount = amountCents,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                sortOrder = sort,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun update(
        id: Long,
        categoryId: Long,
        accountId: Long,
        amountCents: Long,
        description: String?
    ) {
        val existing = quickRecordDao.getById(id)
            ?: throw IllegalArgumentException("快速记账不存在")
        validate(categoryId, accountId, amountCents, description)
        quickRecordDao.update(
            existing.copy(
                categoryId = categoryId,
                accountId = accountId,
                amount = amountCents,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: Long) {
        quickRecordDao.deleteById(id)
    }

    /** 类别归档后清理依赖模板，避免下拉出现失效项 */
    suspend fun deleteByCategoryId(categoryId: Long) {
        quickRecordDao.deleteByCategoryId(categoryId)
    }

    /** 账户归档后清理依赖模板 */
    suspend fun deleteByAccountId(accountId: Long) {
        quickRecordDao.deleteByAccountId(accountId)
    }

    private suspend fun validate(
        categoryId: Long,
        accountId: Long,
        amountCents: Long,
        description: String?
    ) {
        require(amountCents in 1..Money.MAX_CENTS) { "请输入有效金额" }
        val desc = description?.trim().orEmpty()
        require(desc.length <= 200) { "说明最多 200 字" }
        val category = categoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("类别不存在")
        require(!category.isArchived) { "类别已归档，请另选" }
        require(category.type == CategoryType.EXPENSE) { "快速记账仅支持支出类别" }
        val account = accountDao.getById(accountId)
            ?: throw IllegalArgumentException("账户不存在")
        require(!account.isArchived) { "账户已归档，请另选" }
    }
}
