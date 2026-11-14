package com.sonora.data

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * M1 artwork pipeline. Generation (standing in for metadata/artwork IO in
 * M5) always runs off the UI thread; decoded covers are shared through a
 * small LRU so scrolling never regenerates or reallocates per frame.
 */
object ArtworkCache {
    private const val MAX_ENTRIES = 14

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {}

    fun get(albumId: String): ImageBitmap? = synchronized(cache) { cache.get(albumId) }

    fun getOrGenerate(album: Album, sizePx: Int): ImageBitmap {
        synchronized(cache) { cache.get(album.id)?.let { return it } }
        val art = ArtworkGenerator.generate(album.artSeed, sizePx).asImageBitmap()
        synchronized(cache) { cache.put(album.id, art) }
        return art
    }

    fun evictAll() = synchronized(cache) { cache.evictAll() }
}

/**
 * Remembers an album cover, generating it asynchronously on first use.
 * Returns null while loading so callers can show a seeded placeholder.
 */
@Composable
fun rememberAlbumArtwork(album: Album, sizePx: Int = 512): ImageBitmap? {
    var art by remember(album.id) { mutableStateOf(ArtworkCache.get(album.id)) }
    if (art == null) {
        LaunchedEffect(album.id) {
            art = withContext(Dispatchers.Default) {
                ArtworkCache.getOrGenerate(album, sizePx)
            }
        }
    }
    return art
}
