package com.biehuale.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clarity Teal 动效 Token（docs/UI_DESIGN.md §4）
 *
 * 少而准：统一时长与曲线，禁止强弹跳 / 循环闪烁。
 */
object AppMotion {
    /** 模式切换指示条 / 语义色交叉 */
    const val ModeMs = 180

    /** 金额入场、数字轻反馈 */
    const val AmountMs = 150

    /** 键盘按下 scale（按下感，略短于金额） */
    const val KeyPressMs = 70

    /** 列表软删 / item 变更 */
    const val ListMs = 180

    /** Sheet / 确认面板 */
    const val SheetMs = 200

    /** 主 Tab 互切 fade（覆盖 Navigation 默认 700ms） */
    const val NavTabMs = 120

    /** 二级页 push / pop */
    const val NavPushMs = 200

    /** 金额入场上移距离 */
    val AmountEnterOffset: Dp = 4.dp

    val Easing = FastOutSlowInEasing

    fun <T> mode(): TweenSpec<T> = tween(ModeMs, easing = Easing)
    fun <T> amount(): TweenSpec<T> = tween(AmountMs, easing = Easing)
    fun <T> keyPress(): TweenSpec<T> = tween(KeyPressMs, easing = Easing)
    fun <T> list(): TweenSpec<T> = tween(ListMs, easing = Easing)
    fun <T> sheet(): TweenSpec<T> = tween(SheetMs, easing = Easing)
    fun <T> navTab(): TweenSpec<T> = tween(NavTabMs, easing = Easing)
    fun <T> navPush(): TweenSpec<T> = tween(NavPushMs, easing = Easing)
}
