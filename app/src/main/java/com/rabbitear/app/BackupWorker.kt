package com.rabbitear.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = (applicationContext as App).repo
        try {
            repo.autoBackupAllFormats(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
