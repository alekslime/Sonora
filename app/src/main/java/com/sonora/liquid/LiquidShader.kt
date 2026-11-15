package com.sonora.liquid

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/**
 * M3 Liquid entry point. Uses the AGSL shader on API 33+ and the Canvas
 * fallback below. Rendering runs only while composed (Liquid visible).
 */
@Composable
fun LiquidCanvas(
    bands: Bands,
    config: LiquidConfig,
    motionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    // Autonomous drift freezes under reduced motion; the mass still
    // responds subtly as band levels change with the music.
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            timeSec += (now - last) / 1_000_000_000f
            last = now
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ShaderLiquid(bands = bands, config = config, timeSec = timeSec, modifier = modifier)
    } else {
        FallbackLiquid(bands = bands, config = config, timeSec = timeSec, modifier = modifier)
    }
}

private const val SHADER_SRC = """
uniform vec2 resolution;
uniform float time;
uniform float bass;
uniform float mid;
uniform float treble;
uniform float pulse;
uniform float intensity;
uniform float quality;
uniform vec3 colA;
uniform vec3 colB;
uniform vec3 colC;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.5 * noise(p);
    p *= 2.03;
    if (quality > 0.5) {
        v += 0.25 * noise(p);
        p *= 2.03;
    }
    if (quality > 1.5) {
        v += 0.125 * noise(p);
    }
    return v;
}

vec4 main(vec2 p) {
    vec2 uv = (p - resolution * 0.5) / (resolution.y * 0.5);
    float t = time;
    // Bass swells the domain warp; the mass breathes with the low end.
    float amp = (0.22 + 0.50 * bass) * intensity;
    vec2 w = vec2(fbm(uv * 1.5 + vec2(0.0, t * 0.16)),
                  fbm(uv * 1.5 + vec2(4.7, 1.9) - vec2(0.0, t * 0.12)));
    vec2 q = uv + (w - 0.5) * amp * 2.0;
    float d = length(q);
    float warp = fbm(q * 2.4 + vec2(1.7, t * 0.22));
    float edge = 0.46 - bass * 0.10 * intensity;
    float body = 1.0 - smoothstep(edge - 0.20, edge + 0.20, d + (warp - 0.5) * (0.45 + 0.55 * bass));
    float shaped = pow(body, 1.6);
    // Midrange ripples live on the mass only, kept subtle.
    float ripple = (sin(d * 90.0 - t * 6.0) * 0.5 + 0.5) * mid * shaped;
    // Treble shimmer is per-pixel grain, not lattice cells.
    float g = hash(p * 0.6 + vec2(t * 17.0, -t * 13.0));
    float shimmer = pow(g, 32.0) * treble * 1.6 * shaped;
    float rim = 1.0 - smoothstep(0.0, 0.035, abs(d - edge));
    vec2 cell = uv * 46.0;
    vec2 cellId = floor(cell);
    float tw = step(0.993, hash(cellId + floor(t * 1.5) * 0.13));
    float sparkleDot = 1.0 - smoothstep(0.0, 0.16, length(fract(cell) - 0.5));
    float sparkle = tw * sparkleDot * (0.35 + 0.65 * treble) * (1.0 - smoothstep(0.15, 0.85, d));
    vec3 col = mix(colA, colB * 1.5 + colC * 0.10, shaped);
    // Faux top light gives the mass volume.
    float shade = 0.82 + 0.30 * clamp(-q.y * 1.4, -1.0, 1.0);
    col *= mix(1.0, shade, shaped);
    col += colC * (rim * (0.35 + 0.65 * pulse) + ripple * 0.10 * intensity);
    col += vec3(1.0, 0.98, 0.95) * (shimmer * intensity + sparkle * 0.5);
    col += colB * pulse * shaped * 0.6;
    col *= 1.0 - smoothstep(0.55, 1.30, length(uv)) * 0.85;
    return vec4(col, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderLiquid(
    bands: Bands,
    config: LiquidConfig,
    timeSec: Float,
    modifier: Modifier = Modifier,
) {
    val shader = remember { RuntimeShader(SHADER_SRC) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val preset = config.preset
    Canvas(modifier.fillMaxSize()) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", timeSec)
        shader.setFloatUniform("bass", bands.bass)
        shader.setFloatUniform("mid", bands.mid)
        shader.setFloatUniform("treble", bands.treble)
        shader.setFloatUniform("pulse", bands.pulse)
        shader.setFloatUniform("intensity", config.intensity)
        shader.setFloatUniform(
            "quality",
            when (config.quality) {
                LiquidQuality.LOW -> 0f
                LiquidQuality.MEDIUM -> 1f
                LiquidQuality.HIGH -> 2f
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
