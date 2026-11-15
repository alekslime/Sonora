package com.sonora.playback

import com.sonora.liquid.Bands
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M6 same-process event bus. The service owns the players and publishes
 * engine truth here; the orchestrator mirrors it into the session state.
 * Sticky state resets on every new queue so a fresh session never replays
 * the previous album's tail.
 */
object PlaybackHub {

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _bands = MutableStateFlow(Bands.Ambient)
    val bands: StateFlow<Bands> = _bands.asStateFlow()

    private val _format = MutableStateFlow<TrackFormat?>(null)
    val format: StateFlow<TrackFormat?> = _format.asStateFlow()

    private val _naturalTransitions = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val naturalTransitions: SharedFlow<Int> = _naturalTransitions.asSharedFlow()

    private val _playlistEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playlistEnded: SharedFlow<Unit> = _playlistEnded.asSharedFlow()

    /** Clears sticky state for a fresh session. Main-thread only. */
    fun reset() {
        _positionMs.value = 0L
        _isPlaying.value = false
        _audioSessionId.value = 0
        _bands.value = Bands.Ambient
        _format.value = null
    }

    fun publishPosition(value: Long) {
        _positionMs.value = value
    }

    fun publishPlaying(value: Boolean) {
        _isPlaying.value = value
    }

    fun publishAudioSession(id: Int) {
        _audioSessionId.value = id
    }

    fun publishBands(value: Bands) {
        _bands.value = value
    }

    fun publishFormat(value: TrackFormat?) {
        _format.value = value
    }

    fun emitTransition(index: Int) {
        _naturalTransitions.tryEmit(index)
    }

    fun emitEnded() {
        _playlistEnded.tryEmit(Unit)
    }
}
