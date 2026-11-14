package com.sonora.shelf

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Screen placement of one sleeve relative to the current scroll offset. */
data class SleevePlacement(
    /** Uniform scale (1 = selected). */
    val scale: Float,
    /** Opacity (distant sleeves fade). */
    val alpha: Float,
    /** Black overlay strength for depth shading. */
    val dim: Float,
    /** Vertical center offset in px from the selection anchor. */
    val yCenterPx: Float,
)

/**
 * Pure shelf geometry. Kept free of Compose so it is unit-testable and
 * allocation behavior is obvious (one small object per visible sleeve).
 */
object ShelfGeometry {
    /** Artwork plane tilt: the "looking down into the collection" angle. */
    const val TILT_DEGREES = 70f

    /** How many sleeves stay composed above / below the selection. */
    const val VISIBLE_ABOVE = 4
    const val VISIBLE_BELOW = 2

    fun placement(distance: Float, spacingPx: Float): SleevePlacement {
        val ad = abs(distance)
        val scale = 1f / (1f + 0.17f * ad)
        val alpha = (1f - 0.24f * ad).coerceIn(0f, 1f)
        val dim = (0.08f + 0.11f * ad).coerceIn(0f, 0.6f)
        return SleevePlacement(scale, alpha, dim, distance * spacingPx)
    }

    fun visibleRange(offset: Float, count: Int): IntRange {
        if (count <= 0) return IntRange.EMPTY
        val lo = floor(offset - VISIBLE_ABOVE).toInt().coerceAtLeast(0)
        val hi = ceil(offset + VISIBLE_BELOW).toInt().coerceAtMost(count - 1)
        if (lo > hi) return IntRange.EMPTY
        return lo..hi
    }
}
