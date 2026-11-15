package com.sonora.liquid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sonora.nowplaying.NowPlayingState
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import kotlinx.coroutines.delay

/**
 * M3 Liquid overlay. Full-screen and temporary: any new touch exits
 * immediately to Now Playing, and there are no visualizer controls to
 * operate. Bands come from the live playback session when available,
 * otherwise the ambient fallback.
 */
@Composable
fun LiquidOverlay(
    session: NowPlayingState?,
    onExit: () -> Unit,
    motionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val config = LiquidConfigStore.config
    BackHandler(onBack = onExit)

    val bands = if (session != null) {
        MockSpectrum.sample(session.positionMs, session.isPlaying)
    } else {
        Bands.Ambient
    }

    // Hint fades away on its own; it is not a control.
    var hintVisible by remember { mutableStateOf(true) }
    val hintAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        delay(2500)
        hintAlpha.animateTo(0f, tween(800))
        hintVisible = false
    }

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Liquid visualizer"
                customActions = listOf(
                    CustomAccessibilityAction("Close visualizer") { onExit(); true },
                )
            }
            .pointerInput(onExit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onExit()
                    // Swallow the rest of this gesture so the up event can
                    // never activate a control underneath after the exit.
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        pressed = event.changes.any { it.pressed }
                    }
                }
            },
    ) {
        LiquidCanvas(bands = bands, config = config, motionEnabled = motionEnabled)
        if (hintVisible) {
            Text(
                "Touch anywhere to return",
                style = type.caption,
                color = colors.ivoryDim,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .alpha(hintAlpha.value),
            )
        }
    }
}
