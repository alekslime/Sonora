package com.sonora.nowplaying

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonora.data.Album
import kotlinx.coroutines.delay

/**
 * M2 mock playback state. Real transport semantics — play, pause, seek,
 * next/previous with end-of-track advance — driven by a ticker so the
 * vinyl, tonearm and progress behave like the real engine will in M5.
 * Only the audio output itself is simulated.
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

    val track get() = album.tracks[trackIndex]
    val durationMs: Long get() = track.durationMs
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    fun play() {
        if (positionMs >= durationMs) positionMs = 0L
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
    }

    fun toggle() = if (isPlaying) pause() else play()

    fun seekTo(position: Long) {
        positionMs = position.coerceIn(0L, durationMs)
    }

    fun next() {
        if (trackIndex < album.tracks.lastIndex) {
            trackIndex += 1
            positionMs = 0L
        } else {
            positionMs = durationMs
            isPlaying = false
        }
    }

    fun prev() {
        if (positionMs > RESTART_THRESHOLD_MS || trackIndex == 0) {
            positionMs = 0L
        } else {
            trackIndex -= 1
            positionMs = 0L
        }
    }

    /** Restore a persisted position (rotation / process death). */
    fun restore(track: Int, position: Long, playing: Boolean) {
        trackIndex = track.coerceIn(album.tracks.indices)
        positionMs = position.coerceIn(0L, durationMs)
        isPlaying = playing && positionMs < durationMs
    }

    /** Advance the clock; called by [ticker], rolls over at track end. */
    fun advanceBy(elapsedMs: Long) {
        if (!isPlaying || elapsedMs <= 0L) return
        if (positionMs + elapsedMs < durationMs) {
            positionMs += elapsedMs
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
