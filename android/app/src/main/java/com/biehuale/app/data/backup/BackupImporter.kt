package com.biehuale.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.dao.AccountDao
import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.dao.TransactionDao
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份导入器 — 合并模式 + 整笔事务
 *
 * 详见 docs/PRD.md §10.1 / §10.2
 *
 * v0.2：DTO 字段保持 String（与历史 v1 JSON 兼容），导入时再转 enum 写入 entity。
 *
 * Merge 交易语义（fingerprint 不含 deletedAt）：
 * - 无匹配 → insert（保留备份 deletedAt）
 * - 本地软删 + 备份 active → restore
 * - 本地软删 + 备份软删 → skip
 * - 本地活跃 + 备份 active → skip
 * - 本地活跃 + 备份软删 → softDelete 本地
 *
 * 同名账户：更新 icon/color/归档；仅当本地 initialBalance==0 时采用备份期初。
 */
@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    suspend fun preview(uri: Uri): Result<ImportPreview> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("无法打开 Uri 输入流：$uri")
        val backup = parseBackupJson(raw)
        ImportPreview(
            backup = backup,
            accountsCount = backup.accounts.size,
            categoriesCount = backup.categories.size,
            transactionsCount = backup.transactions.size
        )
    }

    fun parseBackupJson(raw: String): BackupDto {
        if (raw.isBlank()) {
            throw IllegalArgumentException("空文件")
        }
        val backup = try {
            json.decodeFromString(BackupDto.serializer(), raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("JSON 解析失败：${e.message}", e)
        }
        validateSchema(backup)
        return backup
    }

    suspend fun applyImport(preview: ImportPreview): Result<ImportResult> =
        applyImport(preview.backup)

    suspend fun applyImport(backup: BackupDto): Result<ImportResult> = runCatching {
        validateSchema(backup)
        database.withTransaction {
            mergeBackup(backup)
        }
    }

    private fun validateSchema(backup: BackupDto) {
        if (backup.schemaVersion < BackupSchema.SUPPORTED_MIN_VERSION) {
            throw IllegalStateException(
                "备份 schemaVersion=${backup.schemaVersion} 过低，最小支持 ${BackupSchema.SUPPORTED_MIN_VERSION}"
            )
        }
        if (backup.schemaVersion > BackupSchema.CURRENT_VERSION) {
            throw IllegalStateException(
                "备份 schemaVersion=${backup.schemaVersion} 高于当前 App 支持的 ${BackupSchema.CURRENT_VERSION}，请升级 App"
            )
        }
    }

    private suspend fun mergeBackup(backup: BackupDto): ImportResult {
        val now = System.currentTimeMillis()
        val accountIdMap = mutableMapOf<Long, Long>()
        val categoryIdMap = mutableMapOf<Long, Long>()

        val existingAccounts = accountDao.observeAll().first().toMutableList()
        var accountsInserted = 0
        for (accDto in backup.accounts) {
            val existing = existingAccounts.firstOrNull { it.name == accDto.name }
            val newId = if (existing != null) {
                // 期初：仅本地为 0 时采用备份（覆盖种子现金=0）；本地已非 0 则保留
                val mergedInitial =
                    if (existing.initialBalance == 0L) accDto.initialBalance else existing.initialBalance
                if (existing.initialBalance != mergedInitial ||
                    existing.isArchived != accDto.isArchived ||
                    existing.icon != accDto.icon ||
                    existing.colorHex != accDto.color
                ) {
                    val updated = existing.copy(
                        initialBalance = mergedInitial,
                        isArchived = accDto.isArchived,
                        icon = accDto.icon,
                        colorHex = accDto.color,
                        updatedAt = now
                    )
                    accountDao.update(updated)
                    val idx = existingAccounts.indexOfFirst { it.id == existing.id }
                    if (idx >= 0) existingAccounts[idx] = updated
                }
                existing.id
            } else {
                accountsInserted++
                val id = accountDao.insert(
                    AccountEntity(
                        id = 0L,
                        name = accDto.name,
                        icon = accDto.icon,
                        colorHex = accDto.color,
                        initialBalance = accDto.initialBalance,
                        isArchived = accDto.isArchived,
                        createdAt = accDto.createdAt.takeIf { it > 0 } ?: now,
                        updatedAt = now
                    )
                )
                existingAccounts.add(
                    AccountEntity(
                        id = id,
                        name = accDto.name,
                        icon = accDto.icon,
                        colorHex = accDto.color,
                        initialBalance = accDto.initialBalance,
                        isArchived = accDto.isArchived,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                id
            }
            accountIdMap[accDto.id] = newId
        }

        val existingCategories = categoryDao.observeAll().first().toMutableList()
        var categoriesInserted = 0
        var categoriesSkipped = 0
        for (catDto in backup.categories) {
            val catType = CategoryType.fromOrNull(catDto.type)
            if (catType == null) {
                // 非法 type：不建映射，依赖它的收支会因缺 category 被 skip
                categoriesSkipped++
                continue
            }
            val existing = existingCategories.firstOrNull {
                it.name == catDto.name && it.type == catType
            }
            val newId = if (existing != null) {
                existing.id
            } else {
                categoriesInserted++
                val id = categoryDao.insert(
                    CategoryEntity(
                        id = 0L,
                        name = catDto.name,
                        icon = catDto.icon,
                        colorHex = catDto.color,
                        type = catType,
                        isBuiltin = catDto.isBuiltin,
                        sortOrder = catDto.sortOrder,
                        isArchived = catDto.isArchived,
                        createdAt = catDto.createdAt.takeIf { it > 0 } ?: now,
                        updatedAt = now
                    )
                )
                existingCategories.add(
                    CategoryEntity(
                        id = id,
                        name = catDto.name,
                        icon = catDto.icon,
                        colorHex = catDto.color,
                        type = catType,
                        isBuiltin = catDto.isBuiltin,
                        sortOrder = catDto.sortOrder,
                        isArchived = catDto.isArchived,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                id
            }
            categoryIdMap[catDto.id] = newId
        }

        // fingerprint → 本地行；同指纹优先保留软删行，便于 restore
        val existingByFp = linkedMapOf<String, TransactionEntity>()
        for (tx in transactionDao.getAllIncludingDeleted()) {
            val fp = fingerprint(tx)
            val prev = existingByFp[fp]
            if (prev == null || (prev.deletedAt == null && tx.deletedAt != null)) {
                existingByFp[fp] = tx
            }
        }

        val toInsert = mutableListOf<TransactionEntity>()
        var skipped = 0
        var restored = 0
        var softDeleted = 0

        for (txDto in backup.transactions) {
            val txType = TransactionType.fromOrNull(txDto.type)
            if (txType == null) {
                skipped++
                continue
            }
            val newAccountId = accountIdMap[txDto.accountId]
            if (newAccountId == null) {
                skipped++
                continue
            }
            val newToAccountId = txDto.toAccountId?.let { accountIdMap[it] }
            // 备份里写了 categoryId 但映射不到（含非法类别）→ null
            val mappedCategoryId = txDto.categoryId?.let { categoryIdMap[it] }
            // TRANSFER 强制无类别；收支必须有可映射类别
            val newCategoryId = when (txType) {
                TransactionType.TRANSFER -> null
                TransactionType.EXPENSE, TransactionType.INCOME -> mappedCategoryId
            }

            if (!isValidTransaction(
                    type = txType,
                    amount = txDto.amount,
                    mappedAccountId = newAccountId,
                    mappedToAccountId = newToAccountId,
                    mappedCategoryId = newCategoryId
                )
            ) {
                skipped++
                continue
            }

            val entity = TransactionEntity(
                id = 0L,
                amount = txDto.amount,
                type = txType,
                categoryId = newCategoryId,
                accountId = newAccountId,
                toAccountId = if (txType == TransactionType.TRANSFER) newToAccountId else null,
                description = txDto.description,
                occurredAt = txDto.occurredAt,
                createdAt = txDto.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
                deletedAt = txDto.deletedAt
            )
            val fp = fingerprint(entity)
            val existing = existingByFp[fp]
            if (existing != null) {
                val backupSoft = txDto.deletedAt != null
                val localSoft = existing.deletedAt != null
                when {
                    localSoft && !backupSoft -> {
                        transactionDao.restore(existing.id, now)
                        existingByFp[fp] = existing.copy(deletedAt = null, updatedAt = now)
                        restored++
                    }
                    !localSoft && backupSoft -> {
                        transactionDao.softDelete(existing.id, now)
                        existingByFp[fp] = existing.copy(deletedAt = now, updatedAt = now)
                        softDeleted++
                    }
                    else -> skipped++ // 双方同为活跃或同为软删
                }
                continue
            }
            existingByFp[fp] = entity
            toInsert.add(entity)
        }

        if (toInsert.isNotEmpty()) {
            transactionDao.insertAll(toInsert)
        }

        return ImportResult(
            accountsInserted = accountsInserted,
            categoriesInserted = categoriesInserted,
            categoriesSkipped = categoriesSkipped,
            transactionsInserted = toInsert.size,
            transactionsRestored = restored,
            transactionsSoftDeleted = softDeleted,
            transactionsSkipped = skipped
        )
    }

    /**
     * 与 [com.biehuale.app.data.repository.TransactionRepository.save] 对齐。
     */
    private fun isValidTransaction(
        type: TransactionType,
        amount: Long,
        mappedAccountId: Long,
        mappedToAccountId: Long?,
        mappedCategoryId: Long?
    ): Boolean {
        if (amount <= 0L) return false
        return when (type) {
            TransactionType.EXPENSE, TransactionType.INCOME ->
                mappedCategoryId != null
            TransactionType.TRANSFER ->
                mappedToAccountId != null && mappedToAccountId != mappedAccountId
        }
    }

    companion object {
        /** 内容指纹；不含 deletedAt，使软删后重导可命中并 restore。 */
        fun fingerprint(tx: TransactionEntity): String =
            listOf(
                tx.amount.toString(),
                tx.type.name,
                tx.accountId.toString(),
                (tx.toAccountId ?: -1L).toString(),
                (tx.categoryId ?: -1L).toString(),
                tx.occurredAt.toString(),
                tx.description.orEmpty()
            ).joinToString("|")
    }
}

data class ImportPreview(
    val backup: BackupDto,
    val accountsCount: Int,
    val categoriesCount: Int,
    val transactionsCount: Int
)
