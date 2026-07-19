package com.biehuale.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.biehuale.app.BuildConfig
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份导出器 — 完整快照（含归档账户/类别、软删流水）
 *
 * 详见 docs/PRD.md §10.1
 *
 * v0.2：Entity 字段已升级为 enum，导出时用 .name 转 String（与 v1 JSON 格式兼容）
 */
@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(uri: Uri): Result<Unit> = runCatching {
        val backup = database.withTransaction {
            val accounts = accountRepository.observeAll().first()
            val categories = categoryRepository.observeAll().first()
            val transactions = transactionRepository.observeAllActive().first()
            val recycled = transactionRepository.observeRecycleBin().first()
            BackupDto(
                schemaVersion = BackupSchema.CURRENT_VERSION,
                appVersion = BuildConfig.VERSION_NAME,
                exportedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                accounts = accounts.map { it.toDto() },
                categories = categories.map { it.toDto() },
                transactions = (transactions + recycled).distinctBy { it.id }.map { it.toDto() }
            )
        }

        val jsonString = json.encodeToString(backup)
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(jsonString.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: throw IllegalStateException("无法打开 Uri 输出流：$uri")
    }

    fun generateDefaultFileName(): String {
        val now = java.time.LocalDateTime.now()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")
        return "别花乐_${now.format(fmt)}.json"
    }
}

fun AccountEntity.toDto() = AccountDto(
    id = id,
    name = name,
    icon = icon,
    color = colorHex,
    initialBalance = initialBalance,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CategoryEntity.toDto() = CategoryDto(
    id = id,
    name = name,
    icon = icon,
    color = colorHex,
    type = type.name,  // enum -> String
    isBuiltin = isBuiltin,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TransactionEntity.toDto() = TransactionDto(
    id = id,
    amount = amount,
    type = type.name,  // enum -> String
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    description = description,
    occurredAt = occurredAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
