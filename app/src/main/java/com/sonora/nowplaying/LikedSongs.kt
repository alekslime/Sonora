package com.sonora.nowplaying

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonora.data.Album

/**
 * M8 liked songs. Real tracks key by content URI (stable across scans);
 * demo tracks without a URI key by album id + index. The set persists
 * through [com.sonora.data.SonoraPrefs]; encode/decode are pure.
 */
object LikedStore {

    var liked by mutableStateOf<Set<String>>(emptySet())
        private set

    fun replace(keys: Set<String>) {
        liked = keys
    }

    fun isLiked(key: String): Boolean = key in liked

    fun toggle(key: String) {
        liked = if (key in liked) liked - key else liked + key
    }

    fun keyFor(album: Album, trackIndex: Int): String {
        album.tracks.getOrNull(trackIndex)?.let { track ->
            track.uri?.let { return it }
            track.originKey?.let { return it }
        }
        return "${album.id}#$trackIndex"
    }

    /** Sorted newline-joined keys; empty set encodes to blank. */
    fun encode(keys: Set<String>): String = keys.sorted().joinToString("\n")

    /** Null, blank or whitespace-only lines decode to empty. */
    fun decode(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Resolves liked keys against the library in album/track order,
     * silently dropping keys whose songs are gone. Pure.
     */
    fun resolve(keys: Set<String>, albums: List<Album>): List<LikedEntry> {
        if (keys.isEmpty()) return emptyList()
        return albums.flatMap { album ->
            album.tracks.indices.mapNotNull { index ->
                if (keyFor(album, index) in keys) LikedEntry(album, index) else null
            }
        }
    }

    /**
     * Resolves keys in the given order (playlists, history), dropping
     * vanished songs. Pure.
     */
    fun resolveOrdered(keys: List<String>, albums: List<Album>): List<LikedEntry> {
        if (keys.isEmpty()) return emptyList()
        val byKey = mutableMapOf<String, LikedEntry>()
        albums.forEach { album ->
            album.tracks.indices.forEach { index ->
                byKey[keyFor(album, index)] = LikedEntry(album, index)
            }
        }
        return keys.mapNotNull { byKey[it] }
    }
}

/** A liked track pinned to its album and position. */
data class LikedEntry(val album: Album, val trackIndex: Int)
