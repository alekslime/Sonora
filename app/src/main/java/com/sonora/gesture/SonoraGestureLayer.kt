package com.sonora.gesture

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * True when TalkBack-style touch exploration or display magnification is
 * active. Three-finger input yields to accessibility services; the visible
 * alternatives (artwork tap, Studio button) stay available.
 */
fun isAssistiveTouchActive(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    if (manager?.isEnabled == true && manager.isTouchExplorationEnabled) return true
    return try {
        // No SDK constant exists for this key; the string is stable API.
        Settings.Secure.getInt(
            context.contentResolver,
            "accessibility_display_magnification_enabled",
            0,
        ) == 1
    } catch (_: Exception) {
        false
    }
}

/**
 * Root three-finger layer. Pure observation below three pointers (never
 * consumes, so scrolling and taps work normally); once three pointers
 * land it owns the gesture, feeding the [GestureMachine] and consuming
 * so nothing underneath double-handles it.
 */
@Composable
fun SonoraGestureLayer(
    enabled: Boolean,
    context: GestureContext,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var machine by remember { mutableStateOf(GestureMachine()) }

    if (!enabled) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }

    val slopPx = remember(density) { with(density) { 16.dp.toPx() } }
    val swipePx = remember(density) { with(density) { 120.dp.toPx() } }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(context, enabled) {
                awaitEachGesture {
                    machine = GestureMachine()
                    var holdJob: Job? = null
                    var anchorX = 0f
                    var anchorY = 0f
                    var lastX = 0f
                    var lastY = 0f
                    var owned = false
                    var previousCount = 0

                    fun fire(event: GestureEvent) {
                        val (next, action) = machine.reduce(event, context, swipePx)
                        machine = next
                        if (action != GestureAction.NONE) onAction(action)
                    }

                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        val count = pressed.size
                        // At full release nothing is pressed: resolve swipes
                        // from the last live centroid, not the anchor.
                        val centroidX = if (pressed.isNotEmpty()) {
                            pressed.map { it.position.x }.average().toFloat().also { lastX = it }
                        } else {
                            lastX
                        }
                        val centroidY = if (pressed.isNotEmpty()) {
                            pressed.map { it.position.y }.average().toFloat().also { lastY = it }
                        } else {
                            lastY
                        }
                        if (count != previousCount) {
                            if (count == 3 && previousCount < 3) {
                                owned = true
                                anchorX = centroidX
                                anchorY = centroidY
                                holdJob?.cancel()
                                val holdMs = GestureConfigStore.config.holdDurationMs
                                holdJob = scope.launch {
                                    delay(holdMs)
                                    val before = machine
                                    fire(GestureEvent.HoldTimeout)
                                    if (machine.holdFired && !before.holdFired &&
                                        GestureConfigStore.config.haptics
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            } else {
                                holdJob?.cancel()
                                holdJob = null
                            }
                            fire(GestureEvent.PointersChanged(count, centroidY))
                            previousCount = count
                        } else if (count == 3 && machine.holdArmed &&
                            hypot(centroidX - anchorX, centroidY - anchorY) > slopPx
                        ) {
                            holdJob?.cancel()
                            holdJob = null
                            fire(GestureEvent.MovedBeyondSlop)
                        }
                        if (owned) event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    holdJob?.cancel()
                }
            },
    ) {
        content()
    }
}
