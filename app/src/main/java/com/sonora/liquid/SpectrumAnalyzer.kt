package com.sonora.liquid

import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * M5 real spectrum, M10 signal: feeds Liquid from the player's audio
 * session via the platform [Visualizer]. Attaching to our own session
 * needs no permission. The service drives [ensureAttached] from its
 * poll loop, so a failed attach or a stalled capture stream retries
 * instead of stranding the visualizer on ambient levels; [active] and
 * the capture clock stay visible for the settings readout.
 */
class SpectrumAnalyzer {

    var bands by mutableStateOf(Bands.Ambient)
        private set

    /** Latest full signal (bands plus waveform and spectrum). */
    var signal by mutableStateOf<VizSignal?>(null)
        private set

    var active by mutableStateOf(false)
        private set

    private var visualizer: Visualizer? = null
    private var sessionId: Int = 0
    private var bassEma: Float = 0f
    private var peak: Float = 0f
    private var pendingWaveform: ByteArray? = null
    private var lastCaptureMs = 0L
    private var lastAttemptMs = 0L

    fun attach(audioSessionId: Int) {
        if (audioSessionId == sessionId && visualizer != null) return
        release()
        if (audioSessionId == 0) return
        lastAttemptMs = SystemClock.elapsedRealtime()
        try {
            val viz = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            viz.captureSize = range[1].coerceAtMost(MAX_CAPTURE)
            val rate = minOf(CAPTURE_RATE_HZ * 1000, Visualizer.getMaxCaptureRate())
            viz.setDataCaptureListener(captureListener, rate, true, true)
            viz.enabled = true
            visualizer = viz
            sessionId = audioSessionId
            lastCaptureMs = lastAttemptMs
            active = true
        } catch (_: Exception) {
            release()
        }
    }

    /**
     * Self-heal for the poll loop: follows session changes, drops a
     * dead session, and re-attaches when captures stall. Retries are
     * throttled so a hard failure costs one attempt per window.
     */
    fun ensureAttached(audioSessionId: Int, nowMs: Long) {
        if (audioSessionId == 0) {
            if (visualizer != null) release()
            return
        }
        if (audioSessionId != sessionId || visualizer == null) {
            if (nowMs - lastAttemptMs >= RETRY_MS) attach(audioSessionId)
            return
        }
        if (active && nowMs - lastCaptureMs > STALE_MS &&
            nowMs - lastAttemptMs >= RETRY_MS
        ) {
            attach(audioSessionId)
        }
    }

    fun release() {
        active = false
        bassEma = 0f
        peak = 0f
        bands = Bands.Ambient
        signal = null
        pendingWaveform = null
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
            // Stashed for the next FFT; the array is only valid in here.
            pendingWaveform = waveform?.copyOf()
            lastCaptureMs = SystemClock.elapsedRealtime()
        }

