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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.components.CoverArt
import com.sonora.components.FastScroller
import com.sonora.components.NowPlayingIndicator
import com.sonora.components.RowEnter
import com.sonora.components.SwipeLike
import com.sonora.components.rememberTapTick
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.nowplaying.NowPlayingState
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content
import com.sonora.theme.contentInverse
import com.sonora.theme.field
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
    session: NowPlayingState? = null,
    onOpenLibrary: (() -> Unit)? = null,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val sessionKey = session?.let { LikedStore.keyFor(it.album, it.trackIndex) }
    var query by rememberSaveable { mutableStateOf("") }
    val entries = LikedStore.resolve(LikedStore.liked, albums).filter { entry ->
        query.isBlank() ||
            entry.album.tracks[entry.trackIndex].title.contains(query, ignoreCase = true) ||
            entry.album.artist.contains(query, ignoreCase = true) ||
            entry.album.title.contains(query, ignoreCase = true)
    }
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
                    0 -> if (query.isNotBlank()) "No matches" else "Nothing yet"
                    1 -> "1 song"
                    else -> "${entries.size} songs"
                },
                style = type.caption,
                color = colors.ivoryDim,
            )
        }
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Search songs", style = type.body, color = colors.ivoryFaint) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = colors.ivoryDim) },
            singleLine = true,
            shape = SonoraShapes.large,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.field,
                unfocusedContainerColor = colors.field,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colors.ivory,
                unfocusedTextColor = colors.ivory,
                cursorColor = colors.ivory,
            ),
        )
        if (entries.isEmpty()) {
            val tick = rememberTapTick()
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
                    if (onOpenLibrary != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Browse library".uppercase(),
                            style = type.labelCaps,
                            color = colors.contentInverse,
                            modifier = Modifier
                                .clip(SonoraShapes.small)
                                .background(colors.content)
                                .clickable(role = Role.Button, onClickLabel = "Browse library", onClick = { tick(); onOpenLibrary() })
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            return
        }
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                itemsIndexed(entries, key = { _, entry -> LikedStore.keyFor(entry.album, entry.trackIndex) }) { index, entry ->
                    LikedRow(
                        entry = entry,
                        onPlay = { onPlayQueue(entry, entries) },
                        onToggleLike = {
                            LikedStore.toggle(LikedStore.keyFor(entry.album, entry.trackIndex))
                        },
                        enterIndex = index,
                        nowPlaying = LikedStore.keyFor(entry.album, entry.trackIndex) == sessionKey,
                    )
                }
            }
            FastScroller(listState, sectionFor = {
                entries.getOrNull(it)?.let { entry ->
                    entry.album.tracks[entry.trackIndex].title.firstOrNull()?.uppercase() ?: ""
                } ?: ""
            })
        }
    }
}

@Composable
private fun LikedRow(
    entry: LikedEntry,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    enterIndex: Int = 0,
    nowPlaying: Boolean = false,
) {
    val art = rememberAlbumArtwork(entry.album)
    RowEnter(enterIndex) {
        SwipeLike(liked = true, onToggle = onToggleLike) {
            LikedRowContent(
                entry = entry,
                art = art,
                onPlay = onPlay,
                onToggleLike = onToggleLike,
                nowPlaying = nowPlaying,
            )
        }
    }
}

@Composable
private fun LikedRowContent(
    entry: LikedEntry,
    art: ImageBitmap?,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    nowPlaying: Boolean = false,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val track = entry.album.tracks[entry.trackIndex]
    val tick = rememberTapTick()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Play ${track.title}", onClick = { tick(); onPlay() })
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nowPlaying) {
            NowPlayingIndicator(color = colors.amber, modifier = Modifier.padding(start = 8.dp))
        }
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
        Box(
            Modifier
                .size(48.dp)
                .clip(SonoraShapes.extraSmall)
                .clickable(role = Role.Button, onClickLabel = "Unlike ${track.title}", onClick = { tick(); onToggleLike() })
                .semantics { contentDescription = "Unlike ${track.title}" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
