package com.biehuale.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主题偏好（DataStore）
 *
 * 详见 docs/PRD.md §7.2
 *
 * 三档：SYSTEM（跟系统）/ LIGHT（强制浅色）/ DARK（强制深色）
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 顶层 DataStore 扩展（一个 App 一个 DataStore）
 */
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "biehuale_prefs")

/**
 * 主题偏好仓库
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val raw = prefs[themeKey] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }
}
