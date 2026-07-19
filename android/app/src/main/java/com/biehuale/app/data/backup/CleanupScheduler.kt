package com.biehuale.app.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 回收站清理调度器
 *
 * 详见 docs/PRD.md §6.2
 *
 * 用法：
 *  - App 启动时调一次 `schedule()` 注册 WorkManager
 *  - 重复调用是安全的（KEEP 策略）
 */
object CleanupScheduler {

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            // 不需要网络，纯本地
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.DAYS  // 最小周期 15 分钟，这里用 1 天
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CleanupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // 已存在则保留，不重复
            request
        )
    }

    /**
     * 立即触发一次（App 启动兜底）
     */
    fun runOnce(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<CleanupWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
