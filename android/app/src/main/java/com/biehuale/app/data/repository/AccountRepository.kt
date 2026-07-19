package com.biehuale.app.data.repository

import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {

    fun observeActive(): Flow<List<AccountEntity>> = accountDao.observeActive()

    fun observeAll(): Flow<List<AccountEntity>> = accountDao.observeAll()

    suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)

    suspend fun countActive(): Int = accountDao.countActive()

    suspend fun getBalance(id: Long): Long? = accountDao.getBalance(id)

    suspend fun create(
        name: String,
        initialBalance: Long = 0L,
        icon: String? = null,
        colorHex: String? = null
    ): Long {
        require(accountDao.findActiveByName(name) == null) { "已存在同名账户" }
        val now = System.currentTimeMillis()
        return accountDao.insert(
            AccountEntity(
                id = 0L,
                name = name,
                icon = icon,
                colorHex = colorHex,
                initialBalance = initialBalance,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun update(account: AccountEntity) {
        val conflict = accountDao.findActiveByName(account.name)
        require(conflict == null || conflict.id == account.id) { "已存在同名账户" }
        accountDao.update(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun archive(id: Long) {
        accountDao.archive(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) {
        val account = accountDao.getById(id) ?: throw IllegalStateException("账户不存在")
        val conflict = accountDao.findActiveByName(account.name)
        require(conflict == null || conflict.id == id) { "已存在同名活跃账户，无法恢复" }
        accountDao.restore(id, System.currentTimeMillis())
    }
}
