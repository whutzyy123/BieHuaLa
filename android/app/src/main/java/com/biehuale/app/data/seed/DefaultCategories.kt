package com.biehuale.app.data.seed

import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.domain.model.CategoryType

/**
 * 内置类别 seed 数据
 *
 * 详见：
 *  - docs/PRD.md §5.2
 *  - docs/DEV_PLAN.md §4 Task 1.6
 *
 * 预置 15 个类别（10 支出 + 5 收入），首次启动时插入。
 *
 * 注意：
 *  - id = 0（让 autoGenerate 工作）
 *  - createdAt / updatedAt = 0L（callback 注入时由 Callback.onCreate 用真实时间覆盖）
 *  - isArchived = false
 *  - isBuiltin = true（用户不能硬删，只能归档或"重置默认"）
 */
object DefaultCategories {

    /**
     * 支出类别（10 个）
     */
    private val EXPENSE_CATEGORIES: List<CategoryEntity> = listOf(
        category(name = "餐饮", icon = "restaurant", colorHex = "#FF5722", type = CategoryType.EXPENSE, sortOrder = 1),
        category(name = "交通", icon = "directions_bus", colorHex = "#2196F3", type = CategoryType.EXPENSE, sortOrder = 2),
        category(name = "购物", icon = "shopping_cart", colorHex = "#9C27B0", type = CategoryType.EXPENSE, sortOrder = 3),
        category(name = "住房", icon = "home", colorHex = "#795548", type = CategoryType.EXPENSE, sortOrder = 4),
        category(name = "娱乐", icon = "sports_esports", colorHex = "#E91E63", type = CategoryType.EXPENSE, sortOrder = 5),
        category(name = "医疗", icon = "medical_services", colorHex = "#F44336", type = CategoryType.EXPENSE, sortOrder = 6),
        category(name = "教育", icon = "school", colorHex = "#3F51B5", type = CategoryType.EXPENSE, sortOrder = 7),
        category(name = "通讯", icon = "phone", colorHex = "#00BCD4", type = CategoryType.EXPENSE, sortOrder = 8),
        category(name = "护肤", icon = "spa", colorHex = "#FF4081", type = CategoryType.EXPENSE, sortOrder = 9),
        category(name = "其他", icon = "more_horiz", colorHex = "#9E9E9E", type = CategoryType.EXPENSE, sortOrder = 99)
    )

    /**
     * 收入类别（5 个）
     */
    private val INCOME_CATEGORIES: List<CategoryEntity> = listOf(
        category(name = "工资", icon = "payments", colorHex = "#4CAF50", type = CategoryType.INCOME, sortOrder = 1),
        category(name = "奖金", icon = "redeem", colorHex = "#8BC34A", type = CategoryType.INCOME, sortOrder = 2),
        category(name = "理财", icon = "trending_up", colorHex = "#009688", type = CategoryType.INCOME, sortOrder = 3),
        category(name = "零钱", icon = "savings", colorHex = "#66BB6A", type = CategoryType.INCOME, sortOrder = 4),
        category(name = "其他", icon = "more_horiz", colorHex = "#9E9E9E", type = CategoryType.INCOME, sortOrder = 99)
    )

    /**
     * 全部 seed 类别
     */
    val ALL: List<CategoryEntity> = EXPENSE_CATEGORIES + INCOME_CATEGORIES

    private fun category(
        name: String,
        icon: String,
        colorHex: String,
        type: CategoryType,
        sortOrder: Int
    ): CategoryEntity = CategoryEntity(
        id = 0L,
        name = name,
        icon = icon,
        colorHex = colorHex,
        type = type,
        isBuiltin = true,
        sortOrder = sortOrder,
        isArchived = false,
        createdAt = 0L,  // 由 Callback.onCreate 用真实时间覆盖
        updatedAt = 0L
    )
}
