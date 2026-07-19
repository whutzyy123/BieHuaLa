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
    const val QUICK_RECORD_MANAGE = "quick-record-manage"
    const val RECYCLE_BIN = "recycle-bin"

    /**
     * 全部流水。
     * categoryId=-1 无预选类别；rangeStart/rangeEnd=-1 不限日期（半开区间 [start,end)）。
     */
    const val ALL_TRANSACTIONS =
        "all-transactions?categoryId={categoryId}&rangeStart={rangeStart}&rangeEnd={rangeEnd}"

    fun allTransactions(
        categoryId: Long? = null,
        rangeStart: Long? = null,
        rangeEndExclusive: Long? = null
    ): String {
        val cat = categoryId ?: NO_CATEGORY
        val start = rangeStart ?: NO_RANGE
        val end = rangeEndExclusive ?: NO_RANGE
        return "all-transactions?categoryId=$cat&rangeStart=$start&rangeEnd=$end"
    }

    const val TRANSACTION_DETAIL = "transaction-detail/{transactionId}"
    fun transactionDetail(id: Long) = "transaction-detail/$id"

    const val RECORD_EDIT = "record-edit/{transactionId}"
    fun recordEdit(id: Long) = "record-edit/$id"

    const val ARG_TRANSACTION_ID = "transactionId"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_RANGE_START = "rangeStart"
    const val ARG_RANGE_END = "rangeEnd"

    /** 路由占位：无类别预筛选 / 无日期区间 */
    const val NO_CATEGORY = -1L
    const val NO_RANGE = -1L
}
