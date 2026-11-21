package com.sonora

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import com.sonora.browse.ArtistDetailScreen
import com.sonora.browse.BrowseScreen
import com.sonora.browse.EmptyLibraryScreen
import com.sonora.browse.LibraryGateScreen
import com.sonora.browse.LikedScreen
import com.sonora.browse.PlaylistDetailScreen
import com.sonora.data.Album
import com.sonora.data.LibraryLists
import com.sonora.data.MediaLibrary
import com.sonora.data.MockLibrary
import com.sonora.data.PlaybackSnapshot
import com.sonora.data.SonoraPrefs
import com.sonora.data.audioLibraryPermission
import com.sonora.detail.AlbumDetailScreen
import com.sonora.home.HomeScreen
import com.sonora.home.HomeTab
import com.sonora.home.ObsidianTabBar
import com.sonora.gesture.GestureAction
import com.sonora.gesture.GestureConfigStore
import com.sonora.gesture.GestureContext
import com.sonora.gesture.SonoraGestureLayer
import com.sonora.gesture.isAssistiveTouchActive
import com.sonora.liquid.LiquidOverlay
import com.sonora.nowplaying.LikedEntry
import com.sonora.nowplaying.LikedStore
import com.sonora.nowplaying.ListeningStats
import com.sonora.nowplaying.MiniPlayer
import com.sonora.nowplaying.NowPlayingScreen
import com.sonora.nowplaying.NowPlayingState
import com.sonora.nowplaying.PlayHistory
import com.sonora.nowplaying.PlaylistsStore
import com.sonora.nowplaying.QueueIds
import com.sonora.nowplaying.QueueSpec
import com.sonora.nowplaying.QueueSpecs
import com.sonora.nowplaying.buildQueueAlbum
import com.sonora.nowplaying.rebuildQueue
import com.sonora.nowplaying.rememberQueueArtist
import com.sonora.playback.PlaybackHub
import com.sonora.playback.PlaybackOrchestrator
import com.sonora.studio.AudioStudioScreen
import com.sonora.studio.AudioStudioStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.SonoraMotion
import com.sonora.theme.SonoraTheme
import com.sonora.theme.ThemeStore
import kotlinx.coroutines.launch
import kotlin.random.Random

private sealed interface Destination {
    data object Home : Destination
    data object Browse : Destination
    data object Liked : Destination
    data class Detail(val albumId: String) : Destination
    data class ArtistPage(val name: String) : Destination
    data class PlaylistPage(val id: String) : Destination
    data class NowPlaying(val albumId: String, val startTrackIndex: Int) : Destination
}

