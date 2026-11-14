package com.sonora

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
import com.sonora.shelf.ShelfScreen
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.SonoraMotion
import com.sonora.theme.SonoraTheme

private sealed interface Destination {
    data object Shelf : Destination
    data object Browse : Destination
    data class Detail(val albumId: String) : Destination
}

private val DestinationSaver = Saver<Destination, String>(
    save = {
        when (it) {
            Destination.Shelf -> "shelf"
            Destination.Browse -> "browse"
            is Destination.Detail -> "detail:${it.albumId}"
        }
    },
    restore = { route ->
        when {
            route == "browse" -> Destination.Browse
            route.startsWith("detail:") -> Destination.Detail(route.removePrefix("detail:"))
            else -> Destination.Shelf
        }
    },
)

/**
 * SONORA root. Minimal state-machine navigation (no nav library needed for
 * three destinations): Shelf <-> Detail, Shelf <-> Browse.
 */
@Composable
fun SonoraApp() {
    SonoraTheme {
        val colors = LocalSonoraColors.current
        val albums = MockLibrary.albums
        var destination by rememberSaveable(stateSaver = DestinationSaver) {
            mutableStateOf<Destination>(Destination.Shelf)
        }
        Box(Modifier.fillMaxSize().background(colors.obsidian)) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    (fadeIn(tween(SonoraMotion.SCREEN_MILLIS)) + scaleIn(tween(SonoraMotion.SCREEN_MILLIS), 0.985f))
                        .togetherWith(fadeOut(tween(SonoraMotion.SCREEN_MILLIS)))
                },
                label = "sonora-nav",
            ) { current ->
                when (current) {
                    Destination.Shelf -> ShelfScreen(
                        albums = albums,
                        onOpenAlbum = { destination = Destination.Detail(it.id) },
                        onBrowseAll = { destination = Destination.Browse },
                    )
                    Destination.Browse -> BrowseScreen(
                        albums = albums,
                        onOpenAlbum = { destination = Destination.Detail(it.id) },
                        onBack = { destination = Destination.Shelf },
                    )
                    is Destination.Detail -> {
                        val album = albums.firstOrNull { it.id == current.albumId } ?: albums.first()
                        AlbumDetailScreen(album = album, onBack = { destination = Destination.Shelf })
                    }
                }
            }
        }
    }
}
