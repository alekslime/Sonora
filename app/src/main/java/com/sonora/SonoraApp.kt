package com.sonora

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sonora.browse.BrowseScreen
import com.sonora.data.MockLibrary
import com.sonora.detail.AlbumDetailScreen
import com.sonora.nowplaying.NowPlayingScreen
import com.sonora.shelf.ShelfScreen
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.SonoraMotion
import com.sonora.theme.SonoraTheme
import kotlin.random.Random

private sealed interface Destination {
    data object Shelf : Destination
    data object Browse : Destination
    data class Detail(val albumId: String) : Destination
    data class NowPlaying(val albumId: String, val startTrackIndex: Int) : Destination
}

private val DestinationSaver = Saver<Destination, String>(
    save = {
        when (it) {
            Destination.Shelf -> "shelf"
            Destination.Browse -> "browse"
            is Destination.Detail -> "detail:${it.albumId}"
            is Destination.NowPlaying -> "nowplaying:${it.albumId}:${it.startTrackIndex}"
        }
    },
    restore = { route ->
        when {
            route == "browse" -> Destination.Browse
            route.startsWith("detail:") -> Destination.Detail(route.removePrefix("detail:"))
            route.startsWith("nowplaying:") -> {
                val parts = route.removePrefix("nowplaying:").split(":")
                Destination.NowPlaying(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            else -> Destination.Shelf
        }
    },
)

private val StackSaver = Saver<List<Destination>, ArrayList<String>>(
    save = { stack -> ArrayList(stack.map { with(DestinationSaver) { save(it)!! } }) },
    restore = { routes -> routes.map { DestinationSaver.restore(it)!! } },
)

/**
 * SONORA root. Minimal state-machine navigation with a back stack:
 * Shelf <-> Detail -> Now Playing, Shelf <-> Browse.
 */
@Composable
fun SonoraApp() {
    SonoraTheme {
        val colors = LocalSonoraColors.current
        val albums = MockLibrary.albums
        var stack by rememberSaveable(stateSaver = StackSaver) {
            mutableStateOf<List<Destination>>(listOf(Destination.Shelf))
        }
        val current = stack.last()
        fun push(destination: Destination) {
            stack = stack + destination
        }
        fun pop() {
            if (stack.size > 1) stack = stack.dropLast(1)
        }
        BackHandler(enabled = stack.size > 1) { pop() }

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
                    Destination.Shelf -> ShelfScreen(
                        albums = albums,
                        onOpenAlbum = { push(Destination.Detail(it.id)) },
                        onBrowseAll = { push(Destination.Browse) },
                    )
                    Destination.Browse -> BrowseScreen(
                        albums = albums,
                        onOpenAlbum = { push(Destination.Detail(it.id)) },
                        onBack = { pop() },
                    )
                    is Destination.Detail -> {
                        val album = albums.firstOrNull { it.id == destination.albumId } ?: albums.first()
                        AlbumDetailScreen(
                            album = album,
                            onBack = { pop() },
                            onPlayAlbum = { shuffle ->
                                val start = if (shuffle) Random.nextInt(album.tracks.size) else 0
                                push(Destination.NowPlaying(album.id, start))
                            },
                            onPlayTrack = { index -> push(Destination.NowPlaying(album.id, index)) },
                        )
                    }
                    is Destination.NowPlaying -> {
                        val album = albums.firstOrNull { it.id == destination.albumId } ?: albums.first()
                        NowPlayingScreen(
                            album = album,
                            startTrackIndex = destination.startTrackIndex,
                            autoplay = true,
                            onBack = { pop() },
                        )
                    }
                }
            }
        }
    }
}