private val DestinationSaver = Saver<Destination, String>(
    save = {
        when (it) {
            Destination.Home -> "home"
            Destination.Browse -> "browse"
            Destination.Liked -> "liked"
            is Destination.Detail -> "detail:${it.albumId}"
            is Destination.ArtistPage -> "artist:${it.name}"
            is Destination.PlaylistPage -> "playlist:${it.id}"
            is Destination.NowPlaying -> "nowplaying:${it.albumId}:${it.startTrackIndex}"
        }
    },
    restore = { route ->
        when {
            route == "home" -> Destination.Home
            route == "browse" || route == "shelf" -> Destination.Browse
            route == "liked" -> Destination.Liked
            route.startsWith("detail:") -> Destination.Detail(route.removePrefix("detail:"))
            route.startsWith("artist:") -> Destination.ArtistPage(route.removePrefix("artist:"))
            route.startsWith("playlist:") -> Destination.PlaylistPage(route.removePrefix("playlist:"))
            route.startsWith("nowplaying:") -> {
                val parts = route.removePrefix("nowplaying:").split(":")
                Destination.NowPlaying(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            else -> Destination.Home
        }
    },
)

private val StackSaver = Saver<List<Destination>, ArrayList<String>>(
    save = { stack -> ArrayList(stack.map { with(DestinationSaver) { save(it)!! } }) },
    restore = { routes -> routes.map { DestinationSaver.restore(it)!! } },
)

/** M5 library source: real scan, permission gate, empty state, or demo. */
private sealed interface LibraryState {
    data object Loading : LibraryState
    data object NeedsPermission : LibraryState
    data object Empty : LibraryState
    data class Ready(val albums: List<Album>) : LibraryState
    data class Demo(val albums: List<Album>) : LibraryState
}

/**
 * SONORA root. Tab navigation (Home, Library, Liked) with Detail and
 * Now Playing pushed on top. A floating mini-player follows the
 * app-scoped session everywhere but Now Playing; Liquid and Audio
 * Studio are overlays above navigation so playback survives visits.
 * The library is scanned from MediaStore once audio access is granted;
 * until then (or when empty) the mock library serves as a demo mode.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SonoraApp() {
    SonoraTheme {
        val colors = LocalSonoraColors.current
        val appContext = LocalContext.current.applicationContext
        val scope = rememberCoroutineScope()
        var library by remember { mutableStateOf<LibraryState>(LibraryState.Loading) }

        suspend fun refreshLibrary() {
            if (ContextCompat.checkSelfPermission(appContext, audioLibraryPermission()) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                library = LibraryState.NeedsPermission
                return
            }
            library = LibraryState.Loading
            val albums = MediaLibrary.scanLibrary(appContext.contentResolver)
            library = if (albums.isEmpty()) LibraryState.Empty else LibraryState.Ready(albums)
        }

        var refreshing by remember { mutableStateOf(false) }

        /** Pull-to-rescan: re-scans without flashing the loading screen. */
        fun rescan() {
            scope.launch {
                if (refreshing) return@launch
                if (ContextCompat.checkSelfPermission(appContext, audioLibraryPermission()) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }
                refreshing = true
                try {
                    val albums = MediaLibrary.scanLibrary(appContext.contentResolver)
                    library = if (albums.isEmpty()) LibraryState.Empty else LibraryState.Ready(albums)
                } finally {
                    refreshing = false
                }
            }
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                scope.launch { refreshLibrary() }
            }

        var snapshot by remember { mutableStateOf<PlaybackSnapshot?>(null) }
        LaunchedEffect(Unit) {
            AudioStudioStore.replace(SonoraPrefs.loadStudio(appContext))
            GestureConfigStore.replace(SonoraPrefs.loadGestures(appContext))
            LikedStore.replace(SonoraPrefs.loadLiked(appContext))
            PlayHistory.replace(SonoraPrefs.loadHistory(appContext))
            PlaylistsStore.replace(SonoraPrefs.loadPlaylists(appContext))
            ThemeStore.replace(SonoraPrefs.loadTheme(appContext))
            SonoraPrefs.loadStats(appContext)?.let {
                ListeningStats.replace(it.plays, it.ms, it.artists, it.tracks)
            }
            snapshot = SonoraPrefs.loadSnapshot(appContext)
            launch { snapshotFlow { AudioStudioStore.config }.collect { SonoraPrefs.saveStudio(appContext, it) } }
            launch { snapshotFlow { GestureConfigStore.config }.collect { SonoraPrefs.saveGestures(appContext, it) } }
            launch { snapshotFlow { LikedStore.liked }.collect { SonoraPrefs.saveLiked(appContext, it) } }
            launch { snapshotFlow { PlayHistory.entries }.collect { SonoraPrefs.saveHistory(appContext, it) } }
            launch { snapshotFlow { PlaylistsStore.lists }.collect { SonoraPrefs.savePlaylists(appContext, it) } }
            launch { snapshotFlow { ThemeStore.mode }.collect { SonoraPrefs.saveTheme(appContext, it) } }
            launch { snapshotFlow { ListeningStats.totalPlays }.collect { SonoraPrefs.saveStats(appContext) } }
            refreshLibrary()
        }

        val libraryAlbums: List<Album>? = when (val lib = library) {
            is LibraryState.Ready -> lib.albums
            is LibraryState.Demo -> lib.albums
            else -> null
        }
        val isDemo = library is LibraryState.Demo

        var stack by rememberSaveable(stateSaver = StackSaver) {
            mutableStateOf<List<Destination>>(listOf(Destination.Home))
        }
        var liquidOpen by rememberSaveable { mutableStateOf(false) }
        var studioOpen by rememberSaveable { mutableStateOf(false) }
        var session by remember { mutableStateOf<NowPlayingState?>(null) }
        // App-scoped demo clock plus history: the mini-player and Home
        // stay live after leaving Now Playing. Real sessions are driven
        // by the service engine instead.
        LaunchedEffect(session) {
            val active = session ?: return@LaunchedEffect
            fun note() {
                val key = LikedStore.keyFor(active.album, active.trackIndex)
                PlayHistory.record(key)
                val artist = LikedStore.resolveOrdered(listOf(key), libraryAlbums ?: emptyList())
                    .firstOrNull()?.album?.artist ?: active.album.artist
                ListeningStats.record(artist, key, active.track.durationMs)
            }
            note()
            var lastIndex = active.trackIndex
            if (!active.album.isReal) launch { active.ticker() }
            snapshotFlow { active.trackIndex }.collect {
                if (it != lastIndex) {
                    lastIndex = it
                    note()
                }
            }
        }
        val current = stack.last()
        // System bar glyphs follow the theme: dark icons on paper.
        val view = LocalView.current
        LaunchedEffect(colors.isLight) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = colors.isLight
                isAppearanceLightNavigationBars = colors.isLight
            }
        }
        val gestureConfig = GestureConfigStore.config
        val assistiveTouch = remember(appContext) { isAssistiveTouchActive(appContext) }
        val gestureContext = when {
            studioOpen -> GestureContext.STUDIO
            current is Destination.NowPlaying -> GestureContext.NOW_PLAYING
            else -> GestureContext.OTHER
        }
        fun push(destination: Destination) {
            stack = stack + destination
        }
        fun pop() {
            if (stack.size > 1) stack = stack.dropLast(1)
        }
        fun selectTab(tab: HomeTab) {
            stack = listOf(
                when (tab) {
                    HomeTab.HOME -> Destination.Home
                    HomeTab.LIBRARY -> Destination.Browse
                    HomeTab.LIKED -> Destination.Liked
                },
            )
        }
        /** Only one source plays: starting real pauses demo and back. */
        fun pauseOtherSource(startingReal: Boolean) {
            if (startingReal) {
                session?.takeIf { !it.album.isReal }?.pause()
            } else {
                PlaybackOrchestrator.state?.pause()
            }
        }
        fun playQueue(
            entry: LikedEntry,
            context: List<LikedEntry>,
            spec: QueueSpec,
            shuffle: Boolean = false,
        ) {
            if (context.isEmpty()) return
            val tapped = LikedStore.keyFor(entry.album, entry.trackIndex)
            val at = context.indexOfFirst {
                LikedStore.keyFor(it.album, it.trackIndex) == tapped
            }.coerceAtLeast(0)
            val start = if (shuffle && context.size > 1) {
                var pick = at
                while (pick == at) pick = Random.nextInt(context.size)
                pick
            } else {
                at
            }
            val album = buildQueueAlbum(spec, context)
            if (album.isReal) {
                pauseOtherSource(startingReal = true)
                PlaybackOrchestrator.startAlbum(appContext, album, start, shuffle = shuffle)
            } else {
                pauseOtherSource(startingReal = false)
            }
            push(Destination.NowPlaying(album.id, start))
        }
        var resumedVirtual by remember { mutableStateOf<Album?>(null) }
        fun resumeSnapshot(album: Album, trackIndex: Int, positionMs: Long, shuffle: Boolean) {
            pauseOtherSource(startingReal = true)
            PlaybackOrchestrator.startAlbum(
                appContext, album, trackIndex,
                positionMs = positionMs, playing = true, shuffle = shuffle,
            )
            resumedVirtual = album
            push(Destination.NowPlaying(album.id, trackIndex))
        }
        fun expandPlayer() {
            val active = session ?: return
            push(Destination.NowPlaying(active.album.id, active.trackIndex))
        }
        LaunchedEffect(Unit) {
            PlaybackHub.skippedTrack.collect { title ->
                Toast.makeText(appContext, "Skipped unplayable file: $title", Toast.LENGTH_SHORT).show()
            }
        }
        BackHandler(enabled = stack.size > 1) { pop() }

        if (libraryAlbums == null) {
            when (library) {
                LibraryState.Loading -> Box(Modifier.fillMaxSize().background(colors.obsidian))
                LibraryState.NeedsPermission -> LibraryGateScreen(
                    onAllowAccess = { permissionLauncher.launch(audioLibraryPermission()) },
                    onTryDemo = { library = LibraryState.Demo(MockLibrary.albums) },
                )
                LibraryState.Empty -> EmptyLibraryScreen(
                    onRescan = { scope.launch { refreshLibrary() } },
                    onTryDemo = { library = LibraryState.Demo(MockLibrary.albums) },
                )
                else -> Unit // Ready/Demo carry albums; unreachable here.
            }
        } else {
            fun resolveAlbum(id: String): Album {
                libraryAlbums.firstOrNull { it.id == id }?.let { return it }
                session?.album?.takeIf { it.id == id }?.let { return it }
                resumedVirtual?.takeIf { it.id == id }?.let { return it }
                if (QueueIds.isQueue(id)) {
                    rebuildQueue(
                        id, libraryAlbums, LikedStore.liked,
                        PlaylistsStore.lists, PlayHistory.entries,
                    )?.let { return it }
                }
                return libraryAlbums.first()
            }
            val exitDemo: (() -> Unit)? = if (isDemo) {
                { scope.launch { refreshLibrary() } }
            } else {
                null
            }
            SonoraGestureLayer(
                enabled = gestureConfig.enabled && !assistiveTouch && !liquidOpen,
                context = gestureContext,
                onAction = { action ->
                    when (action) {
                        GestureAction.OPEN_LIQUID -> liquidOpen = true
                        GestureAction.OPEN_STUDIO -> studioOpen = true
                        GestureAction.CLOSE_STUDIO -> studioOpen = false
                        GestureAction.NONE -> Unit
                    }
                },
            ) {
                Box(Modifier.fillMaxSize().background(colors.obsidian)) {
                    SharedTransitionLayout(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            (fadeIn(tween(SonoraMotion.SCREEN_MILLIS)) + scaleIn(tween(SonoraMotion.SCREEN_MILLIS), 0.985f))
                                .togetherWith(fadeOut(tween(SonoraMotion.SCREEN_MILLIS)))
                        },
                        label = "sonora-nav",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) { destination ->
                        when (destination) {
                            Destination.Home -> HomeScreen(
                                albums = libraryAlbums,
                                session = session,
                                snapshot = snapshot,
                                demo = isDemo,
                                onExitDemo = exitDemo,
                                onResumeSession = ::expandPlayer,
                                onResumeSnapshot = ::resumeSnapshot,
                                onPlayQueue = ::playQueue,
                                onOpenLiked = { selectTab(HomeTab.LIKED) },
                                onOpenLibrary = { selectTab(HomeTab.LIBRARY) },
                            )
                            Destination.Browse -> BrowseScreen(
                                albums = libraryAlbums,
                                onOpenAlbum = { push(Destination.Detail(it.id)) },
                                demo = isDemo,
                                onExitDemo = exitDemo,
                                isRefreshing = refreshing,
                                onRefresh = ::rescan,
                                onPlaySong = { entry, context ->
                                    playQueue(entry, context, QueueSpecs.all(context.size))
                                },
                                onOpenArtist = { push(Destination.ArtistPage(it)) },
                                onOpenPlaylist = { push(Destination.PlaylistPage(it)) },
                            )
                            Destination.Liked -> {
                                LikedScreen(
                                    albums = libraryAlbums,
                                    onPlayQueue = { entry, context ->
                                        playQueue(entry, context, QueueSpecs.liked(context.size))
                                    },
                                )
                            }
                            is Destination.ArtistPage -> {
                                val entry = LibraryLists.artists(libraryAlbums)
                                    .firstOrNull { it.name == destination.name }
                                if (entry == null) {
                                    LaunchedEffect(destination.name) { pop() }
                                } else {
                                    val spec = QueueSpecs.artist(entry.name, entry.songCount)
                                    ArtistDetailScreen(
                                        entry = entry,
                                        onBack = ::pop,
                                        onPlayAll = { playQueue(it.first(), it, spec) },
                                        onShuffle = { playQueue(it.first(), it, spec, shuffle = true) },
                                        onPlaySong = { song, context -> playQueue(song, context, spec) },
                                    )
                                }
                            }
                            is Destination.PlaylistPage -> {
                                val playlist = PlaylistsStore.lists.firstOrNull { it.id == destination.id }
                                if (playlist == null) {
                                    LaunchedEffect(destination.id) { pop() }
                                } else {
                                    val songs = LikedStore.resolveOrdered(playlist.keys, libraryAlbums)
                                    val spec = QueueSpecs.playlist(playlist.id, playlist.name, songs.size)
                                    PlaylistDetailScreen(
                                        playlist = playlist,
                                        albums = libraryAlbums,
                                        onBack = ::pop,
                                        onPlayAll = { playQueue(it.first(), it, spec) },
                                        onShuffle = { playQueue(it.first(), it, spec, shuffle = true) },
                                        onPlaySong = { song, context -> playQueue(song, context, spec) },
                                        onRemove = { PlaylistsStore.removeTrack(playlist.id, it) },
                                        onDelete = {
                                            PlaylistsStore.delete(playlist.id)
                                            pop()
                                        },
                                    )
                                }
                            }
                            is Destination.Detail -> {
                                val album = resolveAlbum(destination.albumId)
                                AlbumDetailScreen(
                                    album = album,
                                    onBack = { pop() },
                                    onPlayAlbum = { shuffle ->
                                        val start = if (shuffle) Random.nextInt(album.tracks.size) else 0
                                        if (album.isReal) {
                                            pauseOtherSource(startingReal = true)
                                            PlaybackOrchestrator.startAlbum(
                                                appContext, album, start, shuffle = shuffle,
                                            )
                                        } else {
                                            pauseOtherSource(startingReal = false)
                                        }
                                        push(Destination.NowPlaying(album.id, start))
                                    },
                                    onPlayTrack = { index ->
                                        if (album.isReal) {
                                            pauseOtherSource(startingReal = true)
                                            PlaybackOrchestrator.startAlbum(appContext, album, index)
                                        } else {
                                            pauseOtherSource(startingReal = false)
                                        }
                                        push(Destination.NowPlaying(album.id, index))
                                    },
                                )
                            }
                            is Destination.NowPlaying -> {
                                val album = resolveAlbum(destination.albumId)
                                NowPlayingScreen(
                                    album = album,
                                    startTrackIndex = destination.startTrackIndex,
                                    autoplay = true,
                                    onBack = { pop() },
                                    onOpenLiquid = { liquidOpen = true },
                                    onOpenStudio = { studioOpen = true },
                                    onSession = { session = it },
                                    adoptSession = session,
                                    artistFor = { index ->
                                        if (!QueueIds.isQueue(album.id)) {
                                            album.artist
                                        } else {
                                            val key = LikedStore.keyFor(album, index)
                                            LikedStore.resolveOrdered(listOf(key), libraryAlbums)
                                                .firstOrNull()?.album?.artist ?: album.artist
                                        }
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                )
                            }
                        }
                    }
                        val miniSession = session
                        AnimatedVisibility(
                            visible = miniSession != null && current !is Destination.NowPlaying,
                            enter = slideInVertically(
                                animationSpec = tween(SonoraMotion.SCREEN_MILLIS),
                                initialOffsetY = { it / 2 },
                            ) + fadeIn(tween(SonoraMotion.SCREEN_MILLIS)),
                            exit = slideOutVertically(
                                animationSpec = tween(SonoraMotion.SCREEN_MILLIS),
                            ) + fadeOut(tween(SonoraMotion.SCREEN_MILLIS)),
                        ) {
                            if (miniSession != null) {
                                MiniPlayer(
                                    session = miniSession,
                                    artistLine = rememberQueueArtist(
                                        miniSession.album, miniSession.trackIndex, libraryAlbums,
                                    ),
                                    onExpand = ::expandPlayer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                )
                            }
                        }
                        when (current) {
                            Destination.Home -> ObsidianTabBar(HomeTab.HOME, ::selectTab)
                            Destination.Browse -> ObsidianTabBar(HomeTab.LIBRARY, ::selectTab)
                            Destination.Liked -> ObsidianTabBar(HomeTab.LIKED, ::selectTab)
                            else -> Unit
                        }
                        Spacer(Modifier.navigationBarsPadding())
                    }
                    }
                    if (studioOpen) {
                        val format by PlaybackHub.format.collectAsState()
                        AudioStudioScreen(
                            onClose = { studioOpen = false },
                            sourceLabel = if (isDemo) "Demo library" else "On-device library",
                            formatLine = format?.displayLine,
                        )
                    }
                    if (liquidOpen) {
                        LiquidOverlay(
                            session = session,
                            motionEnabled = !gestureConfig.reducedMotion,
                            onExit = { liquidOpen = false },
                        )
                    }
                }
            }
        }
    }
}
