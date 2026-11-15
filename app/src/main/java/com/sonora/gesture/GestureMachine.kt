package com.sonora.gesture

import kotlin.math.abs
import kotlin.math.hypot

/** Where the app is when gesture input arrives. */
enum class GestureContext { NOW_PLAYING, STUDIO, OTHER }

sealed interface GestureOutput {
    data object None : GestureOutput
    data object OpenLiquid : GestureOutput
    data object OpenStudio : GestureOutput
    data object CloseStudio : GestureOutput
}

sealed interface GestureEvent {
    data class Down(val id: Long, val x: Float, val y: Float, val t: Long) : GestureEvent
    data class Move(val id: Long, val x: Float, val y: Float, val t: Long) : GestureEvent
    data class Up(val id: Long, val t: Long) : GestureEvent
    data class Cancel(val t: Long) : GestureEvent
    data class Tick(val t: Long) : GestureEvent
}

/**
 * M4 gesture state machine. Pure logic, no Compose: exactly three
 * stationary pointers held for [holdDurationMs] opens Liquid; a coherent
 * three-finger vertical swipe opens/closes Audio Studio depending on
 * context. One trigger per touch session, then silence until full release.
 *
 * Rules:
 * - Any pointer-count change or excess movement before the hold deadline
 *   cancels the hold; more than three pointers fails the session.
 * - Swipes need all three pointers past the threshold in the same
 *   direction with limited sideways drift.
 * - Cancellation resets everything immediately.
 */
class GestureMachine(
    private val holdDurationMs: Long,
    private val touchSlopPx: Float,
    private val swipeThresholdPx: Float,
) {
    private data class Pointer(
        val id: Long,
        var x: Float,
        var y: Float,
        var anchorX: Float,
        var anchorY: Float,
    )

    private val pointers = mutableMapOf<Long, Pointer>()
    private var armedDeadline: Long? = null
    private var fired = false
    private var failed = false

    val pointerCount: Int get() = pointers.size
    val nextDeadline: Long? get() = armedDeadline

    fun reset() {
        pointers.clear()
        armedDeadline = null
        fired = false
        failed = false
    }

    fun handle(
        event: GestureEvent,
        context: GestureContext,
        liquidOpen: Boolean,
    ): GestureOutput {
        when (event) {
            is GestureEvent.Down -> {
                pointers[event.id] = Pointer(event.id, event.x, event.y, event.x, event.y)
                armedDeadline = null
                if (failed || fired) return GestureOutput.None
                if (pointers.size == 3) {
                    pointers.values.forEach {
                        it.anchorX = it.x
                        it.anchorY = it.y
                    }
                    armedDeadline = event.t + holdDurationMs
                } else if (pointers.size > 3) {
                    failed = true
                }
                return GestureOutput.None
            }
            is GestureEvent.Move -> {
                val pointer = pointers[event.id] ?: return GestureOutput.None
                pointer.x = event.x
                pointer.y = event.y
                if (failed || fired || liquidOpen) return GestureOutput.None
                if (pointers.size != 3) {
                    armedDeadline = null
                    return GestureOutput.None
                }
                if (armedDeadline != null) {
                    if (anyBeyondSlop()) {
                        armedDeadline = null
                    } else {
                        return GestureOutput.None
                    }
                }
                val dys = pointers.values.map { it.y - it.anchorY }
                val dxs = pointers.values.map { abs(it.x - it.anchorX) }
                val coherent = dxs.all { it < swipeThresholdPx * 1.5f }
                if (coherent && dys.all { it < -swipeThresholdPx } && context == GestureContext.NOW_PLAYING) {
                    fired = true
                    armedDeadline = null
                    return GestureOutput.OpenStudio
                }
                if (coherent && dys.all { it > swipeThresholdPx } && context == GestureContext.STUDIO) {
                    fired = true
                    armedDeadline = null
                    return GestureOutput.CloseStudio
                }
                return GestureOutput.None
            }
            is GestureEvent.Up -> {
                pointers.remove(event.id)
                armedDeadline = null
                if (pointers.isEmpty()) {
                    fired = false
                    failed = false
                }
                return GestureOutput.None
            }
            is GestureEvent.Cancel -> {
                reset()
                return GestureOutput.None
            }
            is GestureEvent.Tick -> {
                if (failed || fired || liquidOpen) return GestureOutput.None
                val deadline = armedDeadline ?: return GestureOutput.None
                if (pointers.size != 3) {
                    armedDeadline = null
                    return GestureOutput.None
                }
                if (anyBeyondSlop()) {
                    armedDeadline = null
                    return GestureOutput.None
                }
                if (event.t >= deadline) {
                    armedDeadline = null
                    fired = true
                    return GestureOutput.OpenLiquid
                }
                return GestureOutput.None
            }
        }
    }

    private fun anyBeyondSlop(): Boolean =
        pointers.values.any { hypot(it.x - it.anchorX, it.y - it.anchorY) > touchSlopPx }
}
