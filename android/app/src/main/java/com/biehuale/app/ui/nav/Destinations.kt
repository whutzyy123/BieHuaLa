package com.biehuale.app.ui.nav

/**
 * 别花乐 (BieHuaLe) - 路由常量 + 参数 key
 *
 * 集中管理所有 destination 路径，避免散落字符串。
 *
 * 路由层级：
 *  - 3 个 Tab（顶层）：bill / record / settings
 *  - 设置 Tab 子页：account-manage / category-manage
 *  - 流水详情（账单 Tab 弹出）：transaction-detail/{id}
 *  - 编辑（账单 Tab 弹出）：record-edit/{id}
 */
object Destinations {
    // 顶层 Tab
    const val BILL = "bill"
    const val RECORD = "record"
    const val SETTINGS = "settings"

    // 设置子页
    const val ACCOUNT_MANAGE = "account-manage"
    const val CATEGORY_MANAGE = "category-manage"
    const val RECYCLE_BIN = "recycle-bin"

    // 全部流水页（账单 Tab「查看全部」进入）
    const val ALL_TRANSACTIONS = "all-transactions"

    // 流水详情
    const val TRANSACTION_DETAIL = "transaction-detail/{transactionId}"
    fun transactionDetail(id: Long) = "transaction-detail/$id"

    // 编辑模式（复用 RecordScreen，传 transactionId）
    const val RECORD_EDIT = "record-edit/{transactionId}"
    fun recordEdit(id: Long) = "record-edit/$id"

    // 参数 keys
    const val ARG_TRANSACTION_ID = "transactionId"
}
