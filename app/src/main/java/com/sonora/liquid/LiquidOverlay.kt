package com.sonora.liquid

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sonora.nowplaying.NowPlayingState
import com.sonora.playback.PlaybackHub
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import kotlinx.coroutines.delay

/** Live captures older than this yield to the mock signal. */
private const val LIVE_STALE_MS = 500L

/**
 * M3 Liquid overlay, M9 show. Full-screen and temporary: a tap returns
 * to Now Playing, a horizontal swipe steps between equations, and the
 * show otherwise auto-cycles with crossfades. Bands come from the
 * service FFT analyzer over the hub when the session has an engine
 * audio session (M6), from the mock spectrum for demo playback,
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

    // Live signal rides the hub from the service analyzer while its
    // captures are fresh; anything else (demo playback, dead analyzer,
    // no session) falls back to the mock or a flat frame.
    val sessionId = session?.audioSessionId ?: 0
    val liveSignal by PlaybackHub.vizSignal.collectAsState()
    val signal = run {
        val live = liveSignal
        if (sessionId != 0 && live != null &&
            SystemClock.elapsedRealtime() - live.frame.atMs < LIVE_STALE_MS
        ) {
            return@run live
        }
        if (session != null) {
            MockSpectrum.signal(session.positionMs, session.isPlaying)
        } else {
            VizSignal(Bands.Ambient, VizFrame.flat())
        }
    }

    val show = remember(config.mode, config.autoCycle) {
        VizShow(config.mode, config.autoCycle)
    }
    LaunchedEffect(config.autoCycle) {
        show.autoCycle = config.autoCycle
    }
    val density = LocalDensity.current
    val swipeTriggerPx = remember(density) { with(density) { 48.dp.toPx() } }
    val swipeArmPx = remember(density) { with(density) { 24.dp.toPx() } }

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
            .pointerInput(show, onExit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var totalX = 0f
                    var totalY = 0f
                    var swiping = false
                    var finished = false
                    while (!finished) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            finished = true
                        } else {
                            totalX += change.position.x - change.previousPosition.x
                            totalY += change.position.y - change.previousPosition.y
                            if (!swiping && kotlin.math.abs(totalX) > swipeArmPx &&
                                kotlin.math.abs(totalX) > kotlin.math.abs(totalY) * 1.5f
                            ) {
                                swiping = true
                            }
                            if (!change.pressed) {
                                finished = true
                                if (swiping && kotlin.math.abs(totalX) > swipeTriggerPx) {
                                    if (totalX < 0f) show.next() else show.prev()
                                } else if (!swiping) {
                                    onExit()
                                }
                                // A short swipe is a no-op: neither a
                                // switch nor an exit, so nothing underneath
                                // can activate by accident.
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            },
    ) {
        VizCanvas(show = show, signal = signal, config = config, motionEnabled = motionEnabled)
        if (hintVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .alpha(hintAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    show.current.label.uppercase(),
                    style = type.labelCaps,
                    color = colors.ivory,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Touch to return · Swipe to switch",
                    style = type.caption,
                    color = colors.ivoryDim,
                )
            }
        }
    }
}
