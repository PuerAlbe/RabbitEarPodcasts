package com.example.rabbitear

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
class RmsAudioProcessor : BaseAudioProcessor() {
    var rmsLeft = 0f
        private set
    var rmsRight = 0f
        private set

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_INVALID) {
            return inputAudioFormat
        }
        return AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val channelCount = inputAudioFormat.channelCount
        val encoding = inputAudioFormat.encoding
        if (channelCount <= 0) {
            val buffer = replaceOutputBuffer(remaining)
            buffer.put(inputBuffer)
            buffer.flip()
            return
        }

        var sumSquareL = 0.0
        var sumSquareR = 0.0
        var sampleCountL = 0
        var sampleCountR = 0

        val initialPos = inputBuffer.position()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        if (encoding == C.ENCODING_PCM_FLOAT) {
            while (inputBuffer.remaining() >= 4 * channelCount) {
                val sampleL = inputBuffer.float.toDouble()
                sumSquareL += sampleL * sampleL
                sampleCountL++

                if (channelCount >= 2) {
                    val sampleR = inputBuffer.float.toDouble()
                    sumSquareR += sampleR * sampleR
                    sampleCountR++
                }
            }
        } else {
            while (inputBuffer.remaining() >= 2 * channelCount) {
                val sampleL = inputBuffer.short / 32768.0
                sumSquareL += sampleL * sampleL
                sampleCountL++

                if (channelCount >= 2) {
                    val sampleR = inputBuffer.short / 32768.0
                    sumSquareR += sampleR * sampleR
                    sampleCountR++
                }
            }
        }

        if (sampleCountL > 0) {
            val rmsL = sqrt(sumSquareL / sampleCountL).toFloat()
            val valL = (rmsL * 4.5f).coerceIn(0.08f, 1.0f)
            rmsLeft = valL
            PlaybackService.currentRmsLeft = valL
        }
        if (sampleCountR > 0) {
            val rmsR = sqrt(sumSquareR / sampleCountR).toFloat()
            val valR = (rmsR * 4.5f).coerceIn(0.08f, 1.0f)
            rmsRight = valR
            PlaybackService.currentRmsRight = valR
        } else {
            rmsRight = rmsLeft
            PlaybackService.currentRmsRight = rmsLeft
        }

        inputBuffer.position(initialPos)
        val buffer = replaceOutputBuffer(remaining)
        buffer.put(inputBuffer)
        buffer.flip()
    }
}

/** Owns the player. When an episode ends in schedule mode it marks it played and rolls straight into the next one. */
class PlaybackService : MediaSessionService() {
    companion object {
        var currentRmsLeft = 0f
        var currentRmsRight = 0f
    }

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val rmsProcessor = RmsAudioProcessor()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val repo = (application as App).repo
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    } catch (_: Exception) { null }

                    if (device != null && isVehicleDevice(device)) {
                        if (repo.autoPlayInVehicle()) {
                            if (player.mediaItemCount > 0 && !player.isPlaying) {
                                player.play()
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED, AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            }
        }
    }

    private fun isVehicleDevice(device: BluetoothDevice): Boolean {
        return try {
            val bluetoothClass = try { device.bluetoothClass } catch (_: SecurityException) { null }
            if (bluetoothClass != null) {
                val deviceClass = bluetoothClass.deviceClass
                val isCarAudio = deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
                val major = bluetoothClass.majorDeviceClass
                val isAudioMajor = major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                        major == BluetoothClass.Device.Major.UNCATEGORIZED
                val name = try { device.name ?: "" } catch (_: SecurityException) { "" }
                val nameMatchesCar = name.contains("car", ignoreCase = true) ||
                        name.contains("auto", ignoreCase = true) ||
                        name.contains("vehicle", ignoreCase = true) ||
                        name.contains("sync", ignoreCase = true) ||
                        name.contains("uconnect", ignoreCase = true) ||
                        name.contains("carplay", ignoreCase = true) ||
                        name.contains("stereo", ignoreCase = true)
                isCarAudio || (isAudioMajor && nameMatchesCar)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateAudioEffects() {
        val repo = (application as App).repo
        val voiceBoost = repo.voiceBoostEnabled()
        val normalize = repo.audioNormalizationEnabled()
        val sessionAudioId = player.audioSessionId
        if (sessionAudioId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = LoudnessEnhancer(sessionAudioId)
                }
                val gainMb = when {
                    voiceBoost && normalize -> 1200
                    voiceBoost -> 800
                    normalize -> 500
                    else -> 0
                }
                loudnessEnhancer?.apply {
                    setTargetGain(gainMb)
                    enabled = (gainMb > 0)
                }
            } catch (_: Exception) {}
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val repo = (application as App).repo

        val audioSink = DefaultAudioSink.Builder(applicationContext)
            .setAudioProcessors(arrayOf(rmsProcessor))
            .build()
        val renderersFactory = object : DefaultRenderersFactory(applicationContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ) = audioSink
        }

        player = ExoPlayer.Builder(this, renderersFactory)
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
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateAudioEffects()
                if (!isPlaying) save()
            }
        })

        scope.launch {
            while (true) {
                if (player.isPlaying) {
                    currentRmsLeft = rmsProcessor.rmsLeft
                    currentRmsRight = rmsProcessor.rmsRight
                } else {
                    currentRmsLeft = 0f
                    currentRmsRight = 0f
                }
                delay(40)
            }
        }

        scope.launch { while (true) { delay(5_000); if (player.isPlaying) save() } }
        session = MediaSession.Builder(this, player).build()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, filter)
        }
    }

    private fun save() {
        val id = player.currentMediaItem?.mediaId ?: return
        val pos = player.currentPosition
        val dur = player.duration.takeIf { it > 0 } ?: 0L
        val repo = (application as App).repo
        repo.setLastPlayedGuid(id)
        scope.launch { repo.saveProgress(id, pos, dur) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        save(); session?.release(); player.release(); scope.cancel(); super.onDestroy()
    }
}