package com.sonora.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radial theme showcase: a hue wheel drawn with a sweep gradient,
 * tap-to-pick, plus saturation/brightness sliders with a live preview
 * swatch and HSV readout.
 */
@Composable
fun ThemeShowcase(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    var hue by remember { mutableStateOf(198f) }
    var saturation by remember { mutableStateOf(0.8f) }
    var brightness by remember { mutableStateOf(0.95f) }
    val accent = Color.hsv(hue, saturation, brightness)
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Accent color".uppercase(),
            style = type.labelCaps,
            color = colors.ivoryFaint,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(SonoraShapes.medium)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text("AC", style = type.body, color = if (isDarkEnough(accent)) Color.White else Color.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Hue ${hue.toInt()}°", style = type.caption, color = colors.ivory)
                Text(
                    "Sat ${(saturation * 100).toInt()}% · Value ${(brightness * 100).toInt()}%",
                    style = type.caption,
                    color = colors.ivoryDim,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // Hue wheel.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val sizePx = minOf(
                constraints.maxWidth,
                constraints.maxHeight - with(density) { 80.dp.toPx() }.toInt(),
            ).coerceIn(with(density) { 120.dp.toPx() }.toInt(), with(density) { 220.dp.toPx() }.toInt())
            Canvas(
                Modifier
                    .size(sizePx.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> hue = hueAt(offset, sizePx.toFloat()) }
                    },
            ) {
                val center = Offset(sizePx / 2f, sizePx / 2f)
                val r = sizePx / 2f
                drawCircle(
                    Brush.sweepGradient(
                        listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red),
                    ),
                    radius = r,
                    center = center,
                )
                val pointerRadius = with(density) { 10.dp.toPx() }
                // Pointer is drawn at the hue angle; hue 0 sits at +x.
                val angle = hue * PI / 180f
                val px = center.x + (r - pointerRadius) * cos(angle).toFloat()
                val py = center.y + (r - pointerRadius) * sin(angle).toFloat()
                drawCircle(
                    if (isDarkEnough(accent)) Color.White else Color.Black,
                    radius = pointerRadius,
                    center = Offset(px, py),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SliderBar(label = "Saturation", value = saturation) { saturation = it }
        SliderBar(label = "Value", value = brightness) { brightness = it }
    }
}

private fun isDarkEnough(color: Color): Boolean =
    color.red * 0.299 + color.green * 0.587 + color.blue * 0.114 < 0.5f

private fun hueAt(offset: Offset, sizePx: Float): Float {
    val center = Offset(sizePx / 2f, sizePx / 2f)
    val angle = atan2((-offset.y + center.y).toDouble(), (offset.x - center.x).toDouble())
    var h = Math.toDegrees(angle).toFloat()
    if (h < 0) h += 360f
    return h % 360f
}

@Composable
private fun SliderBar(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = type.labelCaps, color = colors.ivoryDim, modifier = Modifier.width(88.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(SonoraShapes.extraSmall)
                .background(colors.content.copy(alpha = 0.2f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value)
                    .height(6.dp)
                    .clip(SonoraShapes.extraSmall)
                    .background(colors.amber),
            )
        }
        Text(
            "${(value * 100).toInt()}%",
            style = type.caption,
            color = colors.ivoryDim,
            modifier = Modifier.width(40.dp),
        )
    }
}
