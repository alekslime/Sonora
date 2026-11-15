package com.sonora.gesture

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * M4 root gesture layer. A single pointer-input scope feeds the shared
 * [GestureMachine], so three-finger gestures can never double-trigger or
 * fight each other. Single-finger interaction is untouched: touches are
 * only consumed while three or more pointers are down, or after a gesture
 * has fired (so the release cannot activate a control underneath).
 *
 * Inert while [enabled] is false or Liquid is open (Liquid owns all touch).
 */
@Composable
fun SonoraGestureLayer(
    enabled: Boolean,
    holdDurationMs: Long,
    context: GestureContext,
    liquidOpen: Boolean,
    hapticsEnabled: Boolean,
    onOpenLiquid: () -> Unit,
    onOpenStudio: () -> Unit,
    onCloseStudio: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val slopPx = remember(view) { ViewConfiguration.get(view.context).scaledTouchSlop.toFloat() }
    val swipePx = remember(density) { with(density) { 56.dp.toPx() } }
    val gestureScope = rememberCoroutineScope()
    // Snapshot callbacks per composition so the pointer loop stays fresh.
    val callbacks = remember(onOpenLiquid, onOpenStudio, onCloseStudio, hapticsEnabled) {
        GestureCallbacks(onOpenLiquid, onOpenStudio, onCloseStudio, hapticsEnabled)
    }

    Box(
        modifier.then(
            if (!enabled || liquidOpen) {
                Modifier
            } else {
                Modifier.pointerInput(context, holdDurationMs, slopPx, swipePx, callbacks) {
                    val machine = GestureMachine(holdDurationMs, slopPx, swipePx)
                    awaitEachGesture {
                        machine.reset()
                        var timer: Job? = null
                        var claimed = false
                        fun fire(output: GestureOutput) {
                            if (output == GestureOutput.None) return
                            claimed = true
                            if (callbacks.hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            when (output) {
                                GestureOutput.OpenLiquid -> callbacks.onOpenLiquid()
                                GestureOutput.OpenStudio -> callbacks.onOpenStudio()
                                GestureOutput.CloseStudio -> callbacks.onCloseStudio()
                                GestureOutput.None -> Unit
                            }
                        }
                        fun ensureTimer() {
                            val deadline = machine.nextDeadline ?: return
                            if (timer?.isActive == true) return
                            timer = gestureScope.launch {
                                delay(max(0L, deadline - SystemClock.uptimeMillis()))
                                fire(machine.handle(GestureEvent.Tick(SystemClock.uptimeMillis()), context, false))
                            }
                        }
                        try {
                            do {
                                val event = awaitPointerEvent()
                                val t = SystemClock.uptimeMillis()
                                for (change in event.changes) {
                                    val id = change.id.value
                                    val gestureEvent = when {
                                        change.pressed && !change.previousPressed ->
                                            GestureEvent.Down(id, change.position.x, change.position.y, t)
                                        !change.pressed && change.previousPressed ->
                                            GestureEvent.Up(id, t)
                                        change.pressed ->
                                            GestureEvent.Move(id, change.position.x, change.position.y, t)
                                        else -> null
                                    }
                                    if (gestureEvent != null) {
                                        fire(machine.handle(gestureEvent, context, false))
                                    }
                                }
                                if (claimed || machine.pointerCount >= 3) {
                                    event.changes.forEach { it.consume() }
                                }
                                ensureTimer()
                            } while (event.changes.any { it.pressed })
                        } finally {
                            timer?.cancel()
                        }
                    }
                }
            },
        ),
    ) {
        content()
    }
}

private data class GestureCallbacks(
    val onOpenLiquid: () -> Unit,
    val onOpenStudio: () -> Unit,
    val onCloseStudio: () -> Unit,
    val hapticsEnabled: Boolean,
)
