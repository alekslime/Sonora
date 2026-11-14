package com.sonora.shelf

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.sonora.theme.SonoraMotion
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Shelf interaction state. A single scroll offset in album-index units
 * drives the whole stack; selection is the settled index. All motion goes
 * through [offset], so drags, flings and taps serialize on its mutator
 * mutex and the state stays predictable.
 */
@Stable
class ShelfState(
    val albumCount: Int,
    initialIndex: Int,
) {
    val offset = Animatable(initialIndex.toFloat().coerceIn(0f, (albumCount - 1).coerceAtLeast(0).toFloat()))

    /**
     * Serializes drags, flings and taps in dispatch order. New user input
     * (drag/select) interrupts a running fling; flings never interrupt input.
     */
    private val mutex = MutatorMutex()

    /** Step size in px, written by the layout pass, read by gestures. UI thread only. */
    var spacingPx: Float = 200f

    val selectedIndex: Int
        get() = offset.value.roundToInt().coerceIn(0, (albumCount - 1).coerceAtLeast(0))

    suspend fun dragBy(deltaPx: Float) = mutex.mutate(MutatePriority.PreventUserInput) {
        val spacing = spacingPx.coerceAtLeast(1f)
        offset.snapTo((offset.value - deltaPx / spacing).coerceIn(0f, (albumCount - 1).toFloat()))
    }

    suspend fun fling(velocityPxPerSec: Float) = mutex.mutate(MutatePriority.UserInput) {
        val spacing = spacingPx.coerceAtLeast(1f)
        val velocityUnits = -velocityPxPerSec / spacing
        val target = if (abs(velocityUnits) < 0.5f) {
            selectedIndex
        } else {
            (offset.value + velocityUnits * 0.22f).roundToInt().coerceIn(0, albumCount - 1)
        }
        offset.animateTo(target.toFloat(), SonoraMotion.settle())
    }

    suspend fun settle() = mutex.mutate(MutatePriority.UserInput) {
        offset.animateTo(selectedIndex.toFloat(), SonoraMotion.settle())
    }

    suspend fun select(index: Int) = mutex.mutate(MutatePriority.PreventUserInput) {
        offset.animateTo(index.coerceIn(0, albumCount - 1).toFloat(), SonoraMotion.settle())
    }
}

@Composable
fun rememberShelfState(albumCount: Int, initialIndex: Int = 3): ShelfState {
    return remember(albumCount) {
        ShelfState(albumCount, initialIndex.coerceIn(0, (albumCount - 1).coerceAtLeast(0)))
    }
}
