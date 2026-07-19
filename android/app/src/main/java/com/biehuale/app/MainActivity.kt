package com.biehuale.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.preferences.ThemePreferences
import com.biehuale.app.data.preferences.ThemeMode
import com.biehuale.app.ui.nav.AppNav
import com.biehuale.app.ui.theme.BieHuaLeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 别花乐 (BieHuaLe) - 单 Activity 容器
 *
 * 职责：
 *  - 启动屏（Android 12+ SplashScreen API / 11- 走 windowBackground）
 *  - 边到边 (edge-to-edge) 渲染
 *  - 套用 BieHuaLeTheme（主题模式从 DataStore 读取）
 *  - 承载 AppNav
 *
 * 详见 docs/DEV_PLAN.md §8 Task 5.2
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启动屏必须在 super.onCreate 之前调
        // Android 12+ 用 SplashScreen API
        // Android 11- 自动用 windowBackground（themes.xml 里的 splash_background）
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            BieHuaLeTheme(themeMode = themeMode) {
                AppNav()
            }
        }
    }
}
