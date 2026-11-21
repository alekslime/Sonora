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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.components.CoverArt
import com.sonora.components.FastScroller
import com.sonora.components.RowEnter
import com.sonora.data.Album
import com.sonora.data.LibraryLists
import com.sonora.data.rememberAlbumArtwork
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.nowplaying.PlaylistsStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content
import com.sonora.theme.contentInverse
import com.sonora.theme.field
import com.sonora.theme.wash
import com.sonora.theme.SonoraShapes

private enum class SortMode(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    YEAR("Year"),
}

private enum class LibraryPane(val label: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
}

/**
 * Library tab: Winamp-style Tracks / Albums / Artists / Playlists
 * panes over the collection, with search and playlist creation.
 * Song taps play inside the visible list as the queue. Pull down
 * anywhere to rescan the on-device library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    albums: List<Album>,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
    demo: Boolean = false,
    onExitDemo: (() -> Unit)? = null,
    onPlaySong: (entry: LikedEntry, context: List<LikedEntry>) -> Unit = { _, _ -> },
    onOpenArtist: (String) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortMode.TITLE) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var pane by rememberSaveable { mutableStateOf(LibraryPane.TRACKS) }
    var naming by rememberSaveable { mutableStateOf(false) }

    val visibleAlbums = albums
        .filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }
        .let { list ->
            when (sort) {
                SortMode.TITLE -> list.sortedBy { it.title }
                SortMode.ARTIST -> list.sortedBy { it.artist }
                SortMode.YEAR -> list.sortedByDescending { it.year }
            }
        }
    val visibleSongs = LibraryLists.allSongs(albums).filter {
        query.isBlank() ||
            it.album.tracks[it.trackIndex].title.contains(query, ignoreCase = true) ||
            it.album.artist.contains(query, ignoreCase = true) ||
            it.album.title.contains(query, ignoreCase = true)
    }
    val visibleArtists = LibraryLists.artists(albums).filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    val playlists = PlaylistsStore.lists.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    val tracksState = rememberLazyListState()
    val albumsState = rememberLazyListState()
    val artistsState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        ) {
            Text("Library", style = type.display, color = colors.ivory)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (demo) "Demo  •  ${albums.size} records" else "${albums.size} records  •  on this device",
                    style = type.caption,
                    color = colors.ivoryDim,
                )
                if (demo && onExitDemo != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Use my music".uppercase(),
                        style = type.labelCaps,
                        color = colors.ivory,
                        modifier = Modifier
                            .clip(SonoraShapes.small)
                            .clickable(role = Role.Button, onClickLabel = "Use my music library", onClick = onExitDemo)
                            .background(colors.content.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryPane.entries.forEach { entry ->
                FilterChip(
                    selected = pane == entry,
                    onClick = { pane = entry },
                    label = { Text(entry.label, style = type.caption) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.content.copy(alpha = 0.06f),
                        labelColor = colors.ivoryDim,
                        selectedContainerColor = colors.content,
                        selectedLabelColor = colors.contentInverse,
                    ),
                )
            }
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    when (pane) {
                        LibraryPane.TRACKS -> "Search songs"
                        LibraryPane.ALBUMS -> "Search title or artist"
                        LibraryPane.ARTISTS -> "Search artists"
                        LibraryPane.PLAYLISTS -> "Search playlists"
                    },
                    style = type.body,
                    color = colors.ivoryFaint,
                )
            },
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

        when (pane) {
            LibraryPane.TRACKS -> {
                if (visibleSongs.isEmpty()) {
                    EmptyPane("No songs match.")
                } else {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = tracksState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            itemsIndexed(
                                visibleSongs,
                                key = { _, entry -> "${entry.album.id}#${entry.trackIndex}" },
                            ) { index, entry ->
                                val key = remember(entry) {
                                    LikedStore.keyFor(entry.album, entry.trackIndex)
                                }
                                SongRow(
                                    entry = entry,
                                    onPlay = { onPlaySong(entry, visibleSongs) },
                                    enterIndex = index,
                                    liked = LikedStore.isLiked(key),
                                    onSwipeLike = { LikedStore.toggle(key) },
                                )
                            }
                        }
                        FastScroller(tracksState, sectionFor = {
                            visibleSongs.getOrNull(it)?.let { song ->
                                song.album.tracks[song.trackIndex].title.firstOrNull()?.uppercase() ?: ""
                            } ?: ""
                        })
                    }
                }
            }
            LibraryPane.ALBUMS -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sort == mode,
                            onClick = { sort = mode },
                            label = { Text(mode.label, style = type.caption) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.content.copy(alpha = 0.06f),
                                labelColor = colors.ivoryDim,
                                selectedContainerColor = colors.content,
                                selectedLabelColor = colors.contentInverse,
                            ),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    ViewToggle(grid = grid, onToggle = { grid = it })
                }
                if (visibleAlbums.isEmpty()) {
                    EmptyPane("No records match.")
                } else if (grid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        itemsIndexed(visibleAlbums, key = { _, album -> album.id }) { index, album ->
                            GridCell(album = album, onOpen = { onOpenAlbum(album) }, enterIndex = index)
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = albumsState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(visibleAlbums, key = { _, album -> album.id }) { index, album ->
                                ListRow(album = album, onOpen = { onOpenAlbum(album) }, enterIndex = index)
                            }
                        }
                        FastScroller(albumsState, sectionFor = {
                            visibleAlbums.getOrNull(it)?.title?.firstOrNull()?.uppercase() ?: ""
                        })
                    }
                }
            }
            LibraryPane.ARTISTS -> {
                if (visibleArtists.isEmpty()) {
                    EmptyPane("No artists match.")
                } else {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = artistsState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            itemsIndexed(visibleArtists, key = { _, artist -> artist.name }) { index, artist ->
                                ArtistRow(
                                    name = artist.name,
                                    subtitle = "${artist.songCount} songs  •  " +
                                        if (artist.albums.size == 1) "1 album" else "${artist.albums.size} albums",
                                    album = artist.albums.first(),
                                    onOpen = { onOpenArtist(artist.name) },
                                    enterIndex = index,
                                )
                            }
                        }
                        FastScroller(artistsState, sectionFor = {
                            visibleArtists.getOrNull(it)?.name?.firstOrNull()?.uppercase() ?: ""
                        })
                    }
                }
            }
            LibraryPane.PLAYLISTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(SonoraShapes.small)
                                .background(colors.wash)
                                .clickable(role = Role.Button, onClickLabel = "New playlist", onClick = { naming = true })
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("New playlist", style = type.body, color = colors.ivory)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (playlists.isEmpty() && query.isBlank()) {
                        item {
                            Text(
                                "Playlists you make live here.",
                                style = type.body,
                                color = colors.ivoryDim,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(SonoraShapes.small)
                                .clickable(role = Role.Button, onClickLabel = "Open ${playlist.name}", onClick = { onOpenPlaylist(playlist.id) })
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(SonoraShapes.extraSmall)
                                    .background(colors.field),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = colors.ivoryDim, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    playlist.name,
                                    style = type.body,
                                    color = colors.ivory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (playlist.keys.size == 1) "1 song" else "${playlist.keys.size} songs",
                                    style = type.caption,
                                    color = colors.ivoryDim,
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.ivoryFaint)
                        }
                    }
                }
            }
        }
        }
    }
    if (naming) {
        NewPlaylistDialog(
            onConfirm = { PlaylistsStore.create(it); naming = false },
            onDismiss = { naming = false },
        )
    }
}

@Composable
private fun EmptyPane(message: String) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = type.body, color = colors.ivoryDim)
    }
}

@Composable
private fun NewPlaylistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    var draft by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.charcoal,
        title = { Text("New playlist", style = type.title, color = colors.ivory) },
        text = {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Playlist name", style = type.body) },
                singleLine = true,
                shape = SonoraShapes.small,
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }, enabled = draft.isNotBlank()) {
                Text("Create", style = type.labelCaps, color = colors.ivory)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = type.labelCaps, color = colors.ivoryDim)
            }
        },
    )
}

@Composable
private fun ArtistRow(name: String, subtitle: String, album: Album, onOpen: () -> Unit, enterIndex: Int = 0) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    RowEnter(enterIndex) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Open $name", onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            art = art,
            fallbackLetter = name.firstOrNull()?.uppercase() ?: "",
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = type.body,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle, style = type.caption, color = colors.ivoryDim, maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.ivoryFaint)
    }
    }
}

@Composable
private fun ViewToggle(grid: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf("List" to false, "Grid" to true).forEach { (label, value) ->
            val active = grid == value
            Text(
                label.uppercase(),
                style = type.labelCaps,
                color = if (active) colors.ivory else colors.ivoryFaint,
                modifier = Modifier
                    .clip(SonoraShapes.extraSmall)
                    .clickable(role = Role.Tab) { onToggle(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ListRow(album: Album, onOpen: () -> Unit, enterIndex: Int = 0) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    RowEnter(enterIndex) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Open ${album.title}", onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            art = art,
            fallbackLetter = album.title.firstOrNull()?.uppercase() ?: "",
            modifier = Modifier
                .size(56.dp)
                .clip(SonoraShapes.extraSmall),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                album.title,
                style = type.title,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${album.artist}  •  ${album.displayYear}",
                style = type.caption,
                color = colors.ivoryDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    }
}

@Composable
private fun GridCell(album: Album, onOpen: () -> Unit, enterIndex: Int = 0) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    RowEnter(enterIndex) {
    Column(
        Modifier
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Open ${album.title}", onClick = onOpen),
    ) {
        CoverArt(
            art = art,
            fallbackLetter = album.title.firstOrNull()?.uppercase() ?: "",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SonoraShapes.small),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            album.title,
            style = type.title,
            color = colors.ivory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${album.artist}  •  ${album.displayYear}",
            style = type.caption,
            color = colors.ivoryDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    }
}
