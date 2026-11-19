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
 * M9 show canvas, M10 signal. Runs the frame clock (shared by every
 * equation so transitions stay seamless), follows the analyzer
 * readings between captures for kick-fast response, and crossfades
 * the outgoing and incoming equations on GPU-composited layers. Band
 * levels are smoothed; the waveform and spectrum pass through raw so
 * the picture never lags the music.
 */
@Composable
fun VizCanvas(
    show: VizShow,
    signal: VizSignal,
    config: LiquidConfig,
    motionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    var smooth by remember(signal.bands) { mutableStateOf(signal.bands) }
    val latestBands by rememberUpdatedState(signal.bands)
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
    val render = remember(signal, smooth) { signal.copy(bands = smooth) }
    val outgoing = show.outgoing
    Box(modifier.fillMaxSize()) {
        VizModeLayer(
            mode = outgoing ?: show.current,
            signal = render,
            config = config,
            timeSec = timeSec,
            alpha = 1f,
        )
        if (outgoing != null) {
            VizModeLayer(
                mode = show.current,
                signal = render,
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
    signal: VizSignal,
    config: LiquidConfig,
    timeSec: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val layer = modifier
        .fillMaxSize()
        .graphicsLayer { this.alpha = alpha }
    when (mode) {
        VizMode.WAVES -> Canvas(layer) {
            drawWaves(signal, config.preset, timeSec, config.intensity, config.quality)
        }
        VizMode.BARS -> BarsViz(
            signal = signal,
            preset = config.preset,
            timeSec = timeSec,
            intensity = config.intensity,
            modifier = layer,
        )
        VizMode.SCOPE -> ScopeViz(
            signal = signal,
            preset = config.preset,
            timeSec = timeSec,
            intensity = config.intensity,
            modifier = layer,
        )
        VizMode.STORM -> StormViz(
            signal = signal,
            preset = config.preset,
            timeSec = timeSec,
            intensity = config.intensity,
            modifier = layer,
        )
    }
}
