package com.sonora.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sonora.data.Album
import com.sonora.data.LibraryLists
import com.sonora.data.Track

/**
 * M12 virtual queues. Any playable list (all songs, a playlist, an
 * artist, liked) becomes a synthetic [Album], so the tested transport,
 * snapshots, queue sheet and mini-player work unchanged. Queue ids
 * carry no colons, keeping the nav saver split intact, and rebuild
 * deterministically from the library for process-death restore.
 */
object QueueIds {
    const val ALL = "queue/all"
    const val LIKED = "queue/liked"
    const val RECENT = "queue/recent"

    fun playlist(id: String) = "queue/playlist/$id"

    fun artist(name: String) = "queue/artist/${slugArtist(name)}"

    fun isQueue(id: String) = id.startsWith("queue/")
}

/** Escapes nav-unsafe chars in artist names; pure and unit-safe. */
fun slugArtist(name: String): String {
    return name
        .replace("%", "%25")
        .replace("/", "%2F")
        .replace(":", "%3A")
        .replace("#", "%23")
}

/**
 * Display album for a queue position: library albums report
 * themselves, virtual queues resolve the track's origin album so
 * artwork, backdrop and tint follow the song instead of freezing
 * on the first entry's cover.
 */
fun queueArtAlbum(album: Album, trackIndex: Int, library: List<Album>): Album {
    if (!QueueIds.isQueue(album.id)) return album
    val key = LikedStore.keyFor(album, trackIndex)
    return LikedStore.resolveOrdered(listOf(key), library).firstOrNull()?.album ?: album
}

@Composable
fun rememberQueueArtAlbum(album: Album, trackIndex: Int, library: List<Album>): Album {
    return remember(album.id, trackIndex, library) {
        queueArtAlbum(album, trackIndex, library)
    }
}

/**
 * Display artist for a queue position: library albums report
 * themselves, virtual queues resolve the canonical artist so rows
 * read "Northfield Howl" instead of "192 songs".
 */
@Composable
fun rememberQueueArtist(album: Album, trackIndex: Int, library: List<Album>): String {
    return rememberQueueArtAlbum(album, trackIndex, library).artist
}

/** Inverse of [slugArtist]; percent-decoding runs last on purpose. */
fun unslugArtist(slug: String): String {
    return slug
        .replace("%23", "#")
        .replace("%3A", ":")
        .replace("%2F", "/")
        .replace("%25", "%")
}

/** Identity block shared by queue builders and the rebuilder. */
data class QueueSpec(val id: String, val title: String, val subtitle: String)

object QueueSpecs {
    fun all(count: Int) = QueueSpec(QueueIds.ALL, "All Songs", "$count songs")

    fun liked(count: Int) =
        QueueSpec(QueueIds.LIKED, "Liked Songs", if (count == 1) "1 song" else "$count songs")

    fun recent(count: Int) =
        QueueSpec(QueueIds.RECENT, "Recently Played", if (count == 1) "1 song" else "$count songs")

    fun playlist(pid: String, name: String, count: Int) =
        QueueSpec(QueueIds.playlist(pid), name, if (count == 1) "1 song" else "$count songs")

    fun artist(name: String, count: Int) =
        QueueSpec(QueueIds.artist(name), name, if (count == 1) "1 song" else "$count songs")
}

/**
 * Builds the playable album for [entries]. Cover and seed come from
 * the first song; every track keeps its canonical key, so hearts,
 * playlists and history resolve against the library album.
 */
fun buildQueueAlbum(spec: QueueSpec, entries: List<LikedEntry>): Album {
    val first = entries.firstOrNull()
    return Album(
        id = spec.id,
        title = spec.title,
        artist = spec.subtitle,
        year = 0,
        label = "SONORA",
        tracks = entries.mapIndexed { i, entry ->
            val track = entry.album.tracks[entry.trackIndex]
            Track(
                number = i + 1,
                title = track.title,
                durationMs = track.durationMs,
                uri = track.uri,
                originKey = LikedStore.keyFor(entry.album, entry.trackIndex),
                artist = track.artist ?: entry.album.artist,
            )
        },
        artSeed = first?.album?.artSeed ?: spec.id.hashCode().toLong(),
        artUri = first?.album?.artUri,
    )
}

/**
 * Rebuilds a virtual queue from the current library (process-death
 * restore). Null when the source is gone; callers fall back.
 */
fun rebuildQueue(
    id: String,
    albums: List<Album>,
    likedKeys: Set<String>,
    playlists: List<Playlist>,
    historyKeys: List<String> = emptyList(),
): Album? {
    return when {
        id == QueueIds.ALL -> {
            val songs = LibraryLists.allSongs(albums)
            if (songs.isEmpty()) null
            else buildQueueAlbum(QueueSpecs.all(songs.size), songs)
        }
        id == QueueIds.LIKED -> {
            val songs = LikedStore.resolve(likedKeys, albums)
            if (songs.isEmpty()) null
            else buildQueueAlbum(QueueSpecs.liked(songs.size), songs)
        }
        id == QueueIds.RECENT -> {
            val songs = LikedStore.resolveOrdered(historyKeys, albums)
            if (songs.isEmpty()) null
            else buildQueueAlbum(QueueSpecs.recent(songs.size), songs)
        }
        id.startsWith("queue/playlist/") -> {
            val pid = id.removePrefix("queue/playlist/")
            val playlist = playlists.firstOrNull { it.id == pid } ?: return null
            val songs = LikedStore.resolveOrdered(playlist.keys, albums)
            if (songs.isEmpty()) null
            else buildQueueAlbum(
                QueueSpecs.playlist(pid, playlist.name, songs.size), songs,
            )
        }
        id.startsWith("queue/artist/") -> {
            val name = unslugArtist(id.removePrefix("queue/artist/"))
            val entry = LibraryLists.artists(albums).firstOrNull { it.name == name } ?: return null
            val songs = LibraryLists.artistSongs(entry)
            if (songs.isEmpty()) null
            else buildQueueAlbum(QueueSpecs.artist(name, songs.size), songs)
        }
        else -> null
    }
}
