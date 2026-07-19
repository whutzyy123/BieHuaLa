package com.biehuale.app.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间工具
 *
 * 内部存储用 epoch millis (Long)，显示时格式化。
 *
 * 详见 docs/PRD.md §6.3
 *
 * 用法：
 *  - `now.toDateString()` = "08-12"
 *  - `now.toDateTimeString()` = "08-12 08:30"
 *  - `now.toTimeString()` = "08:30"
 *  - `monthRange(2026, 8)` = (startMillis, endMillisOfNextMonth)
 */
object DateExt {

    private val zone: TimeZone get() = TimeZone.currentSystemDefault()

    private val DATE_FMT = ThreadLocal.withInitial {
        SimpleDateFormat("MM-dd", Locale.CHINA)
    }

    private val DATETIME_FMT = ThreadLocal.withInitial {
        SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    }

    private val TIME_FMT = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.CHINA)
    }

    /**
     * epoch millis → "08-12"
     */
    fun Long.toDateString(): String = DATE_FMT.get()!!.format(Date(this))

    /**
     * epoch millis → "08-12 08:30"
     */
    fun Long.toDateTimeString(): String = DATETIME_FMT.get()!!.format(Date(this))

    /**
     * epoch millis → "08:30"
     */
    fun Long.toTimeString(): String = TIME_FMT.get()!!.format(Date(this))

    /**
     * epoch millis → LocalDateTime（系统时区）
     */
    fun Long.toLocalDateTime(): LocalDateTime =
        Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)

    /**
     * 当月开始/下月开始（epoch millis）
     *
     * @return Pair(startOfMonth, startOfNextMonth)
     */
    fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val start = kotlinx.datetime.LocalDate(year, Month(month), 1)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()
        val next = if (month == 12) {
            kotlinx.datetime.LocalDate(year + 1, Month.JANUARY, 1)
        } else {
            kotlinx.datetime.LocalDate(year, Month(month + 1), 1)
        }.atStartOfDayIn(zone).toEpochMilliseconds()
        return start to next
    }

    /**
     * 当前时间
     */
    fun nowMillis(): Long = System.currentTimeMillis()
}
