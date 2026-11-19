package com.sonora.liquid

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/**
 * M9 Ribbons: flowing waveform strands in the Windows Media Player
 * spirit. Each strand rides its own band — bass swells the lowest,
 * treble shimmers the highest — and the kick flashes a white core.
 */
fun DrawScope.drawRibbons(
    bands: Bands,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    quality: LiquidQuality,
) {
    drawRect(preset.deep)
    val w = size.width
    val h = size.height
    val strands = when (quality) {
        LiquidQuality.LOW -> 3
        LiquidQuality.MEDIUM -> 4
        LiquidQuality.HIGH -> 5
    }
    val energy = floatArrayOf(bands.bass, bands.mid, bands.treble, bands.pulse, (bands.bass + bands.mid) / 2f)
    for (i in 0 until strands) {
        val e = energy[i % energy.size]
        val baseY = h * (0.26f + 0.12f * i)
        val amp = (16f + h * 0.09f * e) * (0.45f + 0.55f * intensity)
        val freq = 0.9f + i * 0.55f
        val speed = (1.4f + i * 0.5f) * (0.5f + e)
        val phase = i * 1.7f
        val path = Path()
        val steps = 56
        for (s in 0..steps) {
            val p = s.toFloat() / steps
            val x = -40f + (w + 80f) * p
            val y = baseY +
                sin(p * freq * 2f * PI.toFloat() + timeSec * speed + phase) * amp +
                sin(p * freq * 5.1f * PI.toFloat() - timeSec * speed * 1.7f) * amp * 0.22f * bands.treble
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val color = if (i % 2 == 0) preset.accent else vizLineAlt(preset)
        drawPath(path, color.copy(alpha = 0.22f), style = Stroke(width = 26f))
        drawPath(path, color.copy(alpha = 0.9f), style = Stroke(width = 6f))
    }
    if (bands.pulse > 0.03f) {
        val flash = Path()
        val baseY = h * 0.5f
        val steps = 56
        for (s in 0..steps) {
            val p = s.toFloat() / steps
            val x = -40f + (w + 80f) * p
            val y = baseY + sin(p * 12.6f + timeSec * 6f) * h * 0.05f * bands.pulse
            if (s == 0) flash.moveTo(x, y) else flash.lineTo(x, y)
        }
        drawPath(flash, Color.White.copy(alpha = 0.55f * bands.pulse), style = Stroke(width = 3f))
    }
    vizVignette()
}
