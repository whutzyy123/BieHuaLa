package com.biehuale.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.biehuale.app.domain.model.CategoryType

/**
 * 类别表 - Entity
 *
 * 表名：categories
 * 字段详见 docs/PRD.md §5.2
 *
 * 索引：
 *  - `(type, is_archived)`：按类型查活跃类别（最常见查询）
 *  - `is_builtin`：重置默认类别时用
 *
 * v0.2 升级：type 字段从 String 改为 CategoryType 枚举
 *  - DB schema 不变（仍存 TEXT），靠 Converters 互转
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["type", "is_archived"]),
        Index(value = ["is_builtin"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 类别名（≤10 字） */
    val name: String,

    /** 图标 key */
    val icon: String? = null,

    @ColumnInfo(name = "color")
    val colorHex: String? = null,

    /** INCOME / EXPENSE */
    val type: CategoryType,

    /** 内置不可硬删（用户可"归档"或"重置默认"） */
    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean = false,

    /** 显示顺序 */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** 软删除标记：0=活跃，1=归档 */
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
