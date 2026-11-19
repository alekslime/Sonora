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
        album.tracks.getOrNull(trackIndex)?.uri?.let { return it }
        return "${album.id}#$trackIndex"
    }

    /** Sorted newline-joined keys; empty set encodes to blank. */
    fun encode(keys: Set<String>): String = keys.sorted().joinToString("\n")

    /** Null, blank or whitespace-only lines decode to empty. */
    fun decode(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