        override fun onFftDataCapture(
            visualizer: Visualizer?,
            fft: ByteArray?,
            samplingRate: Int,
        ) {
            if (fft == null) return
            val nowMs = SystemClock.elapsedRealtime()
            val frame = fftToBands(fft, bassEma, peak)
            bassEma = frame.bassEma
            peak = frame.peak
            bands = frame.bands
            val wave = pendingWaveform?.let { waveformToFloat(it, WAVEFORM_POINTS) }
                ?: FloatArray(WAVEFORM_POINTS)
            pendingWaveform = null
            lastCaptureMs = nowMs
            signal = VizSignal(
                bands = frame.bands,
                frame = VizFrame(wave, fftToSpectrum(fft, SPECTRUM_BINS), nowMs),
            )
        }
    }

    /** One analyzed frame: bands plus the carried-forward detector state. */
    data class Frame(val bands: Bands, val bassEma: Float, val peak: Float)

    companion object {
        const val CAPTURE_RATE_HZ = 30
        const val MAX_CAPTURE = 1024

        /** Silence this long on an attached session triggers a re-attach. */
        const val STALE_MS = 2000L

        /** Minimum gap between attach attempts. */
        const val RETRY_MS = 2000L

        /**
         * Folds an interleaved real/imag FFT into [Bands]. Levels
         * self-calibrate: [peak] tracks the recent maximum with instant
         * attack and slow decay, so quiet and loud masters alike fill the
         * visual range without any fixed reference. [bassEma] feeds onset
         * (pulse) detection. Pure and tested.
         */
        fun fftToBands(fft: ByteArray, bassEma: Float, peak: Float): Frame {
            val binCount = fft.size / 2
            if (binCount < 8) return Frame(Bands.Ambient, bassEma, peak)
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
            val frameMax = maxOf(bass, mid, treble)
            val newPeak = maxOf(frameMax, peak * PEAK_DECAY)
            val reference = maxOf(newPeak, PEAK_FLOOR)
            val newEma = bassEma + (bass - bassEma) * EMA_ALPHA
            return Frame(
                Bands(
                    bass = perceptual(bass, reference),
                    mid = perceptual(mid, reference),
                    treble = perceptual(treble, reference),
                    pulse = ((bass - bassEma).coerceAtLeast(0f) * PULSE_GAIN).coerceIn(0f, 1f),
                ),
                newEma,
                newPeak,
            )
        }

        /**
         * Folds an interleaved FFT into [bins] 0..1 magnitudes for the
         * bar renderer. Spans grow quadratically (musical spacing, DC
         * skipped), each span reports its peak, and the frame
         * normalizes against its own maximum so quiet masters still
         * dance. Pure and tested.
         */
        fun fftToSpectrum(fft: ByteArray, bins: Int): FloatArray {
            val out = FloatArray(bins.coerceAtLeast(0))
            val binCount = fft.size / 2
            if (binCount < 8 || bins <= 0) return out
            val magnitudes = FloatArray(binCount) { i ->
                val re = fft[2 * i].toInt()
                val im = fft[2 * i + 1].toInt()
                sqrt((re * re + im * im).toFloat()) / binCount
            }
            var peak = 0f
            for (m in magnitudes) peak = maxOf(peak, m)
            val reference = maxOf(peak, SPECTRUM_FLOOR)
            for (b in 0 until bins) {
                val from = ((b.toFloat() / bins).pow(2.2f) * (binCount - 1)).toInt()
                    .coerceIn(1, binCount - 1)
                val to = (((b + 1).toFloat() / bins).pow(2.2f) * (binCount - 1)).toInt()
                    .coerceIn(from + 1, binCount)
                var spanPeak = 0f
                for (i in from until to) spanPeak = maxOf(spanPeak, magnitudes[i])
                out[b] = sqrt((spanPeak / reference).coerceIn(0f, 1f))
            }
            return out
        }

        /**
         * Resamples raw Visualizer waveform bytes (unsigned, 128 center)
         * to [points] -1..1 samples with linear interpolation. Pure.
         */
        fun waveformToFloat(bytes: ByteArray, points: Int): FloatArray {
            val out = FloatArray(points.coerceAtLeast(0))
            if (bytes.isEmpty() || points <= 0) return out
            if (points == 1) {
                out[0] = centered(bytes[bytes.size / 2])
                return out
            }
            for (p in 0 until points) {
                val pos = p.toFloat() / (points - 1) * (bytes.size - 1)
                val i = pos.toInt().coerceIn(0, bytes.size - 1)
                val j = (i + 1).coerceAtMost(bytes.size - 1)
                val f = (pos - i).coerceIn(0f, 1f)
                out[p] = centered(bytes[i]) * (1f - f) + centered(bytes[j]) * f
            }
            return out
        }

        private fun centered(sample: Byte): Float {
            return ((sample.toInt() and 0xFF) - 128) / 128f
        }

        private fun mean(values: FloatArray, from: Int, to: Int): Float {
            if (to <= from) return 0f
            var sum = 0f
            for (i in from until to) sum += values[i]
            return sum / (to - from)
        }

        /** Compresses raw magnitude against the running peak into 0..1. */
        private fun perceptual(magnitude: Float, reference: Float): Float {
            return sqrt((magnitude / reference).coerceIn(0f, 1f))
        }

        private const val EMA_ALPHA = 0.2f
        private const val PULSE_GAIN = 3f
        private const val PEAK_DECAY = 0.98f
        private const val PEAK_FLOOR = 0.5f
        private const val SPECTRUM_FLOOR = 0.015f
    }
}
