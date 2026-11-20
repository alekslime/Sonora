package com.sonora.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.data.toMinSec
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.nowplaying.Playlist
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M12 playlist page: counts, play-all and shuffle over the list in
 * playlist order, per-row remove, and delete at the bottom.
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    albums: List<Album>,
    onBack: () -> Unit,
    onPlayAll: (songs: List<LikedEntry>) -> Unit,
    onShuffle: (songs: List<LikedEntry>) -> Unit,
    onPlaySong: (entry: LikedEntry, context: List<LikedEntry>) -> Unit,
    onRemove: (key: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val songs = LikedStore.resolveOrdered(playlist.keys, albums)
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.ivory)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    playlist.name,
                    style = type.display,
                    color = colors.ivory,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    (if (songs.size == 1) "1 song" else "${songs.size} songs") +
                        "  •  ${songs.sumOf { it.album.tracks[it.trackIndex].durationMs }.toMinSec()}",
                    style = type.caption,
                    color = colors.ivoryDim,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(SonoraShapes.small)
                            .background(if (songs.isEmpty()) Color.White.copy(alpha = 0.25f) else Color.White)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Play all",
                                enabled = songs.isNotEmpty(),
                                onClick = { onPlayAll(songs) },
                            )
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play", style = type.labelCaps, color = Color.Black)
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(SonoraShapes.small)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Shuffle",
                                enabled = songs.isNotEmpty(),
                                onClick = { onShuffle(songs) },
                            )
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle", style = type.labelCaps, color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (songs.isEmpty()) {
            item {
                Text(
                    "Add songs from the player: tap + on anything playing.",
                    style = type.body,
                    color = colors.ivoryDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                )
            }
        }
        items(songs, key = { LikedStore.keyFor(it.album, it.trackIndex) }) { song ->
            SongRow(
                entry = song,
                onPlay = { onPlaySong(song, songs) },
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                val key = LikedStore.keyFor(song.album, song.trackIndex)
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(SonoraShapes.extraSmall)
                        .clickable(role = Role.Button, onClickLabel = "Remove ${song.album.tracks[song.trackIndex].title}", onClick = { onRemove(key) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = colors.ivoryDim, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "Delete playlist".uppercase(),
                style = type.labelCaps,
                color = colors.coral,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(SonoraShapes.small)
                    .clickable(role = Role.Button, onClickLabel = "Delete playlist", onClick = onDelete)
                    .padding(vertical = 14.dp),
            )
        }
    }
}
