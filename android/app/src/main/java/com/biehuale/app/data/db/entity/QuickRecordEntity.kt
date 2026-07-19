package com.biehuale.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 快速记账模板（支出一键落账）
 *
 * 表名：quick_records
 * 金额单位：分（Long）
 */
@Entity(
    tableName = "quick_records",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sort_order"]),
        Index(value = ["category_id"]),
        Index(value = ["account_id"])
    ]
)
data class QuickRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    /** 金额（分，必须 > 0） */
    val amount: Long,

    /** 说明（≤200 字符，可空） */
    val description: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
