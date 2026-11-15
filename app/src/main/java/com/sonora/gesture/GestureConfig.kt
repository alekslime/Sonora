package com.sonora.gesture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User-facing gesture configuration. Persisted in M5 alongside audio prefs. */
data class GestureConfig(
    val enabled: Boolean = true,
    /** Three-finger hold duration before Liquid opens. */
    val holdDurationMs: Long = 600,
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

    fun setHoldDurationMs(durationMs: Long) {
        config = config.copy(holdDurationMs = durationMs.coerceIn(GestureConfig.MIN_HOLD_MS, GestureConfig.MAX_HOLD_MS))
    }

    fun setHaptics(haptics: Boolean) {
        config = config.copy(haptics = haptics)
    }

    fun setReducedMotion(reducedMotion: Boolean) {
        config = config.copy(reducedMotion = reducedMotion)
    }
}
