package com.biehuale.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * DateExt 工具类测试
 *
 * 详见 docs/DEV_PLAN.md §8 Task 5.5
 */
class DateExtTest {

    // ---------- monthRange ----------

    @Test
    fun `monthRange 2026-01 是 1 月 1 号到 2 月 1 号`() {
        val (start, end) = DateExt.monthRange(2026, 1)
        // 2026-01-01 00:00:00 UTC+8 = 1767225600000 (实际值由时区决定)
        // 不强断言具体 millis，断言 start < end 且时长 ≈ 31 天
        val days = (end - start) / (24L * 60 * 60 * 1000)
        assertThat(days).isEqualTo(31L)
    }

    @Test
    fun `monthRange 2026-02 是 28 天（平年）`() {
        val (start, end) = DateExt.monthRange(2026, 2)
        val days = (end - start) / (24L * 60 * 60 * 1000)
        assertThat(days).isEqualTo(28L)
    }

    @Test
    fun `monthRange 2024-02 是 29 天（闰年）`() {
        val (start, end) = DateExt.monthRange(2024, 2)
        val days = (end - start) / (24L * 60 * 60 * 1000)
        assertThat(days).isEqualTo(29L)
    }

    @Test
    fun `monthRange 12 月跨年`() {
        val (start, end) = DateExt.monthRange(2026, 12)
        // 12 月到次年 1 月
        val days = (end - start) / (24L * 60 * 60 * 1000)
        assertThat(days).isEqualTo(31L)
    }

    @Test
    fun `monthRange 连续 12 个月总长 365 或 366 天`() {
        val totalDays = (1..12).sumOf { month ->
            val (s, e) = DateExt.monthRange(2026, month)
            (e - s) / (24L * 60 * 60 * 1000)
        }
        // 2026 是平年
        assertThat(totalDays).isEqualTo(365L)
    }

    // ---------- nowMillis ----------

    @Test
    fun `nowMillis 在合理范围`() {
        val now = DateExt.nowMillis()
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        val oneYearLater = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
        assertThat(now).isAtLeast(oneYearAgo)
        assertThat(now).isAtMost(oneYearLater)
    }
}
