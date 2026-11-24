package com.sonora.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sonora.theme.LocalSonoraColors

/**
 * Swipe-left reveal on a row: the content stays put while the row is
 * dragged left; the action chip appears past a threshold and fires on
 * drag end (or release). Used by SongRow for "add to queue" actions.
 */
@Composable
fun SwipeLeftActions(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    onAction: () -> Unit,
) {
    val colors = LocalSonoraColors.current
    var offset by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .offset { androidx.compose.ui.unit.IntOffset(offset.toInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset = 0f },
                    onDragEnd = {
                        if (offset < -100f) onAction()
                        offset = 0f
                    },
                    onDragCancel = { offset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        offset += dragAmount
                        if (offset > 0f) offset = 0f
                        change.consume()
                    },
                )
            }
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        // Action chip revealed while swiping.
        if (offset < -80f) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(18.dp))
            }
        }
        content()
    }
}
