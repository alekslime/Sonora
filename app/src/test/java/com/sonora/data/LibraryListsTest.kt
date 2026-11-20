package com.sonora.data

import com.sonora.nowplaying.LikedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryListsTest {

    private fun albums() = listOf(
        Album(
            id = "a1", title = "T1", artist = "Zed", year = 2000, label = "L",
            tracks = listOf(
                Track(1, "banana", 120_000L),
                Track(2, "Apple", 180_000L),
            ),
            artSeed = 1L,
        ),
        Album(
            id = "a2", title = "T2", artist = "Ann", year = 2001, label = "L",
            tracks = listOf(Track(1, "mango", 60_000L)),
            artSeed = 2L,
        ),
        Album(
            id = "a3", title = "T3", artist = "Zed", year = 2002, label = "L",
            tracks = listOf(Track(1, "Cherry", 60_000L)),
            artSeed = 3L,
        ),
    )

    private fun titles(entries: List<LikedEntry>) =
        entries.map { it.album.tracks[it.trackIndex].title }

    @Test
    fun allSongsSortsByTitleCaseInsensitively() {
        assertEquals(
            listOf("Apple", "banana", "Cherry", "mango"),
            titles(LibraryLists.allSongs(albums())),
        )
        assertTrue(LibraryLists.allSongs(emptyList()).isEmpty())
    }

    @Test
    fun artistsGroupAndSort() {
        val artists = LibraryLists.artists(albums())
        assertEquals(listOf("Ann", "Zed"), artists.map { it.name })
        assertEquals(1, artists[0].albums.size)
        assertEquals(1, artists[0].songCount)
        assertEquals(2, artists[1].albums.size)
        assertEquals(3, artists[1].songCount)
        // Cover comes from the first album in library order.
        assertEquals("a1", artists[1].albums.first().id)
    }

    @Test
    fun artistSongsFollowAlbumOrder() {
        val zed = LibraryLists.artists(albums()).first { it.name == "Zed" }
        assertEquals(
            listOf("banana", "Apple", "Cherry"),
            titles(LibraryLists.artistSongs(zed)),
        )
    }
}
