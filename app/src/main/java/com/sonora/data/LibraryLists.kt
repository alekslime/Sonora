package com.sonora.data

import com.sonora.nowplaying.LikedEntry

/**
 * M12 library-wide lists derived from the scan. Pure.
 */
object LibraryLists {

    /** Every track in the library, sorted by title. */
    fun allSongs(albums: List<Album>): List<LikedEntry> {
        return albums.flatMap { album ->
            album.tracks.indices.map { LikedEntry(album, it) }
        }.sortedBy { it.album.tracks[it.trackIndex].title.lowercase() }
    }

    /** Artists A-Z with their albums in library order. */
    fun artists(albums: List<Album>): List<ArtistEntry> {
        return albums.groupBy { it.artist }
            .map { (name, list) ->
                ArtistEntry(
                    name = name,
                    albums = list,
                    songCount = list.sumOf { it.tracks.size },
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /** An artist's songs in album/track order. */
    fun artistSongs(entry: ArtistEntry): List<LikedEntry> {
        return entry.albums.flatMap { album ->
            album.tracks.indices.map { LikedEntry(album, it) }
        }
    }
}

/** An artist with their albums; cover art comes from the first album. */
data class ArtistEntry(
    val name: String,
    val albums: List<Album>,
    val songCount: Int,
)
