package com.sonora.liquid

import android.media.audiofx.Visualizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.sqrt

/**
 * M5 real spectrum: feeds Liquid from the player's audio session via the
 * platform [Visualizer]. Attaching to our own session needs no permission;
 * anything that fails (missing effect, dead session) degrades to [active]
 * being false, in which case the overlay keeps using [MockSpectrum].
 */
class SpectrumAnalyzer {

    var bands by mutableStateOf(Bands.Ambient)
        private set

    var active by mutableStateOf(false)
        private set

    private var visualizer: Visualizer? = null
    private var sessionId: Int = 0
    private var bassEma: Float = 0f

    fun attach(audioSessionId: Int) {
        if (audioSessionId == sessionId && visualizer != null) return
        release()
        if (audioSessionId == 0) return
        try {
            val viz = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            viz.captureSize = range[1].coerceAtMost(MAX_CAPTURE)
            val rate = minOf(CAPTURE_RATE_HZ * 1000, Visualizer.getMaxCaptureRate())
            viz.setDataCaptureListener(captureListener, rate, false, true)
            viz.enabled = true
            visualizer = viz
            sessionId = audioSessionId
            active = true
        } catch (_: Exception) {
            release()
        }
    }

    fun release() {
        active = false
        bassEma = 0f
        bands = Bands.Ambient
        try {
            visualizer?.release()
        } catch (_: Exception) {
            // Already gone; nothing to do.
        }
        visualizer = null
        sessionId = 0
    }

    private val captureListener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(
            visualizer: Visualizer?,
            waveform: ByteArray?,
            samplingRate: Int,
        ) {
            // FFT only; waveform unused.
        }

        override fun onFftDataCapture(
            visualizer: Visualizer?,
            fft: ByteArray?,
            samplingRate: Int,
        ) {
            if (fft == null) return
            val (next, ema) = fftToBands(fft, bassEma)
            bassEma = ema
            bands = next
        }
    }

    companion object {
        const val CAPTURE_RATE_HZ = 20
        const val MAX_CAPTURE = 1024

        /**
         * Folds an interleaved real/imag FFT into [Bands], with [bassEma] as
         * the running bass average for onset (pulse) detection. Returns the
         * bands plus the updated EMA. Pure and tested.
         */
        fun fftToBands(fft: ByteArray, bassEma: Float): Pair<Bands, Float> {
            val binCount = fft.size / 2
            if (binCount < 8) return Pair(Bands.Ambient, bassEma)
            val magnitudes = FloatArray(binCount) { i ->
                val re = fft[2 * i].toInt()
                val im = fft[2 * i + 1].toInt()
                sqrt((re * re + im * im).toFloat()) / binCount
            }
            // Log-ish thirds of the spectrum, skipping the DC bin.
            val bassEnd = maxOf(2, binCount / 16)
            val midEnd = maxOf(bassEnd + 1, binCount / 4)
            val bass = mean(magnitudes, 1, bassEnd)
            val mid = mean(magnitudes, bassEnd, midEnd)
            val treble = mean(magnitudes, midEnd, binCount)
            val newEma = bassEma + (bass - bassEma) * EMA_ALPHA
            return Pair(
                Bands(
                    bass = perceptual(bass),
                    mid = perceptual(mid),
                    treble = perceptual(treble),
                    pulse = ((bass - bassEma).coerceAtLeast(0f) * PULSE_GAIN).coerceIn(0f, 1f),
                ),
                newEma,
            )
        }

        private fun mean(values: FloatArray, from: Int, to: Int): Float {
            if (to <= from) return 0f
            var sum = 0f
            for (i in from until to) sum += values[i]
            return sum / (to - from)
        }

        /** Compresses raw magnitude into a 0..1 visual level. */
        private fun perceptual(magnitude: Float): Float {
            return sqrt((magnitude / REFERENCE).coerceIn(0f, 1f))
        }

        private const val REFERENCE = 8f
        private const val EMA_ALPHA = 0.2f
        private const val PULSE_GAIN = 3f
    }
}
