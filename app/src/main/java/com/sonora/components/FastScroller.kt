package com.sonora.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content
import com.sonora.theme.contentInverse
import kotlinx.coroutines.launch

/**
 * M15 fast scroller for long song lists. Drag the rail to jump;
 * a bubble names the section under your thumb. Silent on short lists.
 */
@Composable
fun BoxScope.FastScroller(
    listState: LazyListState,
    sectionFor: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(40.dp),
    ) {
        val total = listState.layoutInfo.totalItemsCount
        if (total <= 8) return@BoxWithConstraints
        val heightPx = with(density) { maxHeight.toPx() }
        fun jumpTo(yPx: Float) {
            val fraction = (yPx / heightPx).coerceIn(0f, 1f)
            scope.launch {
                listState.scrollToItem((fraction * total).toInt().coerceIn(0, total - 1))
            }
        }
        val first = listState.firstVisibleItemIndex
        val fraction = if (total <= 1) 0f else (first.toFloat() / (total - 1)).coerceIn(0f, 1f)
        val thumbPx = with(density) { 56.dp.toPx() }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 10.dp)
                .offset { IntOffset(0, (fraction * (heightPx - thumbPx)).toInt()) }
                .width(4.dp)
                .height(56.dp)
                .clip(CircleShape)
                .background(
                    if (dragging) colors.content else colors.content.copy(alpha = 0.28f),
                ),
        )
        if (dragging) {
            Text(
                sectionFor(first.coerceIn(0, total - 1)),
                style = type.title,
                color = colors.contentInverse,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.content)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(total) {
                    detectTapGestures(onTap = { jumpTo(it.y) })
                }
                .pointerInput(total) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragging = true
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ ->
                            jumpTo(change.position.y)
                            change.consume()
                        },
                    )
                },
        )
    }
}

/**
 * M15 row entrance: a short fade-and-rise staggered by index. Rows
 * keep their state once in, so the motion plays on first load only.
 */
@Composable
fun RowEnter(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = tween(220, delayMillis = index.coerceIn(0, 8) * 30),
        ) + slideInVertically(
            animationSpec = tween(220, delayMillis = index.coerceIn(0, 8) * 30),
            initialOffsetY = { it / 6 },
        ),
        label = "row-enter",
    ) {
        Box(modifier) { content() }
    }
}
