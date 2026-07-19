package com.biehuale.app.data.backup

import kotlinx.serialization.Serializable

/**
 * 别花乐 (BieHuaLe) - 备份 JSON Schema
 *
 * 详见 docs/PRD.md §10.1
 *
 * Schema 版本演进：
 *  - v1：初始 schema（type 为 String）
 *  - v2：TransactionDto.fee 转账手续费（分）；缺省 0，旧备份仍可解析
 *  - 删/重命名字段：禁止 — 必须新增 + 迁移
 *
 * 重要：DTO 层的 `type` 保持 String（与历史 v1 备份兼容），
 * 仅在 import / export 时与 domain enum 互转（toDto / fromDto 转换函数）。
 */
object BackupSchema {
    const val CURRENT_VERSION = 2
    const val SUPPORTED_MIN_VERSION = 1  // 拒绝 schemaVersion < 此值的备份
}

@Serializable
data class BackupDto(
    val schemaVersion: Int = BackupSchema.CURRENT_VERSION,
    val appVersion: String,
    val exportedAt: String,  // ISO 8601 UTC
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class AccountDto(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val initialBalance: Long = 0L,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    /** "INCOME" / "EXPENSE" */
    val type: String,
    val isBuiltin: Boolean = false,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class TransactionDto(
    val id: Long,
    val amount: Long,                    // 分
    /** "INCOME" / "EXPENSE" / "TRANSFER" */
    val type: String,
    val categoryId: Long? = null,
    val accountId: Long,
    val toAccountId: Long? = null,
    /** 转账手续费（分）；旧备份缺省为 0 */
    val fee: Long = 0L,
    val description: String? = null,
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

/**
 * 导入结果统计
 */
@Serializable
data class ImportResult(
    val accountsInserted: Int = 0,
    val categoriesInserted: Int = 0,
    /** 非法类别 type 等未建映射的类别数 */
    val categoriesSkipped: Int = 0,
    val transactionsInserted: Int = 0,
    /** 本地软删 + 备份 active → 已恢复 */
    val transactionsRestored: Int = 0,
    /** 本地活跃 + 备份软删 → 已同步软删 */
    val transactionsSoftDeleted: Int = 0,
    /** 校验失败、无法映射或双方状态一致而跳过的交易数 */
    val transactionsSkipped: Int = 0
)
