package com.sonora.liquid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * M9 Star: a five-lobed Lissajous outline with motion echoes. Bass
 * pumps its reach, mid turns it, treble roughens the rim, and two
 * trailing phases sell the movement.
 */
fun DrawScope.drawStar(
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
    val echoes = when (quality) {
        LiquidQuality.LOW -> 1
        LiquidQuality.MEDIUM -> 2
        LiquidQuality.HIGH -> 3
    }
    val reach = unit * 0.36f * (1f + bands.bass * 0.30f * intensity)
    val spin = timeSec * 0.25f + bands.mid * 0.8f
    for (e in echoes - 1 downTo 0) {
        val te = timeSec - e * 0.09f
        val path = Path()
        val steps = 160
        for (s in 0..steps) {
            val a = s.toFloat() / steps * 2f * PI.toFloat()
            val lobe = abs(sin(2.5f * a + te * 0.9f))
            val wobble = 0.55f + 0.45f * lobe
            val r = reach * wobble * (1f + 0.08f * sin(6f * a + te * 3f) * bands.treble)
            val a2 = a + spin + e * 0.02f
            val x = center.x + cos(a2) * r
            val y = center.y + sin(a2) * r
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val alpha = when (e) {
            0 -> 0.9f
            1 -> 0.35f
            else -> 0.15f
        }
        val color = if (e == 0) preset.accent else vizLineAlt(preset)
        drawPath(path, color.copy(alpha = alpha), style = Stroke(width = 4.5f))
    }
    vizVignette()
}
