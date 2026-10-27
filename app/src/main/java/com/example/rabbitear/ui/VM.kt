package com.example.rabbitear.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.rabbitear.App
import com.example.rabbitear.PlaybackService
import com.example.rabbitear.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class VM(app: Application) : AndroidViewModel(app) {
    val repo = (app as App).repo
    private var c: MediaController? = null
    var nowTitle by mutableStateOf(""); var nowShow by mutableStateOf(""); var nowArtwork by mutableStateOf<String?>(null)
    var isPlaying by mutableStateOf(false); var scheduleOn by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L); var durationMs by mutableLongStateOf(0L)
    var sleepTimerRemainingSec by mutableIntStateOf(0)
    private var sleepTimerJob: Job? = null
    private var pauseTimerJob: Job? = null

    init {
        val f = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
        f.addListener({
            val ctl = f.get(); c = ctl
            isPlaying = ctl.isPlaying
            nowTitle = ctl.mediaMetadata.title?.toString() ?: ""
            nowShow = ctl.mediaMetadata.artist?.toString() ?: ""
            nowArtwork = ctl.mediaMetadata.artworkUri?.toString()
            scheduleOn = repo.mode() != null
            ctl.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(v: Boolean) {
                    isPlaying = v
                    if (!v && sleepTimerRemainingSec > 0) {
                        pauseTimerJob?.cancel()
                        pauseTimerJob = viewModelScope.launch {
                            var count = 0
                            val target = sleepTimerRemainingSec
                            while (count < target && !isPlaying) {
                                delay(1000)
                                count++
                            }
                            if (!isPlaying && sleepTimerRemainingSec > 0) {
                                cancelSleepTimerInternal()
                            }
                        }
                    } else if (v) {
                        pauseTimerJob?.cancel()
                    }
                }
                override fun onMediaMetadataChanged(m: MediaMetadata) {
                    nowTitle = m.title?.toString() ?: ""
                    nowShow = m.artist?.toString() ?: ""
                    nowArtwork = m.artworkUri?.toString()
                    scheduleOn = repo.mode() != null
                }
            })
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                c?.let {
                    if (it.isPlaying) {
                        positionMs = it.currentPosition.coerceAtLeast(0)
                        durationMs = it.duration.coerceAtLeast(0)
                    }
                }
                delay(500)
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                try {
                    val file = getSettingsFile(app)
                    file.outputStream().use { repo.exportSettings(it) }
                } catch (_: Exception) {}
            }
        }
    }

    private fun load(e: Episode) = viewModelScope.launch {
        val item = repo.item(e)
        c?.apply { setMediaItem(item, e.positionMs); prepare(); play() }
        scheduleOn = repo.mode() != null
    }

    fun play(e: Episode) { repo.setMode(null); load(e) }
    fun startSchedule(m: Mode) { viewModelScope.launch { repo.startSchedule(m)?.let { load(it) } } }

    fun skip() {
        viewModelScope.launch {
            val curGuid = repo.currentGuid() ?: c?.currentMediaItem?.mediaId
            if (curGuid != null) {
                repo.finish(curGuid)
            }
            val m = repo.mode() ?: Mode.OLDEST
            repo.nextScheduled(m)?.let { load(it) }
        }
    }

    fun skipBack() {
        c?.let { it.seekTo((it.currentPosition - 30_000).coerceAtLeast(0)) }
    }

    fun skipForward() {
        c?.let {
            val dur = if (it.duration > 0) it.duration else Long.MAX_VALUE
            it.seekTo((it.currentPosition + 30_000).coerceAtMost(dur))
        }
    }

    fun seekTo(pos: Long) {
        c?.seekTo(pos)
        positionMs = pos
    }

    fun togglePlay() {
        c?.let { ctl ->
            val id = ctl.currentMediaItem?.mediaId
            val pos = ctl.currentPosition.coerceAtLeast(0)
            val dur = ctl.duration.takeIf { it > 0 } ?: 0L
            if (id != null) {
                viewModelScope.launch { repo.saveProgress(id, pos, dur) }
            }
            if (ctl.isPlaying) ctl.pause() else ctl.play()
        }
    }

    fun sub(p: Podcast, on: Boolean) { viewModelScope.launch { repo.dao.setSubscribed(p.feedUrl, on) } }
    fun setPlayed(e: Episode, v: Boolean) {
        viewModelScope.launch {
            if (v) repo.finish(e.guid) else repo.dao.setPlayed(e.guid, false)
        }
    }
    fun toggleDownload(e: Episode) {
        if (e.localPath == null) {
            repo.enqueueDownload(e.guid, bySyndication = false)
        } else viewModelScope.launch { repo.deleteDownload(e) }
    }
    fun toggleFavorite(e: Episode) {
        viewModelScope.launch {
            repo.dao.setFavorite(e.guid, !e.favorite)
        }
    }
    fun toggleSlate(e: Episode) {
        viewModelScope.launch {
            repo.dao.setInSlate(e.guid, !e.inSlate, System.currentTimeMillis())
        }
    }
    fun currentMediaId(): String? = c?.currentMediaItem?.mediaId
    fun toggleFavoriteCurrent() {
        val guid = currentMediaId() ?: return
        viewModelScope.launch {
            val ep = repo.dao.episode(guid)
            if (ep != null) {
                repo.dao.setFavorite(ep.guid, !ep.favorite)
            }
        }
    }

    private fun cancelSleepTimerInternal() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseTimerJob?.cancel()
        pauseTimerJob = null
        sleepTimerRemainingSec = 0
    }

    fun startSleepTimer(minutes: Int, ctx: Context) {
        cancelSleepTimerInternal()
        val totalSec = minutes * 60
        sleepTimerRemainingSec = totalSec
        android.widget.Toast.makeText(ctx, "Sleep timer set for $minutes minutes", android.widget.Toast.LENGTH_SHORT).show()

        sleepTimerJob = viewModelScope.launch {
            while (sleepTimerRemainingSec > 0) {
                delay(1000)
                sleepTimerRemainingSec--
            }
            c?.pause()
            android.widget.Toast.makeText(ctx, "Sleep timer finished. Playback paused.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelSleepTimer(ctx: Context) {
        cancelSleepTimerInternal()
        android.widget.Toast.makeText(ctx, "Sleep timer turned off", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun getSettingsFile(ctx: Context): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!docsDir.exists()) {
            try { docsDir.mkdirs() } catch (_: Exception) {}
        }
        val targetDir = if (docsDir.exists() && docsDir.canWrite()) docsDir else ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: ctx.filesDir
        return File(targetDir, "rabbitear_settings.json")
    }

    fun exportSettingsDirect(ctx: Context, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = getSettingsFile(ctx)
                file.outputStream().use { repo.exportSettings(it) }
                onDone(true)
            } catch (_: Exception) {
                onDone(false)
            }
        }
    }

    fun importSettingsDirect(ctx: Context, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val file = getSettingsFile(ctx)
                if (file.exists()) {
                    file.inputStream().use { repo.importSettings(it) }
                    onDone(true)
                } else {
                    onDone(false)
                }
            } catch (_: Exception) {
                onDone(false)
            }
        }
    }
}
