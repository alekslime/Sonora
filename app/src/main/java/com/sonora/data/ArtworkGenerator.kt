package com.sonora.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural M1 cover art. Each album seed deterministically yields one of
 * six compositions in a rich, saturated palette so the dark library reads the
 * way the spec intends: artwork supplies the color.
 *
 * Runs on a background dispatcher; callers must never invoke it on the UI
 * thread. Output is always square; callers must preserve the aspect ratio.
 */
object ArtworkGenerator {

    private data class Palette(val deep: Int, val primary: Int, val secondary: Int, val accent: Int)

    private val PALETTES = listOf(
        Palette(0xFF0E2A30.toInt(), 0xFF1E7E74.toInt(), 0xFFE0A458.toInt(), 0xFFF2EDE3.toInt()), // teal / amber
        Palette(0xFF241536.toInt(), 0xFF6C3FA3.toInt(), 0xFFE26D3D.toInt(), 0xFFF5D76E.toInt()), // violet / ember
        Palette(0xFF0F1F3D.toInt(), 0xFF2E5FA3.toInt(), 0xFFE2574C.toInt(), 0xFFF2EDE3.toInt()), // navy / coral
        Palette(0xFF14261A.toInt(), 0xFF3E7A3A.toInt(), 0xFFB8D94A.toInt(), 0xFFF2EDE3.toInt()), // forest / lime
        Palette(0xFF2E1420.toInt(), 0xFF8A2E3F.toInt(), 0xFFE0A458.toInt(), 0xFFF2EDE3.toInt()), // maroon / gold
        Palette(0xFF1A2233.toInt(), 0xFF4A6FA5.toInt(), 0xFF9AD1D4.toInt(), 0xFFE0A458.toInt()), // slate / ice
        Palette(0xFF2B1B2E.toInt(), 0xFF7A4A6E.toInt(), 0xFF7BD389.toInt(), 0xFFF2EDE3.toInt()), // plum / mint
        Palette(0xFF1C1C20.toInt(), 0xFFB03A2E.toInt(), 0xFFE67E22.toInt(), 0xFFF2EDE3.toInt()), // charcoal / ember
    )

    fun generate(seed: Long, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(96, 1024)
        val random = Random(seed)
        val palette = PALETTES[(random.nextInt() and Int.MAX_VALUE) % PALETTES.size]
        val style = (random.nextInt() and Int.MAX_VALUE) % 6

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (style) {
            0 -> drawGrooves(canvas, paint, size, palette, random)
            1 -> drawBeams(canvas, paint, size, palette, random)
            2 -> drawPrism(canvas, paint, size, palette, random)
            3 -> drawSunburst(canvas, paint, size, palette, random)
            4 -> drawHorizon(canvas, paint, size, palette, random)
            else -> drawOrbit(canvas, paint, size, palette, random)
        }
        drawGrain(canvas, size, random)
        drawVignette(canvas, paint, size)
        return bitmap
    }

    // --- Compositions -----------------------------------------------------

    private fun drawGrooves(canvas: Canvas, paint: Paint, size: Int, p: Palette, r: Random) {
        val s = size.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            s * 0.5f, s * 0.42f, s * 0.75f, p.primary, p.deep, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, s, s, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        val cx = s * (0.42f + r.nextFloat() * 0.16f)
        val cy = s * (0.42f + r.nextFloat() * 0.16f)
        val rings = 26 + r.nextInt(14)
        for (i in 1..rings) {
            val radius = (i.toFloat() / rings) * s * 0.62f
            paint.strokeWidth = s * 0.0022f
            paint.color = withAlpha(if (i % 5 == 0) p.accent else p.secondary, 0.10f + 0.10f * r.nextFloat())
            canvas.drawCircle(cx, cy, radius, paint)
        }
        // Center label.
        paint.style = Paint.Style.FILL
        paint.color = p.accent
        canvas.drawCircle(cx, cy, s * 0.13f, paint)
        paint.color = p.deep
        canvas.drawCircle(cx, cy, s * 0.035f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.006f
        paint.color = withAlpha(p.deep, 0.55f)
        canvas.drawCircle(cx, cy, s * 0.10f, paint)
    }

    private fun drawBeams(canvas: Canvas, paint: Paint, size: Int, p: Palette, r: Random) {
        val s = size.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, s, s, p.deep, p.primary, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, s, s, paint)
        paint.shader = null
        val beams = 3 + r.nextInt(3)
        for (i in 0 until beams) {
            val t = i.toFloat() / beams
            val beamColor = if (i % 2 == 0) p.secondary else p.accent
            paint.color = withAlpha(beamColor, 0.24f + 0.20f * r.nextFloat())
            val x0 = s * (t * 1.3f - 0.15f)
            val w = s * (0.05f + r.nextFloat() * 0.10f)
            val slant = s * 0.35f
            val path = Path().apply {
                moveTo(x0, -s * 0.05f)
                lineTo(x0 + w, -s * 0.05f)
                lineTo(x0 + w + slant, s * 1.05f)
                lineTo(x0 + slant, s * 1.05f)
                close()
            }
            canvas.drawPath(path, paint)
        }
        // Dark equalizer-like bars along the base for a musical read.
        val bars = 18
        for (i in 0 until bars) {
            val h = s * (0.06f + 0.22f * r.nextFloat() * r.nextFloat())
            paint.color = withAlpha(0xFF000000.toInt(), 0.35f)
            val bw = s / bars
            canvas.drawRect(i * bw + bw * 0.2f, s - h, (i + 1) * bw - bw * 0.2f, s, paint)
        }
    }

