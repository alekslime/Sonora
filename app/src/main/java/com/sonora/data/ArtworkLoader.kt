package com.sonora.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * M1 artwork pipeline, M5-backed. Real albums load their embedded cover
 * art; anything without usable art (mock albums, missing tags, revoked
 * permission mid-scroll) falls back to deterministic procedural covers.
 * Loading always runs off the UI thread; covers are shared through a small
 * LRU so scrolling never reloads or reallocates per frame.
 */
object ArtworkCache {
    private const val MAX_ENTRIES = 14

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {}

    fun get(albumId: String): ImageBitmap? = synchronized(cache) { cache.get(albumId) }

    fun getOrLoad(context: Context, album: Album, sizePx: Int): ImageBitmap {
        synchronized(cache) { cache.get(album.id)?.let { return it } }
        val art = loadEmbeddedArt(context, album, sizePx)
            ?: ArtworkGenerator.generate(album.artSeed, sizePx).asImageBitmap()
        synchronized(cache) { cache.put(album.id, art) }
        return art
    }

    fun evictAll() = synchronized(cache) { cache.evictAll() }

    /**
     * Embedded picture of the album's representative track, downscaled to
     * [sizePx] on the long edge. Null for mock albums and every failure.
     */
    private fun loadEmbeddedArt(context: Context, album: Album, sizePx: Int): ImageBitmap? {
        val artUri = album.artUri ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(artUri))
            val bytes = retriever.embeddedPicture ?: return null
            val full = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            scaleDown(full, sizePx).asImageBitmap()
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Already gone; nothing to do.
            }
        }
    }

    private fun scaleDown(bitmap: Bitmap, sizePx: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= sizePx) return bitmap
        val scale = sizePx.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}

/**
 * Remembers an album cover, loading it asynchronously on first use.
 * Returns null while loading so callers can show a seeded placeholder.
 */
@Composable
fun rememberAlbumArtwork(album: Album, sizePx: Int = 512): ImageBitmap? {
    val context = LocalContext.current
    var art by remember(album.id) { mutableStateOf(ArtworkCache.get(album.id)) }
    if (art == null) {
        LaunchedEffect(album.id) {
            art = withContext(Dispatchers.IO) {
                ArtworkCache.getOrLoad(context, album, sizePx)
            }
        }
    }
    return art
}
