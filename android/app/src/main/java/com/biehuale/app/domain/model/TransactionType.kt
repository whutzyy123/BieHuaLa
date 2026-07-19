package com.biehuale.app.domain.model

/**
 * 交易类型枚举
 *
 * 详见 docs/PRD.md §5.3
 *
 * - INCOME / EXPENSE / TRANSFER
 * - 持久化通过 Room TypeConverter 转为 String（保持 schema 兼容）
 * - JSON 备份通过 kotlinx.serialization 序列化为 enum name
 *
 * 重要：枚举值的 name 必须与历史 JSON 备份中的字符串完全一致（"INCOME" 等）
 */
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER;

    companion object {
        /** 安全解析：未知值返回 null（不抛） */
        fun fromOrNull(value: String?): TransactionType? = when (value) {
            "INCOME" -> INCOME
            "EXPENSE" -> EXPENSE
            "TRANSFER" -> TRANSFER
            else -> null
        }
    }
}
