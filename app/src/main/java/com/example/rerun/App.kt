package com.example.rerun

import android.app.Application
import android.content.Context
import androidx.work.*
import com.example.rerun.data.Repo
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class App : Application() {
    lateinit var repo: Repo
    override fun onCreate() {
        super.onCreate()
        repo = Repo(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "refresh", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(3, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
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
        val dir = applicationContext.getExternalFilesDir("episodes")!!.apply { mkdirs() }
        val file = File(dir, "${ep.guid.hashCode()}.audio")
        try {
            repo.http.newCall(Request.Builder().url(ep.audioUrl).build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext Result.retry()
                r.body!!.byteStream().use { i -> file.outputStream().use { i.copyTo(it) } }
            }
            repo.dao.setLocal(ep.guid, file.path, bySyndication)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}
