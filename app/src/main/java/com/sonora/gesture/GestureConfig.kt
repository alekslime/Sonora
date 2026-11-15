package com.sonora.gesture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * M4 gesture preferences. Three-finger input stays secondary to visible
 * controls and yields to accessibility services (checked at the layer).
 */
data class GestureConfig(
    val enabled: Boolean = true,
    /** Stationary hold time before the Liquid gesture fires. */
    val holdDurationMs: Long = 700L,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
) {
    companion object {
        const val MIN_HOLD_MS = 300L
        const val MAX_HOLD_MS = 1500L
    }
}

/** In-memory M4 store; M5 backs it with persistent preferences. */
object GestureConfigStore {
    var config by mutableStateOf(GestureConfig())
        private set

    fun setEnabled(enabled: Boolean) {
        config = config.copy(enabled = enabled)
    }

    fun setHoldDuration(durationMs: Long) {
        config = config.copy(
            holdDurationMs = durationMs.coerceIn(
                GestureConfig.MIN_HOLD_MS,
                GestureConfig.MAX_HOLD_MS,
            ),
        )
    }

    fun setHaptics(enabled: Boolean) {
        config = config.copy(haptics = enabled)
    }

    fun setReducedMotion(enabled: Boolean) {
        config = config.copy(reducedMotion = enabled)
    }
}
