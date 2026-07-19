package com.biehuale.app.data.db.dao

/**
 * 账户余额聚合行（Room 投影）
 */
data class AccountBalanceRow(
    val accountId: Long,
    val balance: Long
)
