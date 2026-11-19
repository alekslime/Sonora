package com.sonora.nowplaying

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonora.data.Album

/**
 * M11 recently-played history. Most-recent-first liked-style keys;
 * a replay jumps to the front. Persisted through
 * [com.sonora.data.SonoraPrefs]; encode/decode are pure.
 */
object PlayHistory {

    const val MAX_ENTRIES = 30

    var entries by mutableStateOf<List<String>>(emptyList())
        private set

    fun replace(keys: List<String>) {
        entries = keys.take(MAX_ENTRIES)
    }

    fun record(key: String) {
        entries = (listOf(key) + entries).distinct().take(MAX_ENTRIES)
    }

    /** Resolves keys in recency order, dropping vanished songs. Pure. */
    fun resolve(albums: List<Album>): List<LikedEntry> {
        if (entries.isEmpty()) return emptyList()
        val byKey = mutableMapOf<String, LikedEntry>()
        albums.forEach { album ->
            album.tracks.indices.forEach { index ->
                byKey[LikedStore.keyFor(album, index)] = LikedEntry(album, index)
            }
        }
        return entries.mapNotNull { byKey[it] }
    }

    fun encode(keys: List<String>): String = keys.joinToString("\n")

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_ENTRIES)
    }
}
