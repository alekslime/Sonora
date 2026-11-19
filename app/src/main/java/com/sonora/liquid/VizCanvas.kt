package com.sonora.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * M9 show canvas. Runs the frame clock (shared by every equation so
 * transitions stay seamless), follows the analyzer readings between
 * captures for kick-fast response, and crossfades the outgoing and
 * incoming equations on a GPU-composited layer stack.
 */
@Composable
fun VizCanvas(
    show: VizShow,
    bands: Bands,
    config: LiquidConfig,
    motionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    var smooth by remember { mutableStateOf(bands) }
    val latestBands by rememberUpdatedState(bands)
    LaunchedEffect(motionEnabled) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dtMs = (now - last) / 1_000_000L
            last = now
            smooth = smoothBands(smooth, latestBands, dtMs / 1000f)
            // Autonomous drift freezes under reduced motion; the mass
            // still responds subtly as band levels change with the music.
            if (motionEnabled) timeSec += dtMs / 1000f
            show.frame(dtMs)
        }
    }
    val outgoing = show.outgoing
    Box(modifier.fillMaxSize()) {
        VizModeLayer(
            mode = outgoing ?: show.current,
            bands = smooth,
            config = config,
            timeSec = timeSec,
            alpha = 1f,
        )
        if (outgoing != null) {
            VizModeLayer(
                mode = show.current,
                bands = smooth,
                config = config,
                timeSec = timeSec,
                alpha = show.fade,
            )
        }
    }
}

@Composable
private fun VizModeLayer(
    mode: VizMode,
    bands: Bands,
    config: LiquidConfig,
    timeSec: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val layer = modifier
        .fillMaxSize()
        .graphicsLayer { this.alpha = alpha }
    when (mode) {
        VizMode.MANDELBROT -> MandelbrotViz(
            bands = bands,
            preset = config.preset,
            timeSec = timeSec,
            intensity = config.intensity,
            quality = config.quality,
            modifier = layer,
        )
        VizMode.RIBBONS -> Canvas(layer) {
            drawRibbons(bands, config.preset, timeSec, config.intensity, config.quality)
        }
        VizMode.POLAR -> Canvas(layer) {
            drawPolar(bands, config.preset, timeSec, config.intensity, config.quality)
        }
        VizMode.STAR -> Canvas(layer) {
            drawStar(bands, config.preset, timeSec, config.intensity, config.quality)
        }
    }
}
