package com.sonora.liquid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * M9 Polar Scope: concentric waveform rings traced in polar
 * coordinates. Bass breathes the radius, mid bends the lobes, treble
 * jitters the rim, and the kick fires the center dot.
 */
fun DrawScope.drawPolar(
    bands: Bands,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    quality: LiquidQuality,
) {
    drawRect(preset.deep)
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h * 0.46f)
    val unit = minOf(w, h)
    val rings = when (quality) {
        LiquidQuality.LOW -> 2
        LiquidQuality.MEDIUM -> 3
        LiquidQuality.HIGH -> 4
    }
    val breathe = 1f + bands.bass * 0.38f * intensity
    for (k in 0 until rings) {
        val base = unit * (0.16f + 0.085f * k) * breathe
        val path = Path()
        val steps = 140
        for (s in 0..steps) {
            val a = s.toFloat() / steps * 2f * PI.toFloat()
            val r = base * (
                1f +
                    0.24f * sin(3f * a + timeSec * (2f + k * 0.7f)) * bands.mid +
                    0.12f * sin(8f * a - timeSec * 4.3f + k) * bands.treble +
                    0.10f * sin(2f * a + timeSec * 9f) * bands.pulse
                )
            val x = center.x + cos(a) * r
            val y = center.y + sin(a) * r
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val color = if (k % 2 == 0) preset.accent else vizLineAlt(preset)
        if (k == 0) {
            drawPath(path, color.copy(alpha = 0.15f), style = Stroke(width = 14f))
        }
        drawPath(path, color.copy(alpha = 0.85f), style = Stroke(width = 4f))
    }
    // Radial ticks with deterministic treble jitter.
    val ticks = 28
    val tickBase = unit * 0.085f * breathe
    for (i in 0 until ticks) {
        val a = i.toFloat() / ticks * 2f * PI.toFloat() + timeSec * 0.12f
        val jitter = 0.5f + 0.5f * sin(i * 12.9f + timeSec * 13f)
        val len = unit * (0.012f + 0.035f * bands.treble * jitter)
        val dir = Offset(cos(a), sin(a))
        drawLine(
            preset.accent.copy(alpha = 0.5f),
            center + dir * tickBase,
            center + dir * (tickBase + len),
            strokeWidth = 3f,
        )
    }
    if (bands.pulse > 0.03f) {
        drawCircle(
            Color.White.copy(alpha = 0.8f * bands.pulse),
            radius = unit * 0.014f * (1f + bands.pulse),
            center = center,
        )
    }
    vizVignette()
}
