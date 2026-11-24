package com.sonora.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sonora.components.rememberTapTick
import com.sonora.data.Album
import com.sonora.data.Track
import com.sonora.data.toMinSec
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes
import com.sonora.theme.content

/**
 * M8 Up Next sheet: the album's tracks with the playing one marked.
 * Swipe an entry to the left to remove it from the queue (the row
 * slides into a wash), and drag vertically to reorder. A tap jumps
 * straight to that track, real or demo.
 */
@Composable
fun QueueSheet(
    album: Album,
    currentIndex: Int,
    onPlayTrack: (Int) -> Unit,
    onRemoveTrack: ((index: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val tick = rememberTapTick()
    val tracks = remember(album.id) { mutableStateListOf<Track>().apply { addAll(album.tracks) } }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 96.dp.toPx() }
    val draggedIndex = remember(album.id) { mutableStateOf(-1) }

    fun moveItem(from: Int, by: Int) {
        if (tracks.isEmpty()) return
        val target = (from + by).coerceIn(0, tracks.lastIndex)
        if (target != from) {
            val item = tracks.removeAt(from)
            tracks.add(target, item)
        }
    }

    fun removeAt(index: Int) {
        if (index !in tracks.indices) return
        tracks.removeAt(index)
        onRemoveTrack?.invoke(index)
    }

    Column(modifier.fillMaxWidth()) {
        Text(
            "Up Next",
            style = type.title,
            color = colors.ivory,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${album.title}  •  ${album.artist}",
            style = type.caption,
            color = colors.ivoryDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(max = 380.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                val visible = listState.layoutInfo.visibleItemsInfo
                                val base = visible.firstOrNull()?.offset ?: 0
                                val index = ((base + it.y) / rowHeightPx).toInt()
                                if (index in 0 until tracks.size) {
                                    draggedIndex.value = index
                                }
                            },
                            onDragCancel = { draggedIndex.value = -1 },
                            onDragEnd = { draggedIndex.value = -1 },
                            onDrag = { _, delta ->
                                val from = draggedIndex.value
                                if (from >= 0) {
                                    if (delta.y > 0) moveItem(from, 1)
                                    if (delta.y < 0) moveItem(from, -1)
                                }
                            },
                        )
                    },
            ) {
                itemsIndexed(tracks) { index, track ->
                    QueueSheetRow(
                        track = track,
                        index = index,
                        current = index == currentIndex,
                        isDragged = index == draggedIndex.value,
                        onPlayTrack = { tick(); onPlayTrack(index) },
                        onSwipeRemove = { removeAt(index) },
                    )
                }
                if (tracks.isEmpty()) {
                    item {
                        Text(
                            "Queue is empty.",
                            style = type.body,
                            color = colors.ivoryFaint,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QueueSheetRow(
    track: Track,
    index: Int,
    current: Boolean,
    isDragged: Boolean,
    onPlayTrack: () -> Unit,
    onSwipeRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    var offsetX by remember(track.number, index) { mutableStateOf(0f) }
    val density = LocalDensity.current
    val widthPx = with(density) { 240.dp.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.toInt(), 0) }
            .drawBehind {
                if (offsetX < -widthPx * 0.35f) {
                    drawRect(
                        Color.Transparent.copy(alpha = 0.05f),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width + if (offsetX < 0f) -offsetX else 0f, size.height),
                    )
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offsetX = 0f },
                    onDragEnd = {
                        if (offsetX < -widthPx * 0.6f) {
                            onSwipeRemove()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        offsetX += dragAmount
                        if (offsetX > 0f) offsetX = 0f
                        change.consume()
                    },
                )
            },
        Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.extraSmall)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Play ${track.title}",
                    onClick = onPlayTrack,
                )
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (current) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = colors.ivory,
                    modifier = Modifier.width(28.dp),
                )
            } else {
                Text(
                    "%02d".format(track.number),
                    style = type.mono,
                    color = colors.ivoryFaint,
                    modifier = Modifier.width(28.dp),
                )
            }
            Text(
                track.title,
                style = type.body,
                color = if (current) colors.ivory else colors.ivoryDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                track.durationMs.toMinSec(),
                style = type.mono,
                color = if (current) colors.ivory else colors.ivoryFaint,
            )
        }
    }
}
