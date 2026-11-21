package com.sonora.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content

/**
 * M13 cover art with a shimmer sweep while loading and the seeded
 * letter underneath. Shape and size come from the caller.
 */
@Composable
fun CoverArt(
    art: ImageBitmap?,
    fallbackLetter: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    Box(
        modifier.background(
            if (art == null) rememberShimmerBrush()
            else SolidColor(if (colors.isLight) Color(0xFFE7E1D4) else Color(0xFF1A1A1A)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                fallbackLetter,
                style = type.display,
                color = colors.content.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val light = LocalSonoraColors.current.isLight
    val transition = rememberInfiniteTransition(label = "cover-shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "sweep",
    )
    val x = -400f + offset * 1200f
    val stops = if (light) {
        listOf(Color(0xFFE3DDD0), Color(0xFFF4F0E6), Color(0xFFE3DDD0))
    } else {
        listOf(Color(0xFF1E1E2E), Color(0xFF2E2E40), Color(0xFF1E1E2E))
    }
    return Brush.linearGradient(
        stops,
        start = Offset(x, 0f),
        end = Offset(x + 400f, 300f),
    )
}

/**
 * M13 light tick for glass controls. Read once per control cluster
 * and call inside the click handler.
 */
@Composable
fun rememberTapTick(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
}
