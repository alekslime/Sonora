package com.sonora.studio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val EQ_BAND_COUNT = 10
const val EQ_MIN_DB = -12f
const val EQ_MAX_DB = 12f
const val PREAMP_MIN_DB = -12f
const val PREAMP_MAX_DB = 12f
const val CROSSFADE_MIN_SEC = 2
const val CROSSFADE_MAX_SEC = 12

/** Standard ten-band center frequencies and their short labels. */
val EQ_FREQUENCY_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

enum class EqPreset { FLAT, BASS_BOOST, TREBLE_BOOST, WARM, VOCAL, CUSTOM }

enum class ReplayGainMode { OFF, TRACK, ALBUM }

/** Factory EQ curves in dB, band order following [EQ_FREQUENCY_LABELS]. */
val EQ_FACTORY_PRESETS: Map<EqPreset, List<Float>> = mapOf(
    EqPreset.FLAT to List(EQ_BAND_COUNT) { 0f },
    EqPreset.BASS_BOOST to listOf(7f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
    EqPreset.TREBLE_BOOST to listOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 2.5f, 4f, 5.5f),
    EqPreset.WARM to listOf(3f, 2.5f, 1.5f, 0.5f, 0f, -0.5f, -1f, -1.5f, -2f, -2.5f),
    EqPreset.VOCAL to listOf(-1.5f, -1f, 0f, 1.5f, 3f, 3f, 2f, 1f, 0f, -1f),
)

/**
 * M4 Audio Studio configuration. Gapless defaults on, crossfade defaults
 * off, ReplayGain defaults off (never invent gain without file tags).
 * Applied to real playback by the M5 audio engine.
 */
data class AudioStudioConfig(
    val eqEnabled: Boolean = true,
    val preset: EqPreset = EqPreset.FLAT,
    val bandGainsDb: List<Float> = List(EQ_BAND_COUNT) { 0f },
    val customGainsDb: List<Float> = List(EQ_BAND_COUNT) { 0f },
    val preampDb: Float = 0f,
    val replayGain: ReplayGainMode = ReplayGainMode.OFF,
    val gapless: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSec: Int = 6,
)

/** In-memory M4 store; M5 backs it with persistent preferences. */
object AudioStudioStore {
    var config by mutableStateOf(AudioStudioConfig())
        private set

    fun setEqEnabled(enabled: Boolean) {
        config = config.copy(eqEnabled = enabled)
    }

    fun setPreset(preset: EqPreset) {
        val gains = if (preset == EqPreset.CUSTOM) {
            config.customGainsDb
        } else {
            EQ_FACTORY_PRESETS.getValue(preset)
        }
        config = config.copy(preset = preset, bandGainsDb = gains)
    }

    /** Any manual fader move selects the Custom curve and records it. */
    fun setBand(index: Int, gainDb: Float) {
        if (index !in 0 until EQ_BAND_COUNT) return
        val clamped = gainDb.coerceIn(EQ_MIN_DB, EQ_MAX_DB)
        val bands = config.bandGainsDb.toMutableList().also { it[index] = clamped }
        config = config.copy(preset = EqPreset.CUSTOM, bandGainsDb = bands, customGainsDb = bands)
    }

    fun setPreamp(gainDb: Float) {
        config = config.copy(preampDb = gainDb.coerceIn(PREAMP_MIN_DB, PREAMP_MAX_DB))
    }

    fun setReplayGain(mode: ReplayGainMode) {
        config = config.copy(replayGain = mode)
    }

    fun setGapless(gapless: Boolean) {
        config = config.copy(gapless = gapless)
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        config = config.copy(crossfadeEnabled = enabled)
    }

    fun setCrossfadeSec(seconds: Int) {
        config = config.copy(crossfadeSec = seconds.coerceIn(CROSSFADE_MIN_SEC, CROSSFADE_MAX_SEC))
    }
}
