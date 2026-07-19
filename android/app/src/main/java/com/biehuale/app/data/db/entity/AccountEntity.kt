package com.biehuale.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账户表 - Entity
 *
 * 表名：accounts
 * 字段详见 docs/PRD.md §5.2
 *
 * 索引：
 *  - `is_archived`：按状态查询（活跃 vs 归档）
 *  - PK `id` 自带索引
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["is_archived"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 账户名（≤20 字） */
    val name: String,

    /** 图标 key（如 "wallet", "wechat", "alipay"） */
    val icon: String? = null,

    @ColumnInfo(name = "color")
    val colorHex: String? = null,

    /** 初始余额（分，Long 永不用 Double） */
    @ColumnInfo(name = "initial_balance")
    val initialBalance: Long = 0L,

    /** 软删除标记：0=活跃，1=归档 */
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
