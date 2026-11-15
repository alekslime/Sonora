package com.sonora.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * M3 fallback renderer for API < 33 (no RuntimeShader): layered,
 * deforming blobs with rim light and orbiting particles on Canvas 2D.
 */
@Composable
fun FallbackLiquid(
    bands: Bands,
    config: LiquidConfig,
    timeSec: Float,
    modifier: Modifier = Modifier,
) {
    val preset = config.preset
    val particleSeeds = remember {
        val random = Random(1337L)
        List(90) { ParticleSeed(random.nextFloat(), random.nextFloat(), random.nextFloat()) }
    }
    val layers = when (config.quality) {
        LiquidQuality.LOW -> 2
        LiquidQuality.MEDIUM -> 3
        LiquidQuality.HIGH -> 4
    }
    val particleCount = when (config.quality) {
        LiquidQuality.LOW -> 20
        LiquidQuality.MEDIUM -> 45
        LiquidQuality.HIGH -> 80
    }
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.46f)
        val baseR = minOf(w, h) * 0.34f
        val intensity = config.intensity
        drawRect(preset.deep)
        // Soft glow behind the mass.
        drawCircle(
            color = preset.body.copy(alpha = 0.25f),
            radius = baseR * 1.9f,
            center = center,
        )
        // Deforming body layers, back to front.
        for (layer in layers - 1 downTo 0) {
            val t = layer.toFloat() / layers
            val radius = baseR * (1.05f - 0.16f * t)
            val path = blobPath(
                center = center,
                radius = radius,
                timeSec = timeSec,
                phase = layer * 1.7f,
                wobble = (0.05f + 0.16f * bands.bass) * intensity + 0.02f,
                ripple = bands.mid * intensity,
            )
            drawPath(
                path = path,
                color = preset.body.copy(alpha = 0.35f + 0.45f * (1f - t)),
            )
        }
        // Rim light on the front layer edge.
        val front = blobPath(
            center = center,
            radius = baseR * 1.05f,
            timeSec = timeSec,
            phase = 0f,
            wobble = (0.05f + 0.16f * bands.bass) * intensity + 0.02f,
            ripple = bands.mid * intensity,
        )
        drawPath(
            path = front,
            color = preset.accent.copy(alpha = 0.35f + 0.45f * bands.pulse),
            style = Stroke(width = 3f),
        )
        // Pulse flash.
        if (bands.pulse > 0.05f) {
            drawCircle(
                color = preset.accent.copy(alpha = 0.12f * bands.pulse),
                radius = baseR * (1.2f + bands.pulse * 0.3f),
                center = center,
            )
        }
        // Orbiting particles; treble drives their shimmer.
        repeat(particleCount) { i ->
            val seed = particleSeeds[i]
            val angle = seed.a * 6.2832f + timeSec * (0.15f + seed.b * 0.5f) * (if (i % 2 == 0) 1f else -1f)
            val orbit = baseR * (0.6f + seed.c * 1.1f)
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(timeSec * (2f + seed.b * 4f) + seed.a * 20f))
            drawCircle(
                color = (if (i % 3 == 0) preset.accent else Color.White)
                    .copy(alpha = twinkle * (0.15f + 0.55f * bands.treble)),
                radius = 1.5f + seed.b * 3f,
                center = Offset(
                    center.x + cos(angle) * orbit,
                    center.y + sin(angle) * orbit * 0.9f,
                ),
            )
        }
    }
}

private data class ParticleSeed(val a: Float, val b: Float, val c: Float)

/** Closed organic outline: bass deforms, midrange ripples. */
private fun blobPath(
    center: Offset,
    radius: Float,
    timeSec: Float,
    phase: Float,
    wobble: Float,
    ripple: Float,
): Path {
    val path = Path()
    val steps = 72
    for (i in 0..steps) {
        val theta = i.toFloat() / steps * 6.2832f
        val r = radius * (
            1f +
                wobble * sin(3f * theta + timeSec * 0.9f + phase) +
                wobble * 0.6f * sin(5f * theta - timeSec * 0.7f) +
                ripple * 0.08f * sin(11f * theta + timeSec * 3f)
            )
        val point = Offset(center.x + cos(theta) * r, center.y + sin(theta) * r)
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
