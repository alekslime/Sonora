package com.sonora.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.data.toMinSec
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M8 Up Next sheet: the album's tracks with the playing one marked.
 * A tap jumps straight there (queue jump), real or demo.
 */
@Composable
fun QueueSheet(
    album: Album,
    currentIndex: Int,
    onPlayTrack: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
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
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp),
        ) {
            itemsIndexed(album.tracks, key = { index, _ -> index }) { index, track ->
                val current = index == currentIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(SonoraShapes.extraSmall)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Play ${track.title}",
                            onClick = { onPlayTrack(index) },
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
        Spacer(Modifier.height(24.dp))
    }
}
