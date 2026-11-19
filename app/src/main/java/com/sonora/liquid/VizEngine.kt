package com.sonora.liquid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** How long each equation holds before the show auto-advances. */
const val VIZ_CYCLE_MS = 12_000L

/** Crossfade between equations; both render during the fade. */
const val VIZ_FADE_MS = 1_500L

/**
 * Frame-rate band follower. The analyzer publishes at ~20 Hz, so the
 * renderer interpolates toward the latest reading every frame: instant
 * attack for kick response, slower release so decays glide. Pure.
 */
fun smoothBand(
    current: Float,
    target: Float,
    dtSec: Float,
    attackPerSec: Float = 18f,
    releasePerSec: Float = 6f,
): Float {
    if (dtSec <= 0f) return current
    val rate = if (target > current) attackPerSec else releasePerSec
    val t = (rate * dtSec).coerceIn(0f, 1f)
    return current + (target - current) * t
}

/** Applies [smoothBand] to every channel, including the pulse. */
fun smoothBands(
    current: Bands,
    target: Bands,
    dtSec: Float,
    attackPerSec: Float = 18f,
    releasePerSec: Float = 6f,
): Bands {
    return Bands(
        bass = smoothBand(current.bass, target.bass, dtSec, attackPerSec, releasePerSec),
        mid = smoothBand(current.mid, target.mid, dtSec, attackPerSec, releasePerSec),
        treble = smoothBand(current.treble, target.treble, dtSec, attackPerSec, releasePerSec),
        pulse = smoothBand(current.pulse, target.pulse, dtSec, attackPerSec, releasePerSec),
    )
}

/**
 * M9 show state. Owns the current equation, the outgoing one during a
 * crossfade, and the auto-cycle clock. The render clock lives outside
 * (it never resets), which is what keeps transitions seamless.
 */
class VizShow(startMode: VizMode, autoCycle: Boolean) {

    var current by mutableStateOf(startMode)
        private set

    var outgoing by mutableStateOf<VizMode?>(null)
        private set

    /** 0 = fully outgoing, 1 = fully current. */
    var fade by mutableFloatStateOf(1f)
        private set

    var autoCycle by mutableStateOf(autoCycle)

    private var cycleClockMs = 0L
    private var fadeClockMs = 0L

    fun next() {
        val modes = VizMode.entries
        go(modes[(current.ordinal + 1) % modes.size])
    }

    fun prev() {
        val modes = VizMode.entries
        go(modes[(current.ordinal + modes.size - 1) % modes.size])
    }

    private fun go(mode: VizMode) {
        if (mode == current) return
        outgoing = current
        current = mode
        fade = 0f
        fadeClockMs = 0L
        cycleClockMs = 0L
    }

    /** Advances the fade and cycle clocks; call every frame. */
    fun frame(elapsedMs: Long) {
        if (outgoing != null) {
            fadeClockMs += elapsedMs
            fade = (fadeClockMs.toFloat() / VIZ_FADE_MS).coerceIn(0f, 1f)
            if (fade >= 1f) outgoing = null
        } else if (autoCycle) {
            cycleClockMs += elapsedMs
            if (cycleClockMs >= VIZ_CYCLE_MS) {
                cycleClockMs = 0L
                next()
            }
        }
    }
}
