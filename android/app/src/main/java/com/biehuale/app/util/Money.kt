package com.biehuale.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * 金额工具（分 ↔ 显示字符串）
 *
 * 详见 docs/PRD.md §6.3
 *  - 内部存储用 Long（分），运算不用 Double
 *  - UI 显示用 BigDecimal / NumberFormat
 */
object Money {

    /** 分：上限 999,999,999.99 元 */
    const val MAX_CENTS: Long = 99_999_999_999L

    private val FORMATTER: NumberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    /**
     * Long (分) → "1.00"（无货币符号）
     */
    fun Long.toMoneyString(): String {
        val bd = BigDecimal.valueOf(this).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return bd.toPlainString()
    }

    /**
     * Long (分) → "¥1.00"
     */
    fun Long.toDisplayString(): String {
        val yuan = BigDecimal.valueOf(this).movePointLeft(2)
        return FORMATTER.format(yuan)
    }

    /**
     * 输入过程中的展示：完整金额补齐两位小数；输入中的 "5." / "5.5" 原样保留。
     */
    fun formatAmountDisplay(raw: String): String {
        if (raw == "0") return "0.00"
        if (raw.endsWith(".")) return raw
        val afterDot = raw.substringAfter('.', missingDelimiterValue = "")
        if (raw.contains('.') && afterDot.length < 2) return raw
        return parseToCents(raw)?.toMoneyString() ?: raw
    }

    /**
     * "5.50" → 550L（分）；非法 / 超上限 → null
     */
    fun parseToCents(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val bd = trimmed.toBigDecimalOrNull() ?: return null
        if (bd.signum() < 0) return null
        if (bd.scale() > 2) return null

        val centsBd = bd.movePointRight(2)
        if (centsBd > BigDecimal.valueOf(MAX_CENTS)) return null
        return centsBd.toLong()
    }
}
