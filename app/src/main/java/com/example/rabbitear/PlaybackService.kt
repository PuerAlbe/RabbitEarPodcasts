package com.example.rabbitear

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.*

/** Owns the player. When an episode ends in schedule mode it marks it played and rolls straight into the next one. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val repo = (application as App).repo
        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(30_000).setSeekForwardIncrementMs(30_000)
            .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).setUsage(C.USAGE_MEDIA).build(), true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_ENDED) return
                val id = player.currentMediaItem?.mediaId ?: return
                scope.launch {
                    repo.finish(id)
                    val mode = repo.mode() ?: return@launch
                    repo.nextScheduled(mode)?.let {
                        player.setMediaItem(repo.item(it), it.positionMs)
                        player.prepare(); player.play()
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (!isPlaying) save() }
        })
        scope.launch { while (true) { delay(5_000); if (player.isPlaying) save() } }
        session = MediaSession.Builder(this, player).build()
    }

    private fun save() {
        val id = player.currentMediaItem?.mediaId ?: return
        val pos = player.currentPosition
        val dur = player.duration.takeIf { it > 0 } ?: 0L
        scope.launch { (application as App).repo.saveProgress(id, pos, dur) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        save(); session?.release(); player.release(); scope.cancel(); super.onDestroy()
    }
}
