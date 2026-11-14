package com.sonora.nowplaying

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import com.sonora.data.Album
import kotlin.random.Random

/** 33⅓ RPM in degrees per second — a real vinyl pace. */
const val VINYL_DEGREES_PER_SECOND = 200f

/**
 * The M2 record. Grooves, separators and the label rotate on the GPU
 * (no redraw per frame); reflections and sheen stay fixed like real light.
 * Rotation accumulates through a frame loop so pause/resume never jumps.
 */
@Composable
fun Vinyl(album: Album, diameter: Dp, isPlaying: Boolean, modifier: Modifier = Modifier) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            angle = (angle + (now - last) / 1_000_000_000f * VINYL_DEGREES_PER_SECOND) % 360f
            last = now
        }
    }
    val labelColor = remember(album.artSeed) {
        Color.hsv(((album.artSeed % 360 + 360) % 360).toFloat(), 0.45f, 0.34f)
    }
    val separators = remember(album.artSeed) {
        val random = Random(album.artSeed)
        List(4) { 0.18f + it * 0.19f + random.nextFloat() * 0.05f }
    }

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        // Rotating layer: grooves, dead-wax separators, label, marker.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = angle },
        ) {
            val outerR = size.minDimension / 2f
            val labelR = outerR * 0.32f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0xFF0A0A0C), outerR, center)
            var radius = labelR + 8f
            var groove = 0
            while (radius < outerR - 3f) {
                drawCircle(
                    color = Color.White.copy(alpha = if (groove % 4 == 0) 0.055f else 0.03f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1f),
                )
                radius += 3f
                groove++
            }
            separators.forEach { fraction ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = labelR + (outerR - labelR) * fraction,
                    center = center,
                    style = Stroke(width = 3f),
                )
            }
            drawCircle(labelColor, labelR, center)
            drawCircle(Color.White.copy(alpha = 0.28f), labelR - 1f, center, style = Stroke(width = 2f))
            // Rotation marker wedge on the label edge.
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = -9f,
                sweepAngle = 18f,
                useCenter = true,
                topLeft = Offset(center.x - labelR, center.y - labelR),
                size = Size(labelR * 2f, labelR * 2f),
            )
            drawCircle(Color(0xFF0B0B0D), labelR * 0.13f, center)
            // Label title, rotating with the disc.
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 235
                textSize = labelR * 0.24f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                album.title.take(20),
                center.x,
                center.y + labelR * 0.62f,
                paint,
            )
        }
        // Static layer: rim light, reflection arcs, sheen.
        Canvas(Modifier.fillMaxSize()) {
            val outerR = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = outerR - 1f,
                center = center,
                style = Stroke(width = 2f),
            )
            val band = outerR * 0.36f
            listOf(198f, 18f).forEach { start ->
                drawArc(
                    color = Color.White.copy(alpha = 0.035f),
                    startAngle = start,
                    sweepAngle = 32f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2f, outerR * 2f),
                    style = Stroke(width = band),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(center.x - outerR * 0.45f, center.y - outerR * 0.45f),
                    radius = outerR * 1.1f,
                ),
                radius = outerR,
                center = center,
            )
        }
    }
}
