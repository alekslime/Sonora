package com.sonora.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.math.sin

/**
 * Deterministic stand-in for audio energy until M5 wires the real signal:
 * a slow composite wave while playing, a faint idle otherwise.
 */
fun mockEnergy(positionMs: Long, playing: Boolean): Float {
    if (!playing) return 0.15f
    val wave = 0.45f + 0.25f * sin(positionMs / 310.0) + 0.15f * sin(positionMs / 97.0)
    return wave.toFloat().coerceIn(0.1f, 0.9f)
}

/**
 * The M2 prism motif: entry beam, triangle outline and a fanned spectrum.
 * It breathes subtly with [energy] and never competes with the record.
 */
@Composable
fun PrismMotif(size: Dp, energy: Float, modifier: Modifier = Modifier) {
    val spectrum = remember {
        listOf(
            Color(0xFFE2574C),
            Color(0xFFE67E22),
            Color(0xFFF5D76E),
            Color(0xFF7BD389),
            Color(0xFF4A90D9),
            Color(0xFF7A5FD0),
        )
    }
    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                val pulse = 1f + 0.04f * energy
                scaleX = pulse
                scaleY = pulse
                alpha = 0.55f + 0.45f * energy
            },
    ) {
        val s = size.toPx()
        val cx = s * 0.36f
        val cy = s * 0.44f
        val tri = s * 0.20f
        // Entry beam.
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = Offset(-s * 0.05f, cy - tri * 0.5f),
            end = Offset(cx - tri * 0.4f, cy - tri * 0.12f),
            strokeWidth = s * 0.012f,
            cap = StrokeCap.Round,
        )
        // Spectrum fan.
        spectrum.forEachIndexed { i, color ->
            val spread = (i - (spectrum.size - 1) / 2f) * s * 0.035f
            drawLine(
                color = color.copy(alpha = 0.85f),
                start = Offset(cx + tri * 0.3f, cy + tri * 0.15f),
                end = Offset(s * 1.05f, cy + tri * 0.15f + spread * 2.2f),
                strokeWidth = s * (0.014f + 0.008f * energy),
                cap = StrokeCap.Round,
            )
        }
        // Prism triangle.
        val path = Path().apply {
            moveTo(cx, cy - tri)
            lineTo(cx + tri * 0.92f, cy + tri * 0.62f)
            lineTo(cx - tri * 0.92f, cy + tri * 0.62f)
            close()
        }
        drawPath(path, Color.White.copy(alpha = 0.10f))
        drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width = s * 0.012f))
    }
}
