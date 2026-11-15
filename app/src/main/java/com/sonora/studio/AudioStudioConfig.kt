package com.sonora.studio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Normalization mode. Applied only when the metadata exists (M5/M6). */
enum class ReplayGainMode { OFF, TRACK, ALBUM }

/** A named ten-band curve. */
data class EqPreset(val name: String, val bands: List<Float>) {
    init {
        require(bands.size == AudioStudioConfig.BAND_COUNT) { "EQ presets need 10 bands" }
    }
}

/**
 * M4 audio configuration model. UI-adjustable now; the M5/M6 engine reads
 * it to shape real audio. All dB values clamp to [-12, +12].
 */
data class AudioStudioConfig(
    val eqEnabled: Boolean = true,
    val bands: List<Float> = List(BAND_COUNT) { 0f },
    val presetName: String = "Flat",
    val preampDb: Float = 0f,
    val replayGain: ReplayGainMode = ReplayGainMode.OFF,
    val gapless: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSecs: Int = 4,
) {
    init {
        require(bands.size == BAND_COUNT) { "EQ needs 10 bands" }
    }

    companion object {
        const val BAND_COUNT = 10
        const val MIN_DB = -12f
        const val MAX_DB = 12f
        const val MIN_CROSSFADE_SECS = 1
        const val MAX_CROSSFADE_SECS = 12

        /** Center frequencies in Hz, low to high. */
        val BAND_HZ = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        val BUILT_IN_PRESETS = listOf(
            EqPreset("Flat", List(BAND_COUNT) { 0f }),
            EqPreset("Bass Boost", listOf(5f, 4f, 3f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqPreset("Treble Boost", listOf(0f, 0f, 0f, 0f, 0f, 0f, 1.5f, 3f, 4f, 5f)),
            EqPreset("Vocal", listOf(-1f, -1f, 0f, 1f, 3f, 3f, 2f, 0f, -1f, -1f)),
            EqPreset("Electronic", listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 4f)),
        )

        fun clampDb(value: Float): Float = value.coerceIn(MIN_DB, MAX_DB)

        /**
         * Effective crossfade at a track boundary. Never crossfades into a
         * gapless boundary (true gapless album playback stays intact) or
         * when the listener disabled crossfade.
         */
        fun effectiveCrossfadeMs(config: AudioStudioConfig, gaplessBoundary: Boolean): Long {
            if (!config.crossfadeEnabled || gaplessBoundary) return 0L
            return config.crossfadeSecs.coerceIn(MIN_CROSSFADE_SECS, MAX_CROSSFADE_SECS) * 1000L
        }
    }
}

/** In-memory M4 store; M5 backs it with persistent preferences. */
object AudioStudioStore {
    var config by mutableStateOf(AudioStudioConfig())
        private set

    fun setEqEnabled(enabled: Boolean) {
        config = config.copy(eqEnabled = enabled)
    }

    fun setBand(index: Int, valueDb: Float) {
        if (index !in 0 until AudioStudioConfig.BAND_COUNT) return
        val bands = config.bands.toMutableList()
        bands[index] = AudioStudioConfig.clampDb(valueDb)
        config = config.copy(bands = bands, presetName = "Custom")
    }

    fun applyPreset(preset: EqPreset) {
        config = config.copy(
            bands = preset.bands.map { AudioStudioConfig.clampDb(it) },
            presetName = preset.name,
        )
    }

    fun setPreamp(valueDb: Float) {
        config = config.copy(preampDb = AudioStudioConfig.clampDb(valueDb))
    }

    fun setReplayGain(mode: ReplayGainMode) {
        config = config.copy(replayGain = mode)
    }

    fun setGapless(enabled: Boolean) {
        config = config.copy(gapless = enabled)
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        config = config.copy(crossfadeEnabled = enabled)
    }

    fun setCrossfadeSecs(secs: Int) {
        config = config.copy(
            crossfadeSecs = secs.coerceIn(
                AudioStudioConfig.MIN_CROSSFADE_SECS,
                AudioStudioConfig.MAX_CROSSFADE_SECS,
            ),
        )
    }
}
