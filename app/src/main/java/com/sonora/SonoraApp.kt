package com.sonora

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.sonora.browse.BrowseScreen
import com.sonora.browse.EmptyLibraryScreen
import com.sonora.browse.LibraryGateScreen
import com.sonora.data.Album
import com.sonora.data.MediaLibrary
import com.sonora.data.MockLibrary
import com.sonora.data.SonoraPrefs
import com.sonora.data.audioLibraryPermission
import com.sonora.detail.AlbumDetailScreen
import com.sonora.gesture.GestureAction
import com.sonora.gesture.GestureConfigStore
import com.sonora.gesture.GestureContext
import com.sonora.gesture.SonoraGestureLayer
import com.sonora.gesture.isAssistiveTouchActive
import com.sonora.liquid.LiquidOverlay
import com.sonora.nowplaying.LikedStore
import com.sonora.nowplaying.NowPlayingScreen
import com.sonora.nowplaying.NowPlayingState
import com.sonora.playback.PlaybackHub
import com.sonora.playback.PlaybackOrchestrator
import com.sonora.studio.AudioStudioScreen
import com.sonora.studio.AudioStudioStore
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.SonoraMotion
import com.sonora.theme.SonoraTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

private sealed interface Destination {
    data object Browse : Destination
    data class Detail(val albumId: String) : Destination
    data class NowPlaying(val albumId: String, val startTrackIndex: Int) : Destination
}

private val DestinationSaver = Saver<Destination, String>(
    save = {
        when (it) {
            Destination.Browse -> "browse"
            is Destination.Detail -> "detail:${it.albumId}"
            is Destination.NowPlaying -> "nowplaying:${it.albumId}:${it.startTrackIndex}"
        }
    },
    restore = { route ->
        when {
            route == "browse" || route == "shelf" -> Destination.Browse
            route.startsWith("detail:") -> Destination.Detail(route.removePrefix("detail:"))
            route.startsWith("nowplaying:") -> {
                val parts = route.removePrefix("nowplaying:").split(":")
                Destination.NowPlaying(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            else -> Destination.Browse
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
 * SONORA root. Minimal state-machine navigation with a back stack:
 * Browse (home) -> Detail -> Now Playing. Liquid and Audio Studio are
 * overlays above navigation so playback state survives visits.
 * The library is scanned from MediaStore once audio access is granted;
 * until then (or when empty) the mock library serves as a demo mode.
 */
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

        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                scope.launch { refreshLibrary() }
            }

        LaunchedEffect(Unit) {
            AudioStudioStore.replace(SonoraPrefs.loadStudio(appContext))
            GestureConfigStore.replace(SonoraPrefs.loadGestures(appContext))
            LikedStore.replace(SonoraPrefs.loadLiked(appContext))
            launch { snapshotFlow { AudioStudioStore.config }.collect { SonoraPrefs.saveStudio(appContext, it) } }
            launch { snapshotFlow { GestureConfigStore.config }.collect { SonoraPrefs.saveGestures(appContext, it) } }
            launch { snapshotFlow { LikedStore.liked }.collect { SonoraPrefs.saveLiked(appContext, it) } }
            refreshLibrary()
        }

        val libraryAlbums: List<Album>? = when (val lib = library) {
            is LibraryState.Ready -> lib.albums
            is LibraryState.Demo -> lib.albums
            else -> null
        }
        val isDemo = library is LibraryState.Demo

        var stack by rememberSaveable(stateSaver = StackSaver) {
            mutableStateOf<List<Destination>>(listOf(Destination.Browse))
        }
        var liquidOpen by rememberSaveable { mutableStateOf(false) }
        var studioOpen by rememberSaveable { mutableStateOf(false) }
        var session by remember { mutableStateOf<NowPlayingState?>(null) }
        val current = stack.last()
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
                    AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        (fadeIn(tween(SonoraMotion.SCREEN_MILLIS)) + scaleIn(tween(SonoraMotion.SCREEN_MILLIS), 0.985f))
                            .togetherWith(fadeOut(tween(SonoraMotion.SCREEN_MILLIS)))
                    },
                    label = "sonora-nav",
                ) { destination ->
                    when (destination) {
                        Destination.Browse -> BrowseScreen(
                            albums = libraryAlbums,
                            onOpenAlbum = { push(Destination.Detail(it.id)) },
                            demo = isDemo,
                            onExitDemo = if (isDemo) {
                                { scope.launch { refreshLibrary() } }
                            } else {
                                null
                            },
                        )
                        is Destination.Detail -> {
                            val album = libraryAlbums.firstOrNull { it.id == destination.albumId } ?: libraryAlbums.first()
                            AlbumDetailScreen(
                                album = album,
                                onBack = { pop() },
                                onPlayAlbum = { shuffle ->
                                    val start = if (shuffle) Random.nextInt(album.tracks.size) else 0
                                    if (album.isReal) {
                                        PlaybackOrchestrator.startAlbum(
                                            appContext, album, start, shuffle = shuffle,
                                        )
                                    }
                                    push(Destination.NowPlaying(album.id, start))
                                },
                                onPlayTrack = { index ->
                                    if (album.isReal) {
                                        PlaybackOrchestrator.startAlbum(appContext, album, index)
                                    }
                                    push(Destination.NowPlaying(album.id, index))
                                },
                            )
                        }
                        is Destination.NowPlaying -> {
                            val album = libraryAlbums.firstOrNull { it.id == destination.albumId } ?: libraryAlbums.first()
                            NowPlayingScreen(
                                album = album,
                                startTrackIndex = destination.startTrackIndex,
                                autoplay = true,
                                onBack = { pop() },
                                onOpenLiquid = { liquidOpen = true },
                                onOpenStudio = { studioOpen = true },
                                onSession = { session = it },
                            )
                        }
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
