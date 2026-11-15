package com.sonora.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.sonora.nowplaying.EngineHandle
import com.sonora.studio.AudioStudioConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M6 UI-side engine: implements [EngineHandle] over a MediaController, so
 * the state machine keeps issuing the same commands while the players live
 * in [SonoraPlaybackService]. Connects lazily and buffers commands until
 * the session binds; engine truth flows back through [PlaybackHub], never
 * the controller. Main-thread only.
 *
 * The service runs in this process, so it only ever dies alone via its own
 * stop paths (task swiped while paused) — never under a live UI. When that
 * happens across an app relaunch, [connected] drops after [hadConnection]
 * and the screen restarts the session instead of commanding a ghost.
 */
class ServiceEngine private constructor(appContext: Context) : EngineHandle {

    companion object {
        private const val MAX_PENDING = 64

        @Volatile
        private var instance: ServiceEngine? = null

        fun get(context: Context): ServiceEngine {
            return instance ?: synchronized(this) {
                instance ?: ServiceEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val app = appContext.applicationContext
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = ArrayDeque<(MediaController) -> Unit>()

    private val _connected = MutableStateFlow(false)
    /** True while bound to the service session. */
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _hadConnection = MutableStateFlow(false)
    /** True once a session has ever bound in this process. */
    val hadConnection: StateFlow<Boolean> = _hadConnection.asStateFlow()

    private val listener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            this@ServiceEngine.controller = null
            _connected.value = false
        }
    }

    // After every property above: init blocks run in declaration order.
    init {
        ensureController()
    }

    private fun ensureController() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(app, ComponentName(app, SonoraPlaybackService::class.java))
        val future = MediaController.Builder(app, token).setListener(listener).buildAsync()
        future.addListener(
            {
                connecting = false
                controller = try {
                    future.get()
                } catch (_: Exception) {
                    null
                }
                controller?.let { connected ->
                    _connected.value = true
                    _hadConnection.value = true
                    while (pending.isNotEmpty()) {
                        val block = pending.removeFirst()
                        try {
                            block(connected)
                        } catch (e: Exception) {
                            android.util.Log.w("SonoraEng", "buffered command failed", e)
                        }
                    }
                } ?: android.util.Log.w("SonoraEng", "controller build failed")
            },
            ContextCompat.getMainExecutor(app),
        )
    }

    private fun command(name: String, block: (MediaController) -> Unit) {
        controller?.let {
            try {
                block(it)
            } catch (e: Exception) {
                android.util.Log.w("SonoraEng", "command $name failed on live controller", e)
            }
            return
        }
        if (pending.size >= MAX_PENDING) pending.removeFirst()
        pending += block
        ensureController()
    }

    private fun custom(name: String, args: Bundle) {
        command("custom:$name") { it.sendCustomCommand(SessionCommand(name, Bundle.EMPTY), args) }
    }

    override fun enginePlay(index: Int, uri: String) {
        custom(
            PlaybackCommands.CUE,
            bundleOf(PlaybackCommands.EXTRA_INDEX to index, PlaybackCommands.EXTRA_URI to uri),
        )
    }

    override fun engineResume() {
        command("resume") {
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(it.currentMediaItemIndex, 0L)
            }
            it.play()
        }
    }

    override fun enginePause() {
        command("pause") { it.pause() }
    }

    override fun engineSeekTo(positionMs: Long) {
        command("seek:$positionMs") { it.seekTo(positionMs.coerceAtLeast(0L)) }
    }

    fun sendQueue(
        uris: List<String>,
        titles: List<String>,
        artists: List<String>,
        index: Int,
        singleQueue: Boolean,
        positionMs: Long,
    ) {
        custom(
            PlaybackCommands.QUEUE,
            bundleOf(
                PlaybackCommands.EXTRA_URIS to ArrayList(uris),
                PlaybackCommands.EXTRA_TITLES to ArrayList(titles),
                PlaybackCommands.EXTRA_ARTISTS to ArrayList(artists),
                PlaybackCommands.EXTRA_INDEX to index,
                PlaybackCommands.EXTRA_SINGLE to singleQueue,
                PlaybackCommands.EXTRA_POSITION to positionMs,
            ),
        )
    }

    fun sendStudio(config: AudioStudioConfig) {
        custom(
            PlaybackCommands.STUDIO,
            bundleOf(
                PlaybackCommands.EXTRA_EQ_ENABLED to config.eqEnabled,
                PlaybackCommands.EXTRA_BANDS to config.bands.toFloatArray(),
                PlaybackCommands.EXTRA_PREAMP to config.preampDb,
                PlaybackCommands.EXTRA_RG to config.replayGain.name,
                PlaybackCommands.EXTRA_GAPLESS to config.gapless,
                PlaybackCommands.EXTRA_XFADE_ENABLED to config.crossfadeEnabled,
                PlaybackCommands.EXTRA_XFADE_SECS to config.crossfadeSecs,
            ),
        )
    }

    fun sendRepeatOne(enabled: Boolean) {
        custom(PlaybackCommands.REPEAT_ONE, bundleOf(PlaybackCommands.EXTRA_ENABLED to enabled))
    }
}
