package com.sonora.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content

/**
 * Overlaid lyrics view on the Now Playing screen: a centered, scrollable
 * column of lines with the playing line tinted. Taps anywhere dismiss.
 */
@Composable
fun LyricsView(
    album: Album,
    trackIndex: Int,
    positionMs: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val track = album.tracks.getOrNull(trackIndex) ?: return
    val lines = remember(album.id, trackIndex) { LyricsStore.linesFor(album, trackIndex) }
    if (lines == null || lines.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp).then(modifier)) {
            Text(
                "No lyrics for \"${track.title}\".",
                style = type.body,
                color = colors.ivoryDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }
    val current = remember(positionMs) {
        lines.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem(current)
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 64.dp)
                .clickable(onClick = onDismiss),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(track.title, style = type.title, color = colors.ivory)
            Spacer(Modifier.height(2.dp))
            Text(album.artist, style = type.caption, color = colors.ivoryDim)
            Spacer(Modifier.height(24.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lines) { index, line ->
                    val isCurrent = index == current
                    Text(
                        line.text,
                        style = if (isCurrent) type.title else type.body,
                        color = if (isCurrent) colors.amber else colors.ivoryFaint,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}
