package com.sonora.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.data.Album
import com.sonora.data.PlaybackSnapshot
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.nowplaying.ListeningStats
import com.sonora.nowplaying.NowPlayingState
import com.sonora.nowplaying.PlayHistory
import com.sonora.nowplaying.QueueSpec
import com.sonora.nowplaying.QueueSpecs
import com.sonora.nowplaying.rememberQueueArtist
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    LIBRARY("Library", Icons.Filled.LibraryMusic),
    LIKED("Liked", Icons.Filled.Favorite),
}

/**
 * M11 Home: a Winamp-laid-out front page in the Obsidian voice —
 * continue listening, liked rail, recently played and the library
 * door. Sections with nothing to show stay hidden.
 */
@Composable
fun HomeScreen(
    albums: List<Album>,
    session: NowPlayingState?,
    snapshot: PlaybackSnapshot?,
    demo: Boolean,
    onExitDemo: (() -> Unit)?,
    onResumeSession: () -> Unit,
    onResumeSnapshot: (album: Album, trackIndex: Int, positionMs: Long, shuffle: Boolean) -> Unit,
    onPlayQueue: (entry: LikedEntry, context: List<LikedEntry>, spec: QueueSpec) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val liked = LikedStore.resolve(LikedStore.liked, albums)
    val recentAll = PlayHistory.resolve(albums)
    val recent = recentAll.take(8)
    val stats = ListeningStats.summary()
    val snapshotAlbum = rememberSnapshotAlbum(snapshot, albums)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
            ) {
                Text("Home", style = type.display, color = colors.ivory)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (demo) "Demo  •  ${albums.size} records"
                        else "${albums.size} records  •  on this device",
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
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (session != null) {
            item {
                ContinueCard(
                    album = session.album,
                    trackTitle = session.track.title,
                    artistLine = rememberQueueArtist(session.album, session.trackIndex, albums),
                    fraction = session.fraction,
                    onResume = onResumeSession,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(20.dp))
            }
        } else if (snapshot != null && snapshotAlbum != null) {
            item {
                val total = snapshotAlbum.tracks.sumOf { it.durationMs }.coerceAtLeast(1L)
                ContinueCard(
                    album = snapshotAlbum,
                    trackTitle = snapshot.trackTitles.getOrElse(snapshot.trackIndex) { snapshotAlbum.title },
                    fraction = snapshot.positionMs.toFloat() / total,
                    onResume = {
                        onResumeSnapshot(
                            snapshotAlbum, snapshot.trackIndex,
                            snapshot.positionMs, snapshot.shuffle,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(20.dp))
            }
        }
        if (liked.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Liked",
                    action = "${liked.size}",
                    onOpen = onOpenLiked,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(liked.take(12), key = { LikedStore.keyFor(it.album, it.trackIndex) }) { entry ->
                        LikedCard(
                            entry = entry,
                            onPlay = { onPlayQueue(entry, liked, QueueSpecs.liked(liked.size)) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        if (recent.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently played",
                    action = null,
                    onOpen = null,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(6.dp))
            }
            items(recent, key = { LikedStore.keyFor(it.album, it.trackIndex) }) { entry ->
                HistoryRow(
                    entry = entry,
                    onPlay = { onPlayQueue(entry, recentAll, QueueSpecs.recent(recentAll.size)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
        if (stats.totalPlays > 0) {
            item {
                StatsCard(
                    albums = albums,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(20.dp))
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(SonoraShapes.medium)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(role = Role.Button, onClickLabel = "Open library", onClick = onOpenLibrary)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("All albums", style = type.title, color = colors.ivory)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${albums.size} records",
                        style = type.caption,
                        color = colors.ivoryDim,
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.ivoryDim)
            }
        }
    }
}

@Composable
private fun rememberSnapshotAlbum(snapshot: PlaybackSnapshot?, albums: List<Album>): Album? {
    if (snapshot == null) return null
    albums.firstOrNull { it.id == snapshot.albumId }?.let { return it }
    val rebuilt = snapshot.toAlbum() ?: return null
    if (snapshot.trackIndex !in rebuilt.tracks.indices) return null
    return rebuilt
}

@Composable
private fun ContinueCard(
    album: Album,
    trackTitle: String,
    fraction: Float,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    artistLine: String = album.artist,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    Column(
        modifier
            .fillMaxWidth()
            .clip(SonoraShapes.medium)
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(role = Role.Button, onClickLabel = "Continue listening", onClick = onResume)
            .padding(16.dp),
    ) {
        Text("Continue listening".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(SonoraShapes.small)
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
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    trackTitle,
                    style = type.title,
                    color = colors.ivory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    artistLine,
                    style = type.caption,
                    color = colors.ivoryDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(albums: List<Album>, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val stats = ListeningStats.summary()
    val topTrack = stats.topTrackKey?.let { key ->
        LikedStore.resolveOrdered(listOf(key), albums).firstOrNull()
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(SonoraShapes.medium)
            .background(Color.White.copy(alpha = 0.07f))
            .padding(16.dp),
    ) {
        Text("Your stats".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
        Spacer(Modifier.height(8.dp))
        Text(
            ListeningStats.formatDuration(stats.totalMs),
            style = type.display,
            color = colors.ivory,
        )
        Text(
            if (stats.totalPlays == 1) "1 play" else "${stats.totalPlays} plays",
            style = type.caption,
            color = colors.ivoryDim,
        )
        Spacer(Modifier.height(10.dp))
        stats.topArtists.forEachIndexed { i, artist ->
            Text(
                "${i + 1}. ${artist.name}  •  ${artist.plays}",
                style = type.body,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (topTrack != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Top track: ${topTrack.album.tracks[topTrack.trackIndex].title}",
                style = type.caption,
                color = colors.ivoryDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onOpen: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onOpen != null) {
                    Modifier
                        .clip(SonoraShapes.extraSmall)
                        .clickable(role = Role.Button, onClickLabel = "Open $title", onClick = onOpen)
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = type.title, color = colors.ivory, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, style = type.caption, color = colors.ivoryDim)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.ivoryDim, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LikedCard(entry: LikedEntry, onPlay: () -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(entry.album)
    val track = entry.album.tracks[entry.trackIndex]
    Column(
        Modifier
            .width(112.dp)
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Play ${track.title}", onClick = onPlay),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SonoraShapes.small)
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
        Spacer(Modifier.height(6.dp))
        Text(
            track.title,
            style = type.caption,
            color = colors.ivory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryRow(entry: LikedEntry, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(entry.album)
    val track = entry.album.tracks[entry.trackIndex]
    Row(
        modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Play ${track.title}", onClick = onPlay)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(SonoraShapes.extraSmall)
                .background(colors.graphite),
        ) {
            if (art != null) {
                Image(art, contentDescription = null, contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = type.body,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.album.artist,
                style = type.caption,
                color = colors.ivoryDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(track.durationMs.toMinSec(), style = type.mono, color = colors.ivoryFaint)
    }
}

@Composable
fun ObsidianTabBar(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HomeTab.entries.forEach { tab ->
                val active = tab == selected
                Column(
                    Modifier
                        .clip(SonoraShapes.extraSmall)
                        .clickable(
                            role = Role.Tab,
                            onClickLabel = "Open ${tab.label}",
                            onClick = { onSelect(tab) },
                        )
                        .semantics { contentDescription = "${tab.label} tab" }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = if (active) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label.uppercase(),
                        style = type.labelCaps.copy(letterSpacing = 1.2.sp),
                        color = if (active) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.Transparent),
                    )
                }
            }
        }
    }
}
