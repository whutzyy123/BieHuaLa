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
 * 别花乐 (BieHuaLe) - Theme
 *
 * - Android 12+ 走 Dynamic Color（从系统壁纸取色）
 * - Android 11- 走 brand fallback（青绿色）
 * - 主题模式：SYSTEM（跟系统）/ LIGHT / DARK
 *
 * 详见 docs/PRD.md §7.1、§7.2。
 */

/** 与 [BieHuaLeTheme] 的实际亮/暗一致（强制主题时不等于系统） */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

// ---------- 浅色 fallback ----------
private val IncomeGreenLight = Color(0xFF2E7D32)
private val ExpenseRedLight = Color(0xFFC62828)
private val TransferBlueLight = Color(0xFF1565C0)

private val LightFallbackScheme = lightColorScheme(
    primary = Brand40,
    onPrimary = Color.White,
    primaryContainer = Brand80,
    onPrimaryContainer = Brand20,

    secondary = Brand40,
    onSecondary = Color.White,
    secondaryContainer = Brand80,
    onSecondaryContainer = Brand20,

    tertiary = IncomeGreenLight,
    onTertiary = Color.White,

    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),

    error = ExpenseRedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5)
)

// ---------- 深色 fallback ----------
private val IncomeGreenDark = Color(0xFF81C784)
private val ExpenseRedDark = Color(0xFFEF9A9A)
private val TransferBlueDark = Color(0xFF64B5F6)

private val DarkFallbackScheme = darkColorScheme(
    primary = Brand80,
    onPrimary = Brand20,
    primaryContainer = Brand40,
    onPrimaryContainer = Brand80,

    secondary = Brand80,
    onSecondary = Brand20,
    secondaryContainer = Brand40,
    onSecondaryContainer = Brand80,

    tertiary = IncomeGreenDark,
    onTertiary = Color(0xFF003910),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE2E3E1),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE2E3E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),

    error = ExpenseRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4946)
)

/**
 * 收入 / 支出 / 转账语义色 — 跟随 [LocalIsDarkTheme]，非系统暗色开关
 */
object AppSemanticColors {
    val income: Color
        @Composable get() = if (LocalIsDarkTheme.current) IncomeGreenDark else IncomeGreenLight
    val expense: Color
        @Composable get() = if (LocalIsDarkTheme.current) ExpenseRedDark else ExpenseRedLight
    val transfer: Color
        @Composable get() = if (LocalIsDarkTheme.current) TransferBlueDark else TransferBlueLight
}

/**
 * Theme 入口
 *
 * @param themeMode SYSTEM（跟系统）/ LIGHT（强制浅色）/ DARK（强制深色）
 * @param dynamicColor 是否使用 Material You Dynamic Color（Android 12+）
 */
@Composable
fun BieHuaLeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
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
            content = content
        )
    }
}
