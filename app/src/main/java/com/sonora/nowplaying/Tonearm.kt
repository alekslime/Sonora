package com.sonora.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

/** Stylus clock angle on the record (screen degrees, 0 = east). */
fun stylusAngleDeg(fraction: Float): Float = lerp(10f, -18f, fraction)

/** Stylus distance from the record center. */
fun stylusRadiusPx(fraction: Float, outerR: Float, labelR: Float): Float =
    lerp(outerR * 0.98f, labelR * 1.2f, fraction)

/** Where the needle sits for a given playback fraction. Pure and tested. */
fun stylusPoint(fraction: Float, center: Offset, outerR: Float, labelR: Float): Offset {
    val radians = Math.toRadians(stylusAngleDeg(fraction).toDouble())
    val radius = stylusRadiusPx(fraction, outerR, labelR)
    return Offset(
        center.x + cos(radians).toFloat() * radius,
        center.y + sin(radians).toFloat() * radius,
    )
}

/**
 * The M2 tonearm: pivot base, counterweight stub, arm tube and headshell.
 * The stylus rides from the outer groove to the label as [fraction]
 * advances; [cueLiftPx] raises the arm for needle drops and track changes.
 */
@Composable
fun Tonearm(
    pivot: Offset,
    stylus: Offset,
    cueLiftPx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        val lift = Offset(0f, -cueLiftPx)
        val p = pivot + lift
        val s = stylus + lift
        val silver = Color(0xFF9AA0A6)
        val darkMetal = Color(0xFF33333A)

        // Counterweight stub behind the pivot, opposite the stylus.
        val backX = p.x - s.x
        val backY = p.y - s.y
        val backLen = (kotlin.math.hypot(backX, backY)).coerceAtLeast(1f)
        val stub = Offset(p.x + backX / backLen * 42f, p.y + backY / backLen * 42f)
        drawLine(darkMetal, p, stub, strokeWidth = 12f, cap = StrokeCap.Round)
        drawCircle(darkMetal, 16f, stub)
        drawCircle(Color(0xFF1C1C20), 16f, stub, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))

        // Arm tube from pivot to stylus.
        drawLine(silver, p, s, strokeWidth = 10f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.25f), p, s, strokeWidth = 3f, cap = StrokeCap.Round)

        // Headshell angled along the arm at the stylus.
        val armAngle = Math.toDegrees(atan2(s.y - p.y, s.x - p.x).toDouble()).toFloat()
        rotate(armAngle, s) {
            drawRect(
                color = Color(0xFF1C1C20),
                topLeft = Offset(s.x - 5f, s.y - 9f),
                size = androidx.compose.ui.geometry.Size(38f, 18f),
            )
            drawRect(
                color = Color(0xFFE0A458),
                topLeft = Offset(s.x + 28f, s.y - 9f),
                size = androidx.compose.ui.geometry.Size(5f, 18f),
            )
        }
        // Stylus tip.
        drawCircle(Color.White.copy(alpha = 0.8f), 3f, s)

        // Pivot base with a restrained amber collar.
        drawCircle(Color(0xFF1C1C20), 32f, p)
        drawCircle(darkMetal, 32f, p, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
        drawCircle(Color(0xFFE0A458).copy(alpha = 0.8f), 21f, p, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        drawCircle(silver, 8f, p)
    }
}
