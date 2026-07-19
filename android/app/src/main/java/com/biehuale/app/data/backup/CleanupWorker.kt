package com.biehuale.app.data.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.biehuale.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 回收站 30 天清理 Worker
 *
 * 详见 docs/PRD.md §6.2
 *
 * 调度策略：
 *  - 周期性：每 24 小时跑一次
 *  - 兜底：App 启动时也触发一次（避免首次安装没机会调度）
 *
 * 删除逻辑：
 *  - transactions.deleted_at < (now - 30 天) 的硬删除
 *  - 删除前不打日志（个人记账，数据隐私）
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val threshold = System.currentTimeMillis() - RETENTION_MILLIS
            transactionRepository.cleanupExpired(threshold)
            Log.i(TAG, "30 天回收站清理完成")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "30 天回收站清理失败", e)
            Result.retry()  // WorkManager 自动重试
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
        /** 30 天 = 30 × 24 × 60 × 60 × 1000 ms */
        const val RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L

        const val UNIQUE_WORK_NAME = "biehuale_recycle_bin_cleanup"
    }
}
