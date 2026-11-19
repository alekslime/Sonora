package com.sonora.liquid

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * M9 Mandelbrot entry point: the AGSL escape-time fractal on API 33+
 * and a low-res CPU bitmap below. Bass breathes the zoom, mid turns
 * the plane, treble drifts the palette, and the kick flashes the rim.
 */
@Composable
fun MandelbrotViz(
    bands: Bands,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    quality: LiquidQuality,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderMandelbrot(
            bands = bands,
            preset = preset,
            timeSec = timeSec,
            intensity = intensity,
            quality = quality,
            modifier = modifier,
        )
    } else {
        CpuMandelbrot(
            bands = bands,
            preset = preset,
            timeSec = timeSec,
            intensity = intensity,
            modifier = modifier,
        )
    }
}

private const val MANDELBROT_SRC = """
uniform vec2 resolution;
uniform float time;
uniform float bass;
uniform float mid;
uniform float treble;
uniform float pulse;
uniform float intensity;
uniform float iters;
uniform vec3 colA;
uniform vec3 colB;
uniform vec3 colC;

vec4 main(vec2 p) {
    vec2 uv = (p - resolution * 0.5) / resolution.y;
    float t = time;
    float zoom = 1.0 + bass * 1.4 * intensity + pulse * 0.8;
    float rot = t * 0.03 + (mid - 0.5) * 0.6;
    float cr = cos(rot);
    float sr = sin(rot);
    vec2 cuv = vec2(uv.x * cr - uv.y * sr, uv.x * sr + uv.y * cr);
    vec2 center = vec2(-0.62 + 0.08 * sin(t * 0.05), 0.02 + 0.08 * cos(t * 0.043));
    vec2 c = center + cuv * (2.4 / zoom);
    vec2 z = vec2(0.0);
    float n = 0.0;
    for (int i = 0; i < 96; i++) {
        if (float(i) >= iters) break;
        z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
        if (dot(z, z) > 4.0) break;
        n += 1.0;
    }
    float m = n / iters;
    vec3 col;
    if (n >= iters - 0.5) {
        col = colA * 0.25;
    } else {
        float band = fract(m * 3.0 + t * 0.02 + treble * 0.2);
        col = mix(colA, colB, smoothstep(0.0, 0.6, m));
        col = mix(col, colC, pow(band, 3.0) * 0.9);
        col += colC * pulse * 0.25 * (1.0 - m);
    }
    col *= 1.0 - smoothstep(0.5, 1.3, length(uv)) * 0.7;
    return vec4(col, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderMandelbrot(
    bands: Bands,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    quality: LiquidQuality,
    modifier: Modifier = Modifier,
) {
    val shader = remember { RuntimeShader(MANDELBROT_SRC) }
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(modifier.fillMaxSize()) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", timeSec)
        shader.setFloatUniform("bass", bands.bass)
        shader.setFloatUniform("mid", bands.mid)
        shader.setFloatUniform("treble", bands.treble)
        shader.setFloatUniform("pulse", bands.pulse)
        shader.setFloatUniform("intensity", intensity)
        shader.setFloatUniform(
            "iters",
            when (quality) {
                LiquidQuality.LOW -> 32f
                LiquidQuality.MEDIUM -> 56f
                LiquidQuality.HIGH -> 88f
            },
        )
        shader.setFloatUniform("colA", rgb(preset.deep))
        shader.setFloatUniform("colB", rgb(preset.body))
        shader.setFloatUniform("colC", rgb(preset.accent))
        drawRect(brush = brush)
    }
}

private fun rgb(color: Color): FloatArray {
    return floatArrayOf(color.red, color.green, color.blue)
}

/**
 * CPU Mandelbrot for API < 33. Renders small (~96 px wide) on a
 * background thread at ~15 fps and upscales chunky; the same zoom
 * and palette math as the shader, so the show matches everywhere.
 */
@Composable
private fun CpuMandelbrot(
    bands: Bands,
    preset: LiquidPreset,
    timeSec: Float,
    intensity: Float,
    modifier: Modifier = Modifier,
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    val latestBands by rememberUpdatedState(bands)
    val latestTime by rememberUpdatedState(timeSec)
    val ramp = remember(preset) { mandelbrotRamp(preset) }
    LaunchedEffect(ramp) {
        while (isActive) {
            val size = viewSize
            if (size.width > 0 && size.height > 0) {
                val bitmap = withContext(Dispatchers.Default) {
                    renderMandelbrotGrid(
                        width = CPU_WIDTH,
                        height = (CPU_WIDTH.toFloat() * size.height / size.width).toInt().coerceIn(1, 320),
                        bands = latestBands,
                        timeSec = latestTime,
                        intensity = intensity,
                        ramp = ramp,
                    )
                }
                frame = bitmap
            }
            delay(66L)
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it },
    ) {
        val current = frame
        Canvas(Modifier.fillMaxSize()) {
            drawRect(preset.deep)
            if (current != null) {
                drawImage(
                    image = current.asImageBitmap(),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None,
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            vizVignette()
        }
    }
}

private const val CPU_WIDTH = 96
private const val CPU_ITERS = 28

private fun mandelbrotRamp(preset: LiquidPreset): IntArray {
    val deep = preset.deep
    val body = preset.body
    val accent = preset.accent
    return IntArray(64) { i ->
        val m = i / 63f
        val base = lerpColor(deep, body, smoothstep(0f, 0.6f, m))
        val band = ((m * 3f) % 1f + 1f) % 1f
        val mixed = lerpColor(base, accent, band * band * band * 0.9f)
        android.graphics.Color.rgb(
            (mixed.red * 255).toInt().coerceIn(0, 255),
            (mixed.green * 255).toInt().coerceIn(0, 255),
            (mixed.blue * 255).toInt().coerceIn(0, 255),
        )
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * k,
        green = a.green + (b.green - a.green) * k,
        blue = a.blue + (b.blue - a.blue) * k,
    )
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun renderMandelbrotGrid(
    width: Int,
    height: Int,
    bands: Bands,
    timeSec: Float,
    intensity: Float,
    ramp: IntArray,
): Bitmap {
    val pixels = IntArray(width * height)
    val zoom = 1f + bands.bass * 1.4f * intensity + bands.pulse * 0.8f
    val rot = timeSec * 0.03f + (bands.mid - 0.5f) * 0.6f
    val cr = kotlin.math.cos(rot)
    val sr = kotlin.math.sin(rot)
    val cx = -0.62f + 0.08f * kotlin.math.sin(timeSec * 0.05f)
    val cy = 0.02f + 0.08f * kotlin.math.cos(timeSec * 0.043f)
    for (y in 0 until height) {
        for (x in 0 until width) {
            // Match the shader: normalize by height, then rotate.
            val uvx = (x - width / 2f) / height * 2.4f / zoom
            val uvy = (y - height / 2f) / height * 2.4f / zoom
            val rx = uvx * cr - uvy * sr
            val ry = uvx * sr + uvy * cr
            var zx = 0f
            var zy = 0f
            var n = 0
            val ccx = cx + rx
            val ccy = cy + ry
            while (n < CPU_ITERS) {
                val zx2 = zx * zx - zy * zy + ccx
                zy = 2f * zx * zy + ccy
                zx = zx2
                if (zx * zx + zy * zy > 4f) break
                n++
            }
            pixels[y * width + x] = if (n >= CPU_ITERS) {
                android.graphics.Color.rgb(8, 8, 10)
            } else {
                ramp[(n.toFloat() / CPU_ITERS * (ramp.size - 1)).toInt()]
            }
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
