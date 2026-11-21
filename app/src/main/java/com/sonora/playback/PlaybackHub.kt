package com.sonora.playback

import com.sonora.liquid.Bands
import com.sonora.liquid.VizSignal
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

    private val _vizSignal = MutableStateFlow<VizSignal?>(null)
    val vizSignal: StateFlow<VizSignal?> = _vizSignal.asStateFlow()

    private val _vizActive = MutableStateFlow(false)
    val vizActive: StateFlow<Boolean> = _vizActive.asStateFlow()

    private val _format = MutableStateFlow<TrackFormat?>(null)
    val format: StateFlow<TrackFormat?> = _format.asStateFlow()

    private val _naturalTransitions = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val naturalTransitions: SharedFlow<Int> = _naturalTransitions.asSharedFlow()

    private val _playlistEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playlistEnded: SharedFlow<Unit> = _playlistEnded.asSharedFlow()

    private val _skippedTrack = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val skippedTrack: SharedFlow<String> = _skippedTrack.asSharedFlow()

    /** Clears sticky state for a fresh session. Main-thread only. */
    fun reset() {
        _positionMs.value = 0L
        _isPlaying.value = false
        _audioSessionId.value = 0
        _bands.value = Bands.Ambient
        _vizSignal.value = null
        _vizActive.value = false
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

    fun publishVizSignal(value: VizSignal) {
        _vizSignal.value = value
    }

    fun publishVizActive(value: Boolean) {
        _vizActive.value = value
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

    fun emitSkipped(title: String) {
        _skippedTrack.tryEmit(title)
    }
}
