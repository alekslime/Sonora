package com.sonora.liquid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val STORM_MAX = 140

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var bright: Boolean,
)

/**
 * Beat spawn rate in particles/sec. Pure.
 */
fun stormBeatRate(beat: Float): Float = beat.coerceIn(0f, 1f) * 600f

/**
 * Ambient spawn rate in particles/sec from the band energy. Pure.
 */
fun stormAmbientRate(energy: Float): Float = energy.coerceIn(0f, 1f) * 150f

/**
 * M10 Storm: beat-emitted particles riding the spectrum. Each mote
 * takes its velocity from a spectral bin — angle from position,
 * speed from magnitude — and draws its own fading trail.
 */
@Composable
fun StormViz(
    signal: VizSignal,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    modifier: Modifier = Modifier,
) {
    val pool = remember { mutableListOf<Particle>() }
    val lastT = remember { mutableFloatStateOf(timeSec) }
    val debt = remember { mutableFloatStateOf(0f) }
    val random = remember { Random(99L) }
    Canvas(modifier.fillMaxSize()) {
        val dt = (timeSec - lastT.floatValue).coerceAtLeast(0f).coerceAtMost(0.1f)
        lastT.floatValue = timeSec
        drawRect(preset.deep)
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.46f)
        val unit = minOf(w, h)
        val spectrum = signal.frame.spectrum
        // Ambient drift plus beat bursts; fractional spawn
        // accumulates across frames so low rates stay smooth.
        // Silence (zero energy, zero pulse) spawns nothing.
        val energy = (signal.bands.bass + signal.bands.mid) / 2f
        val rate = (stormAmbientRate(energy) + stormBeatRate(signal.bands.pulse)) *
            (0.4f + 0.6f * intensity)
        debt.floatValue += rate * dt
        val budget = debt.floatValue.toInt().coerceAtLeast(0)
        debt.floatValue -= budget
        repeat(budget) {
            if (pool.size >= STORM_MAX) pool.removeFirst()
            pool.add(spawnParticle(spectrum, center, unit, random))
        }
        val accent = preset.accent
        val alt = vizLineAlt(preset)
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            val px = p.x
            val py = p.y
            p.x += p.vx * dt
            p.y += p.vy * dt
            // Gentle drag so trails curl to a stop.
            p.vx *= (1f - 1.6f * dt).coerceAtLeast(0f)
            p.vy *= (1f - 1.6f * dt).coerceAtLeast(0f)
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            val color = (if (p.bright) accent else alt).copy(alpha = 0.85f * a)
            drawLine(color, Offset(px, py), Offset(p.x, p.y), strokeWidth = 4f)
            drawCircle(color, radius = 2.5f, center = Offset(p.x, p.y))
        }
        vizVignette()
    }
}

private fun spawnParticle(
    spectrum: FloatArray,
    center: Offset,
    unit: Float,
    random: Random,
): Particle {
    val bin = if (spectrum.isEmpty()) 0 else (random.nextFloat() * spectrum.size).toInt()
    val mag = spectrum.getOrElse(bin) { 0f }
    val angle = (bin.toFloat() / spectrum.size.coerceAtLeast(1)) * 2f * PI.toFloat() +
        (random.nextFloat() - 0.5f) * 0.6f
    val speed = unit * (0.25f + 1.4f * mag) * (0.7f + random.nextFloat() * 0.6f)
    val life = 0.8f + random.nextFloat() * 0.8f
    return Particle(
        x = center.x + (random.nextFloat() - 0.5f) * unit * 0.06f,
        y = center.y + (random.nextFloat() - 0.5f) * unit * 0.06f,
        vx = cos(angle) * speed,
        vy = sin(angle) * speed,
        life = life,
        maxLife = life,
        bright = random.nextFloat() > 0.45f,
    )
}
