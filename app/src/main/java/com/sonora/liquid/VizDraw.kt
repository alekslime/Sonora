package com.sonora.liquid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Line-work companion to the accent: the preset body lifted toward
 * white so strands stay readable on the dark ground. Pure.
 */
fun vizLineAlt(preset: LiquidPreset): Color {
    val b = preset.body
    return Color(
        red = b.red + (1f - b.red) * 0.45f,
        green = b.green + (1f - b.green) * 0.45f,
        blue = b.blue + (1f - b.blue) * 0.45f,
    )
}

/**
 * Shared M9 visualizer dressing: a soft vignette that seats every
 * equation in the dark. Renderers draw it last.
 */
fun DrawScope.vizVignette() {
    drawRect(
        Brush.radialGradient(
            0f to Color.Transparent,
            0.62f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.55f),
            center = Offset(size.width / 2f, size.height * 0.46f),
            radius = maxOf(size.width, size.height) * 0.72f,
        ),
    )
}
