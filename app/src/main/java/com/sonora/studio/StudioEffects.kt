package com.sonora.studio

import android.media.audiofx.Equalizer
import kotlin.math.log10
import kotlin.math.pow

/**
 * M5 bridge from [AudioStudioConfig] to the platform [Equalizer] on the
 * player's audio session. The 10-band Studio curve is interpolated onto
 * whatever bands the device exposes (usually five); failures degrade to
 * unprocessed audio, never a crash.
 */
class StudioEffects {

    private var equalizer: Equalizer? = null
    private var sessionId: Int = 0

    /** Attaches to a player audio session. False when effects are unavailable. */
    fun attach(audioSessionId: Int): Boolean {
        if (audioSessionId == sessionId && equalizer != null) return true
        release()
        if (audioSessionId == 0) return false
        return try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            sessionId = audioSessionId
            true
        } catch (_: Exception) {
            release()
            false
        }
    }

    /** Applies the Studio EQ curve. No-op when detached or disabled. */
    fun apply(config: AudioStudioConfig) {
        val eq = equalizer ?: return
        try {
            eq.enabled = config.eqEnabled
            if (!config.eqEnabled) return
            val centersHz = (0 until eq.numberOfBands.toInt()).map {
                eq.getCenterFreq(it.toShort()) / 1000
            }
            val range = eq.bandLevelRange
            val levels = mapBandsToMillibels(config.bands, centersHz, range[0], range[1])
            levels.forEachIndexed { index, millibels ->
                eq.setBandLevel(index.toShort(), millibels)
            }
        } catch (_: Exception) {
            // Leave last-good levels in place.
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
            // Already gone; nothing to do.
        }
        equalizer = null
        sessionId = 0
    }

    companion object {
        /**
         * Interpolates the 10-band dB curve onto device band centers in
         * log-frequency space, returning millibel levels clamped to the
         * device range. Pure and tested.
         */
        fun mapBandsToMillibels(
            configBands: List<Float>,
            deviceCentersHz: List<Int>,
            minMillibels: Short,
            maxMillibels: Short,
        ): ShortArray {
            require(configBands.size == AudioStudioConfig.BAND_COUNT) {
                "Need ${AudioStudioConfig.BAND_COUNT} studio bands"
            }
            val studioHz = AudioStudioConfig.BAND_HZ
            return ShortArray(deviceCentersHz.size) { i ->
                val center = deviceCentersHz[i].coerceAtLeast(1)
                val db = interpolateDb(studioHz, configBands, center)
                (db * 100).toInt().coerceIn(minMillibels.toInt(), maxMillibels.toInt()).toShort()
            }
        }

        private fun interpolateDb(
            studioHz: List<Int>,
            configBands: List<Float>,
            centerHz: Int,
        ): Float {
            if (centerHz <= studioHz.first()) return configBands.first()
            if (centerHz >= studioHz.last()) return configBands.last()
            val high = studioHz.indexOfFirst { it >= centerHz }
            val low = high - 1
            val lowLog = log10(studioHz[low].toFloat())
            val highLog = log10(studioHz[high].toFloat())
            val centerLog = log10(centerHz.toFloat())
            val t = (centerLog - lowLog) / (highLog - lowLog)
            return configBands[low] + (configBands[high] - configBands[low]) * t
        }

        /**
         * Preamp dB to linear player gain (10^(dB/20)); 0 dB is unity.
         * Pure and tested.
         */
        fun preampToGain(db: Float): Float {
            return 10f.pow(db / 20f).coerceIn(0f, 4f)
        }
    }
}