    private fun drawPrism(canvas: Canvas, paint: Paint, size: Int, p: Palette, r: Random) {
        val s = size.toFloat()
        canvas.drawColor(p.deep)
        val cx = s * (0.38f + r.nextFloat() * 0.10f)
        val cy = s * (0.40f + r.nextFloat() * 0.10f)
        val tri = s * 0.24f
        // Entry beam.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.008f
        paint.color = withAlpha(p.accent, 0.85f)
        canvas.drawLine(-s * 0.05f, cy - tri * 0.35f, cx - tri * 0.45f, cy - tri * 0.12f, paint)
        // Spectrum fan.
        val spectrum = intArrayOf(
            0xFFE2574C.toInt(), 0xFFE67E22.toInt(), 0xFFF5D76E.toInt(),
            0xFF7BD389.toInt(), 0xFF4A90D9.toInt(), 0xFF7A5FD0.toInt(),
        )
        val fanX = s * 1.05f
        for (i in spectrum.indices) {
            paint.strokeWidth = s * (0.016f + 0.004f * r.nextFloat())
            paint.color = withAlpha(spectrum[i], 0.9f)
            val spread = (i - (spectrum.size - 1) / 2f) * s * 0.055f
            canvas.drawLine(cx + tri * 0.35f, cy + tri * 0.18f, fanX, cy + tri * 0.18f + spread * 2.4f, paint)
        }
        // Prism triangle.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.010f
        paint.color = p.accent
        val path = Path().apply {
            moveTo(cx, cy - tri)
            lineTo(cx + tri * 0.92f, cy + tri * 0.62f)
            lineTo(cx - tri * 0.92f, cy + tri * 0.62f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(p.accent, 0.10f)
        canvas.drawPath(path, paint)
    }

    private fun drawSunburst(canvas: Canvas, paint: Paint, size: Int, p: Palette, r: Random) {
        val s = size.toFloat()
        val fx = s * (0.30f + r.nextFloat() * 0.40f)
        val fy = s * (0.30f + r.nextFloat() * 0.40f)
        canvas.drawColor(p.deep)
        paint.style = Paint.Style.FILL
        val rays = 26 + r.nextInt(10)
        val maxR = s * 1.1f
        for (i in 0 until rays) {
            if (i % 2 == 1) continue
            val a0 = (i.toFloat() / rays) * (Math.PI * 2)
            val a1 = ((i + 1).toFloat() / rays) * (Math.PI * 2)
            paint.color = withAlpha(if (i % 4 == 0) p.primary else p.secondary, 0.5f)
            val path = Path().apply {
                moveTo(fx, fy)
                lineTo(fx + cos(a0).toFloat() * maxR, fy + sin(a0).toFloat() * maxR)
                lineTo(fx + cos(a1).toFloat() * maxR, fy + sin(a1).toFloat() * maxR)
                close()
            }
            canvas.drawPath(path, paint)
        }
        paint.shader = RadialGradient(fx, fy, s * 0.30f, p.accent, withAlpha(p.accent, 0f), Shader.TileMode.CLAMP)
        canvas.drawCircle(fx, fy, s * 0.30f, paint)
        paint.shader = null
        paint.color = p.accent
        canvas.drawCircle(fx, fy, s * 0.075f, paint)
    }

    private fun drawHorizon(canvas: Canvas, paint: Paint, size: Int, p: Palette, r: Random) {
        val s = size.toFloat()
        paint.style = Paint.Style.FILL
        val bands = listOf(p.deep, p.primary, p.secondary, p.accent, p.secondary, p.deep)
        val horizonAt = 0.52f + r.nextFloat() * 0.12f
        bands.forEachIndexed { i, color ->
            val top = s * horizonAt * i / (bands.size - 1) * 1.6f
            val bottom = s * horizonAt * (i + 1) / (bands.size - 1) * 1.6f
            paint.color = color
            canvas.drawRect(0f, min(top, s), s, min(bottom, s), paint)
        }
        // Sun disc half-sunk on the horizon.
        val sunX = s * (0.30f + r.nextFloat() * 0.40f)
        paint.color = p.accent
        canvas.drawCircle(sunX, s * horizonAt, s * 0.11f, paint)
        paint.color = withAlpha(p.deep, 0.55f)
        var stripeY = s * horizonAt - s * 0.08f
        while (stripeY < s * horizonAt + s * 0.10f) {
            canvas.drawRect(sunX - s * 0.11f, stripeY, sunX + s * 0.11f, stripeY + s * 0.012f, paint)
            stripeY += s * 0.035f
        }
        // Dark foreground.
        paint.color = withAlpha(0xFF000000.toInt(), 0.72f)
        canvas.drawRect(0f, s * horizonAt + s * 0.10f, s, s, paint)
        // Water reflection.
        paint.color = withAlpha(p.secondary, 0.5f)
        val streaks = 8 + r.nextInt(6)
        repeat(streaks) {
            val y = s * horizonAt + s * 0.10f + r.nextFloat() * s * (0.90f - horizonAt - 0.10f)
            val w = s * (0.04f + r.nextFloat() * 0.16f)
            val x = sunX - w / 2 + (r.nextFloat() - 0.5f) * s * 0.2f
            canvas.drawRect(x, y, x + w, y + s * 0.008f, paint)
        }
    }

    private fun drawOrbit(canvas: Canvas, paint: Paint, size: Int, p: Palette, r: Random) {
        val s = size.toFloat()
        canvas.drawColor(p.deep)
        paint.style = Paint.Style.FILL
        // Starfield.
        repeat(140) {
            paint.color = withAlpha(p.accent, 0.15f + 0.65f * r.nextFloat() * r.nextFloat())
            val star = s * (0.0012f + r.nextFloat() * 0.0022f)
            canvas.drawCircle(r.nextFloat() * s, r.nextFloat() * s, star, paint)
        }
        // Planets.
        val planets = 2 + r.nextInt(2)
        repeat(planets) { i ->
            val px = s * (0.20f + r.nextFloat() * 0.60f)
            val py = s * (0.22f + r.nextFloat() * 0.56f)
            val pr = s * (0.06f + r.nextFloat() * 0.11f)
            val body = listOf(p.primary, p.secondary, p.accent)[i % 3]
            paint.shader = RadialGradient(
                px - pr * 0.35f, py - pr * 0.35f, pr * 1.4f, body, p.deep, Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(px, py, pr, paint)
            paint.shader = null
            if (i == 0) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s * 0.008f
                paint.color = withAlpha(p.accent, 0.75f)
                val ring = RectF(px - pr * 1.7f, py - pr * 0.55f, px + pr * 1.7f, py + pr * 0.55f)
                canvas.save()
                canvas.rotate(-18f, px, py)
                canvas.drawOval(ring, paint)
                canvas.restore()
                paint.style = Paint.Style.FILL
            }
        }
    }

    // --- Finish -----------------------------------------------------------

    private fun drawGrain(canvas: Canvas, size: Int, r: Random) {
        val paint = Paint().apply { style = Paint.Style.FILL }
        val dots = size * 6
        repeat(dots) {
            val light = r.nextBoolean()
            paint.color = if (light) 0x14FFFFFF else 0x14000000
            val x = r.nextFloat() * size
            val y = r.nextFloat() * size
            canvas.drawRect(x, y, x + 1.5f, y + 1.5f, paint)
        }
    }

    private fun drawVignette(canvas: Canvas, paint: Paint, size: Int) {
        val s = size.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            s / 2, s / 2, s * 0.72f,
            intArrayOf(0x00000000, 0x55000000),
            floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, s, s, paint)
        paint.shader = null
    }

    private fun withAlpha(argb: Int, alpha: Float): Int {
        val a = (argb ushr 24) * alpha.coerceIn(0f, 1f)
        return (argb and 0x00FFFFFF) or (a.toInt().coerceIn(0, 255) shl 24)
    }
}
