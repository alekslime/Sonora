package com.sonora.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * M0 motion principles: motion communicates physicality and state changes.
 * Shelf settling uses a slightly underdamped spring so the stack feels
 * weighty; screen transitions are short fades, never playful.
 */
object SonoraMotion {
    /** Shelf snap-to-selection and programmatic selection. */
    fun <T> settle() = spring<T>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = 0.86f,
    )

    /** Selection emphasis, scrim and control fades. */
    fun <T> gentle(durationMillis: Int = 260) = tween<T>(
        durationMillis = durationMillis,
        easing = EmphasisEasing,
    )

    /** Screen enter/exit. */
    const val SCREEN_MILLIS = 240

    val EmphasisEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
}
