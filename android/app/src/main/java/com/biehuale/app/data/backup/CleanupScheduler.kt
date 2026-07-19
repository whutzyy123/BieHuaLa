package com.biehuale.app.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 回收站清理调度器
 *
 * 详见 docs/PRD.md §6.2
 *
 * 用法：
 *  - App 启动时调一次 `schedule()` 注册 Periodic WorkManager
 *  - `runOnce()` 用 unique work + KEEP，反复冷启动不会堆队列
 */
object CleanupScheduler {

    const val ONE_TIME_UNIQUE_NAME = "biehuale_recycle_bin_cleanup_once"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CleanupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * 立即触发一次（App 启动兜底）。已有同名任务在队列则保留，不重复 enqueue。
     */
    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<CleanupWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
