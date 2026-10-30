package com.rabbitear.app

import android.app.Application
import android.content.Context
import androidx.work.*
import com.rabbitear.app.data.Repo
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class App : Application() {
    lateinit var repo: Repo
    override fun onCreate() {
        super.onCreate()
        repo = Repo(this)
        CoroutineScope(Dispatchers.IO).launch {
            repo.autoRestoreIfNeeded(this@App)
            repo.reconcileDownloadsOnStartup()
        }
        val hours = repo.refreshIntervalHours()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "refresh", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(hours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_backup", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS).build()
        )
    }
}

class RefreshWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result { (applicationContext as App).repo.refreshAll(); return Result.success() }
}

class DownloadWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = (applicationContext as App).repo
        val guid = inputData.getString("guid") ?: return@withContext Result.failure()
        val bySyndication = inputData.getBoolean("bySyndication", false)
        val ep = repo.dao.episode(guid) ?: return@withContext Result.failure()

        val dir = File(repo.dataDirectoryPath(), "episodes").apply { mkdirs() }
        val finalFileName = repo.getDownloadFileName(guid)
        val finalFile = File(dir, finalFileName)
        val tmpFile = File(dir, "$finalFileName.tmp")

        if (finalFile.exists()) {
            repo.dao.setLocal(ep.guid, finalFile.path, bySyndication)
            return@withContext Result.success()
        }

        try {
            val existingLength = if (tmpFile.exists()) tmpFile.length() else 0L
            val reqBuilder = Request.Builder().url(ep.audioUrl)
            if (existingLength > 0L) {
                reqBuilder.header("Range", "bytes=$existingLength-")
            }

            repo.http.newCall(reqBuilder.build()).execute().use { resp ->
                val code = resp.code
                val isPartial = (code == 206)
                if (!resp.isSuccessful && !isPartial) return@withContext Result.retry()

                val body = resp.body ?: return@withContext Result.retry()
                val append = isPartial && existingLength > 0L

                val outputStream = if (append) java.io.FileOutputStream(tmpFile, true) else java.io.FileOutputStream(tmpFile, false)
                outputStream.use { out ->
                    body.byteStream().use { input ->
                        input.copyTo(out)
                    }
                }

                if (tmpFile.renameTo(finalFile) || finalFile.exists()) {
                    repo.dao.setLocal(ep.guid, finalFile.path, bySyndication)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
