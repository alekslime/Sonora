package com.sonora.playback

import kotlin.math.cos
import kotlin.math.sin

/**
 * M6 crossfade math. The service runs a two-player fade: the outgoing
 * track ramps down on one player while the pre-cued incoming track ramps
 * up on the other, then the players swap roles.
 */
object Crossfade {

    /**
     * Equal-power gains (outgoing, incoming) for progress [t] in 0..1.
     * Equal-power keeps perceived loudness steady through the blend,
     * where a linear ramp would audibly dip. Pure and tested.
     */
    fun fadeGains(t: Float): Pair<Float, Float> {
        val clamped = t.coerceIn(0f, 1f) * (Math.PI / 2).toFloat()
        return Pair(cos(clamped), sin(clamped))
    }
}
