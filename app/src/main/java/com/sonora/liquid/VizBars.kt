package com.sonora.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bars around the ring. */
const val VIZ_BAR_COUNT = 48

/**
 * Downsamples a spectrum to [count] peak-per-span values. Pure.
 */
fun downsampleSpectrum(spectrum: FloatArray, count: Int): FloatArray {
    val out = FloatArray(count.coerceAtLeast(0))
    if (spectrum.isEmpty() || count <= 0) return out
    for (i in 0 until count) {
        val from = (i.toFloat() / count * spectrum.size).toInt().coerceIn(0, spectrum.size - 1)
        val to = ((i + 1).toFloat() / count * spectrum.size).toInt().coerceIn(from + 1, spectrum.size)
        var peak = 0f
        for (s in from until to) peak = maxOf(peak, spectrum[s])
        out[i] = peak
    }
    return out
}

/**
 * Peak-hold caps: instant attack, linear fall back toward the live
 * value. Mutates [peaks] in place. Pure.
 */
fun stepPeaks(peaks: FloatArray, values: FloatArray, dtSec: Float, fallPerSec: Float = 0.9f) {
    if (dtSec <= 0f) return
    for (i in peaks.indices) {
        val v = values.getOrElse(i) { 0f }
        peaks[i] = maxOf(v, peaks[i] - fallPerSec * dtSec)
    }
}

/**
 * M10 Bars: the Alchemy homage. A ring of spectrum bars with
 * peak-hold caps — the fastest possible read of reactivity.
 */
@Composable
fun BarsViz(
    signal: VizSignal,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    modifier: Modifier = Modifier,
) {
    val peaks = remember { FloatArray(VIZ_BAR_COUNT) }
    val lastT = remember { mutableFloatStateOf(timeSec) }
    Canvas(modifier.fillMaxSize()) {
        val dt = (timeSec - lastT.floatValue).coerceAtLeast(0f)
        lastT.floatValue = timeSec
        drawRect(preset.deep)
        val values = downsampleSpectrum(signal.frame.spectrum, VIZ_BAR_COUNT)
        stepPeaks(peaks, values, dt)
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.46f)
        val unit = minOf(w, h)
        val baseR = unit * 0.20f
        val range = unit * 0.22f * (0.4f + 0.6f * intensity)
        val barWidth = (2f * PI.toFloat() * baseR / VIZ_BAR_COUNT) * 0.55f
        for (i in 0 until VIZ_BAR_COUNT) {
            val a = i.toFloat() / VIZ_BAR_COUNT * 2f * PI.toFloat() - PI.toFloat() / 2f
            val dir = Offset(cos(a), sin(a))
            val v = values[i]
            val start = center + dir * baseR
            val end = center + dir * (baseR + v * range)
            drawLine(
                preset.accent.copy(alpha = 0.25f + 0.65f * v),
                start,
                end,
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
            val capR = baseR + peaks[i] * range
            drawLine(
                Color.White.copy(alpha = 0.85f),
                center + dir * (capR - barWidth * 0.4f),
                center + dir * (capR + barWidth * 0.4f),
                strokeWidth = barWidth * 0.45f,
                cap = StrokeCap.Round,
            )
        }
        vizVignette()
    }
}
