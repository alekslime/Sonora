package com.sonora.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.components.CoverArt
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M11 Liked tab: every hearted song across the library, resolved
 * against the current scan. A row plays its album at that track; the
 * heart unlikes without leaving.
 */
@Composable
fun LikedScreen(
    albums: List<Album>,
    onPlayQueue: (entry: LikedEntry, context: List<LikedEntry>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val entries = LikedStore.resolve(LikedStore.liked, albums)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        ) {
            Text("Liked", style = type.display, color = colors.ivory)
            Spacer(Modifier.height(4.dp))
            Text(
                when (entries.size) {
                    0 -> "Nothing yet"
                    1 -> "1 song"
                    else -> "${entries.size} songs"
                },
                style = type.caption,
                color = colors.ivoryDim,
            )
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = colors.ivoryFaint,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Songs you heart will live here.",
                        style = type.body,
                        color = colors.ivoryDim,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(entries, key = { LikedStore.keyFor(it.album, it.trackIndex) }) { entry ->
                LikedRow(
                    entry = entry,
                    onPlay = { onPlayQueue(entry, entries) },
                    onToggleLike = {
                        LikedStore.toggle(LikedStore.keyFor(entry.album, entry.trackIndex))
                    },
                )
            }
        }
    }
}

@Composable
private fun LikedRow(entry: LikedEntry, onPlay: () -> Unit, onToggleLike: () -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(entry.album)
    val track = entry.album.tracks[entry.trackIndex]
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Play ${track.title}", onClick = onPlay)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            art = art,
            fallbackLetter = entry.album.title.firstOrNull()?.uppercase() ?: "",
            modifier = Modifier
                .size(56.dp)
                .clip(SonoraShapes.extraSmall),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = type.title,
                color = colors.ivory,
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
        Box(
            Modifier
                .size(48.dp)
                .clip(SonoraShapes.extraSmall)
                .clickable(role = Role.Button, onClickLabel = "Unlike ${track.title}", onClick = onToggleLike)
                .semantics { contentDescription = "Unlike ${track.title}" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
