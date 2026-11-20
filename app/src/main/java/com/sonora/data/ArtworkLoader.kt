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
 * Loading always runs off the UI thread; covers are shared through a
 * small LRU so scrolling never reloads or reallocates per frame.
 *
 * M14: entries are keyed by album and size, and the player backdrop
 * gets its own pre-blurred bitmaps — the heavy blur runs once per
 * album off-thread instead of on the GPU every frame.
 */
object ArtworkCache {
    private const val MAX_ENTRIES = 24
    private const val MAX_BACKDROPS = 8
    private const val BACKDROP_PX = 160
    private const val BACKDROP_BLUR_RADIUS = 5

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {}
    private val backdrops = object : LruCache<String, ImageBitmap>(MAX_BACKDROPS) {}

    fun get(albumId: String, sizePx: Int): ImageBitmap? =
        synchronized(cache) { cache.get(key(albumId, sizePx)) }

    fun getOrLoad(context: Context, album: Album, sizePx: Int): ImageBitmap {
        val key = key(album.id, sizePx)
        synchronized(cache) { cache.get(key)?.let { return it } }
        val art = loadEmbeddedArt(context, album, sizePx)
            ?: ArtworkGenerator.generate(album.artSeed, sizePx).asImageBitmap()
        synchronized(cache) { cache.put(key, art) }
        return art
    }

    /**
     * Small pre-blurred cover for full-bleed backdrops. The blur runs
     * once per album off-thread and is cached, keeping the GPU blur
     * out of the frame path on every API level.
     */
    fun getBackdrop(context: Context, album: Album): ImageBitmap {
        synchronized(backdrops) { backdrops.get(album.id)?.let { return it } }
        val bitmap = loadEmbeddedBitmap(context, album, BACKDROP_PX)
            ?: ArtworkGenerator.generate(album.artSeed, BACKDROP_PX)
        val art = blurBitmap(bitmap, BACKDROP_BLUR_RADIUS).asImageBitmap()
        synchronized(backdrops) { backdrops.put(album.id, art) }
        return art
    }

    fun evictAll() = synchronized(cache) {
        cache.evictAll()
        synchronized(backdrops) { backdrops.evictAll() }
    }

    private fun key(albumId: String, sizePx: Int) = "$albumId@$sizePx"

    /**
     * Embedded picture of the album's representative track, downscaled to
     * [sizePx] on the long edge. Null for mock albums and every failure.
     */
    private fun loadEmbeddedArt(context: Context, album: Album, sizePx: Int): ImageBitmap? {
        return loadEmbeddedBitmap(context, album, sizePx)?.asImageBitmap()
    }

    private fun loadEmbeddedBitmap(context: Context, album: Album, sizePx: Int): Bitmap? {
        val bytes = embeddedBytes(context, album) ?: return null
        val full = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return scaleDown(full, sizePx)
    }

    private fun embeddedBytes(context: Context, album: Album): ByteArray? {
        val artUri = album.artUri ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(artUri))
            retriever.embeddedPicture
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

    /**
     * Two-pass separable box blur (a cheap gaussian stand-in) over ARGB
     * pixels with clamped edges. Fast enough at backdrop size to run
     * once per album on a worker thread.
     */
    private fun blurBitmap(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        var input = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        var output = IntArray(w * h)
        repeat(2) {
            blurPass(input, output, w, h, radius, horizontal = true)
            blurPass(output, input, w, h, radius, horizontal = false)
        }
        // Even passes land back in `input`.
        return Bitmap.createBitmap(input, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun blurPass(
        src: IntArray,
        dst: IntArray,
        w: Int,
        h: Int,
        radius: Int,
        horizontal: Boolean,
    ) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var n = 0
                for (d in -radius..radius) {
                    val xx = if (horizontal) (x + d).coerceIn(0, w - 1) else x
                    val yy = if (horizontal) y else (y + d).coerceIn(0, h - 1)
                    val color = src[yy * w + xx]
                    a += color ushr 24
                    r += (color shr 16) and 0xFF
                    g += (color shr 8) and 0xFF
                    b += color and 0xFF
                    n++
                }
                dst[y * w + x] = (a / n shl 24) or (r / n shl 16) or (g / n shl 8) or (b / n)
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
    var art by remember(album.id, sizePx) { mutableStateOf(ArtworkCache.get(album.id, sizePx)) }
    if (art == null) {
        LaunchedEffect(album.id, sizePx) {
            art = withContext(Dispatchers.IO) {
                ArtworkCache.getOrLoad(context, album, sizePx)
            }
        }
    }
    return art
}

/**
 * Remembers the pre-blurred backdrop bitmap, loading it off-thread on
 * first use. Null while loading; callers keep their gradient wash.
 */
@Composable
fun rememberBackdropArtwork(album: Album): ImageBitmap? {
    val context = LocalContext.current
    var art by remember(album.id) { mutableStateOf<ImageBitmap?>(null) }
    if (art == null) {
        LaunchedEffect(album.id) {
            art = withContext(Dispatchers.IO) {
                ArtworkCache.getBackdrop(context, album)
            }
        }
    }
    return art
}
