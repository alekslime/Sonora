package com.sonora.gesture

/**
 * Where the gesture starts. Swipe meanings depend on it; the hold that
 * opens Liquid works everywhere (Liquid itself consumes its own touches).
 */
enum class GestureContext { NOW_PLAYING, STUDIO, OTHER }

/** What the machine asks the app to do. At most one action per event. */
enum class GestureAction { NONE, OPEN_LIQUID, OPEN_STUDIO, CLOSE_STUDIO }

/**
 * Pure three-finger state machine. The layer feeds pointer events; this
 * decides. Hold fires once per touch after a stationary wait; any move or
 * pointer-count change beforehand cancels it. Lifts resolve swipes.
 */
data class GestureMachine(
    val pointersDown: Int = 0,
    val holdArmed: Boolean = false,
    val holdFired: Boolean = false,
    val swipeTracking: Boolean = false,
    val startCentroidYPx: Float = 0f,
) {
    companion object {
        /** Vertical travel that counts as an intentional three-finger swipe. */
        const val SWIPE_THRESHOLD_PX = 120f
    }
}

sealed interface GestureEvent {
    /** Pointer count changed; centroid is the current mean Y. */
    data class PointersChanged(val count: Int, val centroidYPx: Float) : GestureEvent

    /** Hold wait elapsed without movement or count change. */
    data object HoldTimeout : GestureEvent

    /** Fingers moved beyond touch slop while armed. */
    data object MovedBeyondSlop : GestureEvent

    /** System cancelled the gesture. */
    data object Cancel : GestureEvent
}

/**
 * Reduce one event. Returns the next machine plus at most one action.
 * The swipe threshold is injectable so tests need no display metrics.
 */
fun GestureMachine.reduce(
    event: GestureEvent,
    context: GestureContext,
    swipeThresholdPx: Float = GestureMachine.SWIPE_THRESHOLD_PX,
): Pair<GestureMachine, GestureAction> {
    return when (event) {
        is GestureEvent.PointersChanged -> {
            // Release resolves against the pre-update tracking state.
            if (event.count == 0) {
                val dy = event.centroidYPx - startCentroidYPx
                val action = if (!holdFired && swipeTracking && dy <= -swipeThresholdPx &&
                    context == GestureContext.NOW_PLAYING
                ) {
                    GestureAction.OPEN_STUDIO
                } else if (!holdFired && swipeTracking && dy >= swipeThresholdPx &&
                    context == GestureContext.STUDIO
                ) {
                    GestureAction.CLOSE_STUDIO
                } else {
                    GestureAction.NONE
                }
                return Pair(GestureMachine(), action)
            }
            // Landing exactly on three starts a fresh track; any other
            // count change disarms (the hold timer restarts on landing).
            if (event.count == 3 && pointersDown < 3) {
                Pair(
                    copy(
                        pointersDown = 3,
                        holdArmed = true,
                        swipeTracking = true,
                        startCentroidYPx = event.centroidYPx,
                    ),
                    GestureAction.NONE,
                )
            } else {
                Pair(
                    copy(pointersDown = event.count, holdArmed = false, swipeTracking = false),
                    GestureAction.NONE,
                )
            }
        }
        GestureEvent.HoldTimeout -> {
            if (holdArmed && !holdFired && pointersDown == 3) {
                Pair(copy(holdArmed = false, holdFired = true), GestureAction.OPEN_LIQUID)
            } else {
                Pair(this, GestureAction.NONE)
            }
        }
        GestureEvent.MovedBeyondSlop -> {
            Pair(copy(holdArmed = false), GestureAction.NONE)
        }
        GestureEvent.Cancel -> {
            Pair(GestureMachine(), GestureAction.NONE)
        }
    }
}
