package com.sonora.browse

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.sonora.data.ArtistEntry
import com.sonora.data.LibraryLists
import com.sonora.data.rememberAlbumArtwork
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M12 artist page: cover, counts, play-all and shuffle over the
 * artist's songs, then the song list.
 */
@Composable
fun ArtistDetailScreen(
    entry: ArtistEntry,
    onBack: () -> Unit,
    onPlayAll: (songs: List<LikedEntry>) -> Unit,
    onShuffle: (songs: List<LikedEntry>) -> Unit,
    onPlaySong: (entry: LikedEntry, context: List<LikedEntry>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val songs = LibraryLists.artistSongs(entry)
    val art = rememberAlbumArtwork(entry.albums.first())
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(112.dp)
                        .clip(SonoraShapes.medium)
                        .background(colors.graphite),
                ) {
                    if (art != null) {
                        Image(
                            art,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.name,
                        style = type.display,
                        color = colors.ivory,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${entry.songCount} songs  •  " +
                            if (entry.albums.size == 1) "1 album" else "${entry.albums.size} albums",
                        style = type.caption,
                        color = colors.ivoryDim,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(SonoraShapes.small)
                        .background(Color.White)
                        .clickable(role = Role.Button, onClickLabel = "Play all", onClick = { onPlayAll(songs) })
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
                        .clickable(role = Role.Button, onClickLabel = "Shuffle", onClick = { onShuffle(songs) })
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
        items(songs, key = { LikedStore.keyFor(it.album, it.trackIndex) }) { song ->
            SongRow(
                entry = song,
                onPlay = { onPlaySong(song, songs) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}
