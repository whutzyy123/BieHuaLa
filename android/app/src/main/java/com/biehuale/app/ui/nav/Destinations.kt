package com.biehuale.app.ui.nav

/**
 * 别花乐 (BieHuaLe) - 路由常量 + 参数 key
 */
object Destinations {
    const val BILL = "bill"
    const val RECORD = "record"
    const val SETTINGS = "settings"

    const val ACCOUNT_MANAGE = "account-manage"
    const val CATEGORY_MANAGE = "category-manage"
    const val RECYCLE_BIN = "recycle-bin"

    /** 全部流水；categoryId=-1 表示无预选类别 */
    const val ALL_TRANSACTIONS = "all-transactions?categoryId={categoryId}"
    fun allTransactions(categoryId: Long? = null): String =
        "all-transactions?categoryId=${categoryId ?: NO_CATEGORY}"

    const val TRANSACTION_DETAIL = "transaction-detail/{transactionId}"
    fun transactionDetail(id: Long) = "transaction-detail/$id"

    const val RECORD_EDIT = "record-edit/{transactionId}"
    fun recordEdit(id: Long) = "record-edit/$id"

    const val ARG_TRANSACTION_ID = "transactionId"
    const val ARG_CATEGORY_ID = "categoryId"

    /** 路由占位：无类别预筛选 */
    const val NO_CATEGORY = -1L
}
