package com.sonora.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonora.gesture.GestureConfigStore
import kotlinx.coroutines.delay

/**
 * Animated "this is playing right now" marker: a softly pulsing dot that
 * rides in front of the row's artwork. Pass a subtle accent color so the
 * dot reads as activity, not decoration. Honors reduced motion: when
 * the user prefers it, the dot renders static.
 */
@Composable
fun NowPlayingIndicator(color: Color, modifier: Modifier = Modifier) {
    var pulse by remember { mutableFloatStateOf(0.3f) }
    LaunchedEffect(Unit) {
        if (GestureConfigStore.config.reducedMotion) return@LaunchedEffect
        while (true) {
            pulse = 1f
            delay(700)
            pulse = 0.3f
            delay(650)
        }
    }
    val alpha by animateFloatAsState(
        targetValue = pulse,
        animationSpec = tween(420, easing = EaseInOut),
        label = "playing-pulse",
    )
    Box(modifier.size(8.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(alpha)
                .background(color, CircleShape),
        )
    }
}
