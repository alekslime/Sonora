package com.sonora.nowplaying

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonora.data.Album
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Repeat behavior for end-of-track advance. */
enum class RepeatMode { OFF, ALL, ONE }

/**
 * Commands [NowPlayingState] forwards to the real engine. The state machine
 * stays the single owner of transport policy (shuffle picks, repeat wraps,
 * prev threshold); the engine only renders audio.
 */
interface EngineHandle {
    /** Cues [index] at [uri] and starts it from the top. */
    fun enginePlay(index: Int, uri: String)
    fun engineResume()
    fun enginePause()
    fun engineSeekTo(positionMs: Long)
}

/**
 * Playback state with real transport semantics — play, pause, seek,
 * next/previous with end-of-track advance, shuffle and repeat. The state
 * machine owns all policy; when [engine] is attached (M5 real albums) every
 * decision also forwards to the engine, which renders audio and reports
 * back through [syncTrack], [syncPlayback] and [onPlaylistEnded]. With no
 * engine (demo albums) the ticker simulates the clock instead.
 */
@Stable
class NowPlayingState(album: Album, startTrackIndex: Int) {
    val album: Album = album

    var trackIndex by mutableIntStateOf(startTrackIndex.coerceIn(album.tracks.indices))
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set

    /** Platform audio session of the attached engine; 0 when unattached. */
    var audioSessionId by mutableIntStateOf(0)

    /** Real engine for [com.sonora.data.Album.isReal]; null for demo. */
    var engine: EngineHandle? = null

    val track get() = album.tracks[trackIndex]
    val durationMs: Long get() = track.durationMs
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    fun play() {
        if (positionMs >= durationMs) positionMs = 0L
        isPlaying = true
        engine?.engineResume()
    }

    fun pause() {
        isPlaying = false
        engine?.enginePause()
    }

    fun toggle() = if (isPlaying) pause() else play()

    fun toggleShuffle() {
        shuffle = !shuffle
    }

    fun cycleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun seekTo(position: Long) {
        positionMs = position.coerceIn(0L, durationMs)
        engine?.engineSeekTo(positionMs)
    }

    fun next() {
        if (shuffle && album.tracks.size > 1) {
            var candidate = trackIndex
            while (candidate == trackIndex) {
                candidate = Random.nextInt(album.tracks.size)
            }
            trackIndex = candidate
            positionMs = 0L
            forwardPlay()
        } else if (trackIndex < album.tracks.lastIndex) {
            trackIndex += 1
            positionMs = 0L
            forwardPlay()
        } else if (repeatMode == RepeatMode.ALL) {
            trackIndex = 0
            positionMs = 0L
            forwardPlay()
        } else {
            positionMs = durationMs
            isPlaying = false
            engine?.enginePause()
            engine?.engineSeekTo(durationMs)
        }
    }

    fun prev() {
        if (positionMs > RESTART_THRESHOLD_MS || trackIndex == 0) {
            positionMs = 0L
        } else {
            trackIndex -= 1
            positionMs = 0L
        }
        forwardPlay()
    }

    /** Cues the current track on the engine; no-op without engine or URI. */
    private fun forwardPlay() {
        val uri = track.uri ?: return
        engine?.enginePlay(trackIndex, uri)
    }

    /** Mirrors an engine-reported natural (gapless) transition. */
    fun syncTrack(index: Int) {
        if (engine == null || index !in album.tracks.indices) return
        trackIndex = index
        positionMs = 0L
    }

    /** Mirrors engine position/play flags from the poll loop. */
    fun syncPlayback(position: Long, playing: Boolean) {
        if (engine == null) return
        positionMs = position.coerceIn(0L, durationMs)
        isPlaying = playing && positionMs < durationMs
    }

    /** Natural playlist end: same policy as running past the last track. */
    fun onPlaylistEnded() {
        if (engine == null) return
        next()
    }

    /**
     * Previews the M6 crossfade target without advancing: the same pick
     * [next] would make, or null when playback would stop. The shuffle
     * pick commits when the fade starts, so a cancelled fade simply
     * discards it.
     */
    fun previewNext(): Int? {
        if (shuffle && album.tracks.size > 1) {
            var candidate = trackIndex
            while (candidate == trackIndex) {
                candidate = Random.nextInt(album.tracks.size)
            }
            return candidate
        }
        if (trackIndex < album.tracks.lastIndex) return trackIndex + 1
        if (repeatMode == RepeatMode.ALL) return 0
        return null
    }

    /** Restore a persisted position (rotation / process death). */
    fun restore(
        track: Int,
        position: Long,
        playing: Boolean,
        shuffleOn: Boolean = false,
        repeat: RepeatMode = RepeatMode.OFF,
    ) {
        trackIndex = track.coerceIn(album.tracks.indices)
        positionMs = position.coerceIn(0L, durationMs)
        isPlaying = playing && positionMs < durationMs
        shuffle = shuffleOn
        repeatMode = repeat
    }

    /** Advance the clock; called by [ticker], rolls over at track end. */
    fun advanceBy(elapsedMs: Long) {
        if (!isPlaying || elapsedMs <= 0L) return
        if (positionMs + elapsedMs < durationMs) {
            positionMs += elapsedMs
        } else if (repeatMode == RepeatMode.ONE) {
            positionMs = 0L
        } else {
            next()
        }
    }

    suspend fun ticker() {
        while (true) {
            delay(TICK_MS)
            advanceBy(TICK_MS)
        }
    }

    companion object {
        const val TICK_MS = 100L
        const val RESTART_THRESHOLD_MS = 3000L
    }
}
