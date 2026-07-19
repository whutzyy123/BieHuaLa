package com.biehuale.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.biehuale.app.data.preferences.ThemeMode

/**
 * Clarity Teal Theme（docs/UI_DESIGN.md v2.0）
 *
 * - 默认品牌色板优先；可选 Dynamic Color（设置开关，默认关）
 * - 主题模式：SYSTEM / LIGHT / DARK
 */

/** 与 [BieHuaLeTheme] 的实际亮/暗一致（强制主题时不等于系统） */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightFallbackScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandOnPrimaryLight,
    primaryContainer = BrandContainerLight,
    onPrimaryContainer = Brand20,

    secondary = BrandPrimaryLight,
    onSecondary = BrandOnPrimaryLight,
    secondaryContainer = BrandContainerLight,
    onSecondaryContainer = Brand20,

    tertiary = SemanticIncomeLight,
    onTertiary = Color.White,

    background = SurfaceBaseLight,
    onBackground = BrandInkLight,
    surface = SurfaceElevatedLight,
    onSurface = BrandInkLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = SurfaceBaseLight,
    surfaceContainerLow = SurfaceElevatedLight,
    surfaceContainer = SurfaceElevatedLight,
    surfaceContainerHigh = SurfaceMutedLight,
    surfaceContainerHighest = SurfaceMutedLight,

    error = SemanticExpenseLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = TextSecondaryLight,
    outlineVariant = StrokeSubtleLight
)

private val DarkFallbackScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandContainerDark,
    onPrimaryContainer = Brand80,

    secondary = BrandPrimaryDark,
    onSecondary = BrandOnPrimaryDark,
    secondaryContainer = BrandContainerDark,
    onSecondaryContainer = Brand80,

    tertiary = SemanticIncomeDark,
    onTertiary = Color(0xFF003910),

    background = SurfaceBaseDark,
    onBackground = BrandInkDark,
    surface = SurfaceElevatedDark,
    onSurface = BrandInkDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = SurfaceBaseDark,
    surfaceContainerLow = SurfaceElevatedDark,
    surfaceContainer = SurfaceElevatedDark,
    surfaceContainerHigh = SurfaceMutedDark,
    surfaceContainerHighest = SurfaceMutedDark,

    error = SemanticExpenseDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = TextSecondaryDark,
    outlineVariant = StrokeSubtleDark
)

/**
 * 收入 / 支出 / 转账语义色 — 跟随 [LocalIsDarkTheme]，不随 Dynamic Color 漂移
 */
object AppSemanticColors {
    val income: Color
        @Composable get() = if (LocalIsDarkTheme.current) SemanticIncomeDark else SemanticIncomeLight
    val expense: Color
        @Composable get() = if (LocalIsDarkTheme.current) SemanticExpenseDark else SemanticExpenseLight
    val transfer: Color
        @Composable get() = if (LocalIsDarkTheme.current) SemanticTransferDark else SemanticTransferLight
}

/**
 * Theme 入口
 *
 * @param themeMode SYSTEM / LIGHT / DARK
 * @param dynamicColor 是否使用 Material You（默认 false，品牌优先）
 */
@Composable
fun BieHuaLeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        isDark -> DarkFallbackScheme
        else -> LightFallbackScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
