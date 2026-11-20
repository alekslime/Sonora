package com.sonora.nowplaying

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/** A user playlist: ordered liked-style track keys. */
data class Playlist(val id: String, val name: String, val keys: List<String>)

/**
 * M12 playlists. Persisted through [com.sonora.data.SonoraPrefs];
 * encode/decode are pure.
 */
object PlaylistsStore {

    var lists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    fun replace(lists: List<Playlist>) {
        this.lists = lists
    }

    /** Null for blank names; otherwise the created list. */
    fun create(name: String): Playlist? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val playlist = Playlist(UUID.randomUUID().toString(), trimmed, emptyList())
        lists = lists + playlist
        return playlist
    }

    fun delete(id: String) {
        lists = lists.filterNot { it.id == id }
    }

    fun addTrack(id: String, key: String) {
        lists = lists.map { list ->
            if (list.id == id && key !in list.keys) list.copy(keys = list.keys + key) else list
        }
    }

    fun removeTrack(id: String, key: String) {
        lists = lists.map { list ->
            if (list.id == id) list.copy(keys = list.keys - key) else list
        }
    }

    /** Toggles membership; returns true when the key ends up in. */
    fun toggleTrack(id: String, key: String): Boolean {
        var member = false
        lists = lists.map { list ->
            if (list.id != id) return@map list
            if (key in list.keys) {
                list.copy(keys = list.keys - key)
            } else {
                member = true
                list.copy(keys = list.keys + key)
            }
        }
        return member
    }

    fun contains(id: String, key: String): Boolean {
        return lists.firstOrNull { it.id == id }?.keys?.contains(key) == true
    }

    private const val FIELD = '\u001F'
    private const val ITEM = '\u001E'

    fun encode(lists: List<Playlist>): String {
        return lists.joinToString("\n") { list ->
            listOf(
                scrub(list.id),
                scrub(list.name),
                list.keys.joinToString(ITEM.toString()) { scrub(it) },
            ).joinToString(FIELD.toString())
        }
    }

    fun decode(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val fields = line.split(FIELD)
            if (fields.size != 3 || fields[0].isBlank() || fields[1].isBlank()) return@mapNotNull null
            val keys = if (fields[2].isBlank()) {
                emptyList()
            } else {
                fields[2].split(ITEM).filter { it.isNotEmpty() }
            }
            Playlist(fields[0], fields[1], keys)
        }
    }

    private fun scrub(text: String): String {
        return text.replace(FIELD, ' ').replace(ITEM, ' ').replace('\n', ' ')
    }
}
