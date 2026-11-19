package com.sonora.liquid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * M10 Waves: the Battery/Ambience homage. The live waveform sweeps the
 * screen with its mirror reflection; the kick flashes a white core.
 * Pure signal — no time-driven motion at all.
 */
fun DrawScope.drawWaves(
    signal: VizSignal,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    quality: LiquidQuality,
) {
    drawRect(preset.deep)
    val wave = signal.frame.waveform
    val w = size.width
    val h = size.height
    val centerY = h * 0.46f
    drawLine(
        Color.White.copy(alpha = 0.10f),
        Offset(0f, centerY),
        Offset(w, centerY),
        strokeWidth = 2f,
    )
    if (wave.size < 2) {
        vizVignette()
        return
    }
    val amp = h * 0.30f * (0.35f + 0.65f * intensity)
    val steps = when (quality) {
        LiquidQuality.LOW -> 64
        LiquidQuality.MEDIUM -> 96
        LiquidQuality.HIGH -> wave.size
    }
    // Upper voice and its dimmer mirror.
    listOf(1f to 1f, -1f to 0.45f).forEach { (mirror, gain) ->
        val path = Path()
        for (s in 0..steps) {
            val p = s.toFloat() / steps
            val sample = sampleWave(wave, p)
            val x = p * w
            val y = centerY + mirror * sample * amp
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, preset.accent.copy(alpha = 0.20f * gain), style = Stroke(width = 22f))
        drawPath(path, preset.accent.copy(alpha = 0.9f * gain), style = Stroke(width = 5f))
    }
    val pulse = signal.bands.pulse
    if (pulse > 0.03f) {
        val flash = Path()
        for (s in 0..steps) {
            val p = s.toFloat() / steps
            val x = p * w
            val y = centerY + sampleWave(wave, p) * amp
            if (s == 0) flash.moveTo(x, y) else flash.lineTo(x, y)
        }
        drawPath(flash, Color.White.copy(alpha = 0.6f * pulse), style = Stroke(width = 2.5f))
    }
    vizVignette()
}

private fun sampleWave(wave: FloatArray, p: Float): Float {
    val pos = p.coerceIn(0f, 1f) * (wave.size - 1)
    val i = pos.toInt().coerceIn(0, wave.size - 1)
    val j = (i + 1).coerceAtMost(wave.size - 1)
    val f = (pos - i).coerceIn(0f, 1f)
    return wave[i] * (1f - f) + wave[j] * f
}
