package com.sonora.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * M10 Scope: a phosphor Lissajous trace of the waveform in quadrature,
 * with echo persistence. The line IS the music; echoes sell motion.
 */
@Composable
fun ScopeViz(
    signal: VizSignal,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    modifier: Modifier = Modifier,
) {
    val history = remember { ArrayDeque<FloatArray>() }
    Canvas(modifier.fillMaxSize()) {
        drawRect(preset.deep)
        val wave = signal.frame.waveform
        if (wave.size >= 2) {
            history.addLast(wave.copyOf())
            while (history.size > 3) history.removeFirst()
        }
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.46f)
        // Faint crosshair seats the trace.
        drawLine(
            Color.White.copy(alpha = 0.07f),
            Offset(center.x - minOf(w, h) * 0.42f, center.y),
            Offset(center.x + minOf(w, h) * 0.42f, center.y),
            strokeWidth = 2f,
        )
        drawLine(
            Color.White.copy(alpha = 0.07f),
            Offset(center.x, center.y - minOf(w, h) * 0.42f),
            Offset(center.x, center.y + minOf(w, h) * 0.42f),
            strokeWidth = 2f,
        )
        val frames = history.toList()
        for (e in frames.indices) {
            val age = frames.size - 1 - e
            val alpha = when (age) {
                0 -> 0.95f
                1 -> 0.35f
                else -> 0.15f
            }
            val color = if (age == 0) preset.accent else vizLineAlt(preset)
            drawLissajous(frames[e], center, minOf(w, h), intensity, color.copy(alpha = alpha))
        }
        val pulse = signal.bands.pulse
        if (pulse > 0.03f) {
            drawCircle(
                Color.White.copy(alpha = 0.30f * pulse),
                radius = minOf(w, h) * 0.40f,
                center = center,
                style = Stroke(width = 3f),
            )
        }
        vizVignette()
    }
}

private fun DrawScope.drawLissajous(
    wave: FloatArray,
    center: Offset,
    unit: Float,
    intensity: Float,
    color: Color,
) {
    if (wave.size < 2) return
    val r = unit * 0.36f * (0.5f + 0.5f * intensity)
    // Eighth-window quadrature: a half-window offset collapses
    // harmonic waves onto a diagonal, this stays an open figure.
    val quad = (wave.size / 8).coerceAtLeast(1)
    val path = Path()
    for (i in wave.indices) {
        val x = center.x + wave[i] * r
        val y = center.y + wave[(i + quad) % wave.size] * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color.copy(alpha = color.alpha * 0.25f), style = Stroke(width = 14f))
    drawPath(path, color, style = Stroke(width = 4f))
}
