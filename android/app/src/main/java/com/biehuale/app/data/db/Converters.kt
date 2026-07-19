package com.biehuale.app.data.db

import androidx.room.TypeConverter
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType

/**
 * Room TypeConverters
 *
 * 详见 docs/PRD.md §5.3
 *
 * 把 TransactionType / CategoryType 枚举与 String 互转，DB schema 仍是 TEXT（保持兼容）。
 *
 * 重要：写入时如果遇到 null 或未知值，按"安全降级"处理：
 *  - enum -> String：直接 .name（"INCOME" 等）
 *  - String -> enum：未识别返回 null（让上层决定是抛错还是 fallback）
 */
class Converters {

    // ---------- TransactionType ----------

    @TypeConverter
    fun transactionTypeToString(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun stringToTransactionType(value: String?): TransactionType? =
        TransactionType.fromOrNull(value)

    // ---------- CategoryType ----------

    @TypeConverter
    fun categoryTypeToString(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? =
        CategoryType.fromOrNull(value)
}
