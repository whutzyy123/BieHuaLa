package com.biehuale.app.domain.model

/**
 * 类别类型枚举
 *
 * 详见 docs/PRD.md §5.3
 *
 * - INCOME / EXPENSE
 * - 持久化通过 Room TypeConverter 转为 String（保持 schema 兼容）
 * - JSON 备份通过 kotlinx.serialization 序列化为 enum name
 */
enum class CategoryType {
    INCOME,
    EXPENSE;

    companion object {
        fun fromOrNull(value: String?): CategoryType? = when (value) {
            "INCOME" -> INCOME
            "EXPENSE" -> EXPENSE
            else -> null
        }
    }
}
