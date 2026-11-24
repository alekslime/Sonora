package com.sonora.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * System text-scale awareness. The design is built in sp; when the
 * user enlarges their font scale the design grows here, and when they
 * shrink it, it stays tight. A multiplier keeps the design's intent
 * instead of just echoing the OS raw value.
 */
@Composable
fun rememberSonoraScale(): Float {
    val fontScale = LocalDensity.current.fontScale
    return remember(fontScale) {
        when {
            fontScale >= 1.35f -> 1.25f
            fontScale >= 1.15f -> 1.1f
            fontScale >= 0.9f -> 1.0f
            else -> 0.85f
        }
    }
}

/** Scales a text style's size by [factor] and returns the adjusted style. */
fun TextStyle.scaled(factor: Float): TextStyle {
    if (factor == 1f) return this
    return copy(fontSize = fontSize * factor)
}
