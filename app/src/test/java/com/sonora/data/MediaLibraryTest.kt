package com.sonora.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryTest {

    private fun row(
        id: Long,
        albumId: Long = 7L,
        title: String = "Song",
        artist: String = "Artist",
        album: String = "Album",
        year: Int = 1973,
        track: Int = 1,
        durationMs: Long = 200_000L,
    ) = AudioRow(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        year = year,
        trackTag = track,
        durationMs = durationMs,
        uri = "content://media/audio/$id",
    )

    @Test
    fun groupsRowsIntoAlbums() {
        val albums = MediaLibrary.groupIntoAlbums(
            listOf(row(1L, albumId = 7L), row(2L, albumId = 9L), row(3L, albumId = 7L)),
        )
        assertEquals(2, albums.size)
        val first = albums.first { it.id == "media-7" }
        assertEquals(2, first.tracks.size)
        assertEquals("media-9", albums.first { it.id == "media-9" }.id)
    }

    @Test
    fun albumCarriesIdentityAndArtPointer() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(row(1L, albumId = 7L, track = 2), row(2L, albumId = 7L, track = 1)),
        ).single()
        assertEquals(7L, album.artSeed)
        assertEquals("Local files", album.label)
        assertEquals("content://media/audio/2", album.artUri)
        assertTrue(album.isReal)
    }

    @Test
    fun ordersTracksByTag() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(
                row(1L, title = "Third", track = 3),
                row(2L, title = "First", track = 1),
                row(3L, title = "Second", track = 2),
            ),
        ).single()
        assertEquals(listOf("First", "Second", "Third"), album.tracks.map { it.title })
        assertEquals(listOf(1, 2, 3), album.tracks.map { it.number })
    }

    @Test
    fun unpacksDiscPackedTrackNumbers() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(row(1L, track = 1003), row(2L, track = 2001)),
        ).single()
        assertEquals(listOf(3, 1), album.tracks.map { it.number })
    }

    @Test
    fun missingTrackNumbersFallBackToPosition() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(row(1L, title = "B", track = 0), row(2L, title = "A", track = 0)),
        ).single()
        // Same tag: title order decides, numbers fill 1..n.
        assertEquals(listOf("A", "B"), album.tracks.map { it.title })
        assertEquals(listOf(1, 2), album.tracks.map { it.number })
    }

    @Test
    fun blankTagsGetUnknownFallbacks() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(row(1L, title = "", artist = "", album = "")),
        ).single()
        assertEquals("Unknown title", album.tracks.single().title)
        assertEquals("Unknown artist", album.artist)
        assertEquals("Unknown album", album.title)
    }

    @Test
    fun mixedArtistsBecomeVarious() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(
                row(1L, artist = "One", track = 1),
                row(2L, artist = "Two", track = 2),
            ),
        ).single()
        assertEquals("Various artists", album.artist)
    }

    @Test
    fun yearIsMostCommonNonZero() {
        val album = MediaLibrary.groupIntoAlbums(
            listOf(
                row(1L, year = 1973, track = 1),
                row(2L, year = 0, track = 2),
                row(3L, year = 1981, track = 3),
                row(4L, year = 1973, track = 4),
            ),
        ).single()
        assertEquals(1973, album.year)
    }

    @Test
    fun allZeroYearsStayZero() {
        val album = MediaLibrary.groupIntoAlbums(listOf(row(1L, year = 0))).single()
        assertEquals(0, album.year)
        assertEquals("----", album.displayYear)
    }

    @Test
    fun albumsSortByArtistThenTitle() {
        val albums = MediaLibrary.groupIntoAlbums(
            listOf(
                row(1L, albumId = 1L, artist = "Zebra", album = "First"),
                row(2L, albumId = 2L, artist = "Apple", album = "Second"),
                row(3L, albumId = 3L, artist = "Apple", album = "First"),
            ),
        )
        assertEquals(listOf("media-3", "media-2", "media-1"), albums.map { it.id })
    }

    @Test
    fun negativeDurationsClampToZero() {
        val album = MediaLibrary.groupIntoAlbums(listOf(row(1L, durationMs = -50L))).single()
        assertEquals(0L, album.tracks.single().durationMs)
    }

    @Test
    fun emptyRowsGiveEmptyLibrary() {
        assertTrue(MediaLibrary.groupIntoAlbums(emptyList()).isEmpty())
    }

    @Test
    fun normalizeTrackNumber() {
        assertEquals(3, with(MediaLibrary) { 1003.normalizeTrackNumber() })
        assertEquals(1, with(MediaLibrary) { 2001.normalizeTrackNumber() })
        assertEquals(5, with(MediaLibrary) { 5.normalizeTrackNumber() })
        assertEquals(null, with(MediaLibrary) { 0.normalizeTrackNumber() })
        assertEquals(null, with(MediaLibrary) { 1000.normalizeTrackNumber() })
        assertEquals(null, with(MediaLibrary) { (-4).normalizeTrackNumber() })
    }
}
