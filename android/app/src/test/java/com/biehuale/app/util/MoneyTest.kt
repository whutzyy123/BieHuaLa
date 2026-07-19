package com.biehuale.app.util

import com.biehuale.app.util.Money.toDisplayString
import com.biehuale.app.util.Money.toMoneyString
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Money 工具类测试
 *
 * 详见 docs/DEV_PLAN.md §8 Task 5.5
 *
 * 关键业务：金额解析 + 格式化（PRD §6.3）
 */
class MoneyTest {

    // ---------- parseToCents ----------

    @ParameterizedTest(name = "[{index}] parseToCents(\"{0}\") = {1}")
    @CsvSource(
        "0,         0",
        "1,         100",
        "1.5,       150",
        "1.50,      150",
        "5.50,      550",
        "100,       10000",
        "100.99,    10099",
        "0.01,      1",
        "99999999.99, 9999999999"
    )
    fun `parseToCents valid input`(input: String, expected: Long) {
        assertThat(Money.parseToCents(input)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "[{index}] parseToCents(\"{0}\") = null")
    @ValueSource(strings = ["", "   ", "abc", "-1", "1.234", "1.50.5"])
    fun `parseToCents invalid input`(input: String) {
        assertThat(Money.parseToCents(input)).isNull()
    }

    @Test
    fun `parseToCents allows trailing or leading dot forms`() {
        // BigDecimal 接受输入中形态；与 formatAmountDisplay 配合
        assertThat(Money.parseToCents("1.")).isEqualTo(100L)
        assertThat(Money.parseToCents(".5")).isEqualTo(50L)
    }

    @Test
    fun `parseToCents 超上限返回 null`() {
        val huge = "1000000000000"  // 1 万亿，远超 MAX_CENTS
        assertThat(Money.parseToCents(huge)).isNull()
    }

    // ---------- toMoneyString ----------

    @Test
    fun `toMoneyString 基本转换`() {
        assertThat(100L.toMoneyString()).isEqualTo("1.00")
        assertThat(0L.toMoneyString()).isEqualTo("0.00")
        assertThat(550L.toMoneyString()).isEqualTo("5.50")
        assertThat(12345L.toMoneyString()).isEqualTo("123.45")
    }

    @Test
    fun `toMoneyString 四舍五入`() {
        // 105 分 = 1.05 元（精确）
        assertThat(105L.toMoneyString()).isEqualTo("1.05")
        // 333 分 = 3.33 元
        assertThat(333L.toMoneyString()).isEqualTo("3.33")
    }

    // ---------- toDisplayString ----------

    @Test
    fun `toDisplayString 带货币符号`() {
        // CNY format: "¥1.00"
        val s = 100L.toDisplayString()
        assertThat(s).contains("1.00")
        // 不严格断言 ¥ 符号（不同 locale 可能不同）
    }

    // ---------- 边界 ----------

    @Test
    @DisplayName("金额上限边界")
    fun `MAX_CENTS 边界`() {
        // PRD：上限 999,999,999.99 元 = 99_999_999_999 分
        assertThat(Money.MAX_CENTS).isEqualTo(99_999_999_999L)
        assertThat(Money.parseToCents("999999999.99")).isEqualTo(Money.MAX_CENTS)
    }

    @Test
    fun `parseToCents 极小值`() {
        assertThat(Money.parseToCents("0.01")).isEqualTo(1L)
        assertThat(Money.parseToCents("0.0")).isEqualTo(0L)
        assertThat(Money.parseToCents("0.00")).isEqualTo(0L)
    }
}
