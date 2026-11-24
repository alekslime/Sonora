package com.sonora.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.components.CoverArt
import com.sonora.components.NowPlayingIndicator
import com.sonora.components.RowEnter
import com.sonora.components.SwipeLeftActions
import com.sonora.components.SwipeLike
import com.sonora.components.rememberTapTick
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.nowplaying.LikedEntry
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M12 shared song row for the tracks, artist and playlist lists:
 * cover, title, artist and duration, with an optional trailing slot.
 * M15: staggered entrance plus optional swipe-to-like.
 */
@Composable
fun SongRow(
    entry: LikedEntry,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    enterIndex: Int = 0,
    liked: Boolean = false,
    onSwipeLike: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    nowPlaying: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(entry.album)
    val track = entry.album.tracks[entry.trackIndex]
    val tick = rememberTapTick()
    RowEnter(enterIndex, modifier) {
        val row: @Composable () -> Unit = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(SonoraShapes.small)
                    .clickable(role = Role.Button, onClickLabel = "Play ${track.title}", onClick = { tick(); onPlay() })
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (nowPlaying) {
                    NowPlayingIndicator(color = colors.amber, modifier = Modifier.padding(start = 8.dp))
                }
                CoverArt(
                    art = art,
                    fallbackLetter = entry.album.title.firstOrNull()?.uppercase() ?: "",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(SonoraShapes.extraSmall),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = type.body,
                        color = if (nowPlaying) colors.amber else colors.ivory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${entry.album.artist}  •  ${track.durationMs.toMinSec()}",
                        style = type.caption,
                        color = colors.ivoryDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                trailing()
            }
        }
        if (onSwipeLeft != null) {
            SwipeLeftActions(
                icon = Icons.Filled.QueueMusic,
                color = colors.amber,
                content = { row() },
                onAction = { tick(); onSwipeLeft() },
            )
        } else if (onSwipeLike != null) {
            SwipeLike(liked = liked, onToggle = onSwipeLike) { row() }
        } else {
            row()
        }
    }
}
