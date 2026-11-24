package com.sonora.playback

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonora.data.Album
import com.sonora.nowplaying.NowPlayingState
import com.sonora.nowplaying.RepeatMode
import com.sonora.studio.AudioStudioStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * M6 app-scoped session owner. The service outlives the Activity, so the
 * transport policy it executes must not be screen-owned: the orchestrator
 * holds the one live [NowPlayingState], mirrors service truth from
 * [PlaybackHub] into it, and answers service callbacks (notification,
 * lock-screen) even with no UI present. Demo albums bypass it entirely
 * with screen-local ticker state.
 */
object PlaybackOrchestrator {

    var state: NowPlayingState? by mutableStateOf(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectorJob: Job? = null

    /**
     * Starts (or replaces) real album playback through the service. Safe
     * to call repeatedly and after process death.
     */
    fun startAlbum(
        context: Context,
        album: Album,
        startIndex: Int,
        positionMs: Long = 0L,
        playing: Boolean = true,
        shuffle: Boolean = false,
    ) {
        require(album.isReal) { "Orchestrator plays real albums; demo stays local" }
        val appContext = context.applicationContext
        // Plain start: every call originates in the foreground UI, and the
        // session manager foregrounds the service itself on playback.
        appContext.startService(
            Intent(appContext, SonoraPlaybackService::class.java)
                .setAction(SonoraPlaybackService.ACTION_START),
        )
        val engine = ServiceEngine.get(appContext)
        val fresh = NowPlayingState(album, startIndex).apply {
            restore(startIndex, positionMs, playing, shuffle, RepeatMode.OFF)
            this.engine = engine
        }
        activate(fresh)
        engine.sendQueue(
            uris = album.tracks.map { it.uri!! },
            titles = album.tracks.map { it.title },
            artists = album.tracks.map { it.artist ?: album.artist },
            index = fresh.trackIndex,
            singleQueue = fresh.shuffle || !AudioStudioStore.config.gapless,
            positionMs = fresh.positionMs,
        )
        engine.sendRepeatOne(false)
        engine.sendStudio(AudioStudioStore.config)
        if (playing) engine.engineResume() else engine.enginePause()
    }

    /**
     * Attaches a service-restored session (M7 sticky resume). The engine
     * handle is already bound; this just publishes the state and mirrors
     * the hub into it.
     */
    fun attachRestored(restored: NowPlayingState) {
        activate(restored)
    }

    private fun activate(fresh: NowPlayingState) {
        state?.engine = null
        state = fresh
        PlaybackHub.reset()
        collectorJob?.cancel()
        collectorJob = scope.launch {
            launch {
                PlaybackHub.positionMs.collect {
                    fresh.syncPlayback(it, PlaybackHub.isPlaying.value)
                }
            }
            launch {
                PlaybackHub.isPlaying.collect {
                    fresh.syncPlayback(PlaybackHub.positionMs.value, it)
                }
            }
            launch { PlaybackHub.audioSessionId.collect { fresh.audioSessionId = it } }
            launch { PlaybackHub.naturalTransitions.collect { fresh.syncTrack(it) } }
            launch { PlaybackHub.playlistEnded.collect { fresh.onPlaylistEnded() } }
        }
    }
}
