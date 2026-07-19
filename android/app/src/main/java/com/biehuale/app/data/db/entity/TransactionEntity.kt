package com.biehuale.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.biehuale.app.domain.model.TransactionType

/**
 * 交易表 - Entity（核心表）
 *
 * 表名：transactions
 * 字段、外键、索引详见 docs/PRD.md §5.2
 *
 * 外键策略：
 *  - category_id     → categories.id  ON DELETE SET NULL   （类别归档不影响历史账）
 *  - account_id      → accounts.id    ON DELETE RESTRICT   （账户必须先归档+迁移）
 *  - to_account_id   → accounts.id    ON DELETE RESTRICT
 *
 * 索引：
 *  - `occurred_at DESC`：列表/分页查询
 *  - `(account_id, occurred_at)`：账户维度
 *  - `(category_id, occurred_at)`：类别维度
 *  - `deleted_at`：回收站清理
 *
 * v0.2 升级：type 字段从 String 改为 TransactionType 枚举
 *  - DB schema 不变（仍存 TEXT），靠 Converters 互转
 *  - 历史数据自动兼容（"INCOME" 等字符串继续可用）
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["occurred_at"]),
        Index(value = ["account_id", "occurred_at"]),
        Index(value = ["category_id", "occurred_at"]),
        Index(value = ["deleted_at"]),
        Index(value = ["to_account_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 金额（分，Long 永不用 Double；正数由 type 决定正负） */
    val amount: Long,

    /** INCOME / EXPENSE / TRANSFER */
    val type: TransactionType,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "to_account_id")
    val toAccountId: Long? = null,

    /** 说明（≤200 字符，可空） */
    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    /** 软删除时间戳；null = 未删除 */
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
