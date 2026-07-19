package com.biehuale.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.biehuale.app.data.backup.CleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 别花乐 (BieHuaLe) - Application
 *
 * 职责：
 *  - 初始化 Hilt（@HiltAndroidApp）
 *  - 提供 WorkManager Configuration（用 HiltWorkerFactory 注入 Worker）
 *  - 调度回收站 30 天清理
 *
 * 详见 docs/DEV_PLAN.md §7 Task 4.5
 */
@HiltAndroidApp
class BieHuaLeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 注册周期清理（KEEP 策略，重复调用安全）
        CleanupScheduler.schedule(this)
        // 启动时立即跑一次兜底
        CleanupScheduler.runOnce(this)
    }
}
