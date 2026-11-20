package com.sonora.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningStatsTest {

    @Test
    fun recordAccumulatesAndSummarizes() {
        ListeningStats.reset()
        ListeningStats.record("Ann", "k1", 120_000L)
        ListeningStats.record("Ann", "k1", 120_000L)
        ListeningStats.record("Bob", "k2", 60_000L)
        val summary = ListeningStats.summary()
        assertEquals(3, summary.totalPlays)
        assertEquals(300_000L, summary.totalMs)
        assertEquals(listOf(ArtistCount("Ann", 2), ArtistCount("Bob", 1)), summary.topArtists)
        assertEquals("k1", summary.topTrackKey)
        ListeningStats.reset()
    }

    @Test
    fun summaryTiesBreakAlphabetically() {
        ListeningStats.reset()
        ListeningStats.record("Zed", "k1", 1000L)
        ListeningStats.record("Ann", "k2", 1000L)
        val summary = ListeningStats.summary()
        assertEquals("Ann", summary.topArtists.first().name)
        assertEquals("k1", summary.topTrackKey)
        ListeningStats.reset()
    }

    @Test
    fun mapsStayCapped() {
        ListeningStats.reset()
        repeat(ListeningStats.MAX_ARTISTS + 10) { ListeningStats.record("a$it", "k$it", 1000L) }
        assertEquals(ListeningStats.MAX_ARTISTS, ListeningStats.artistPlays.size)
        assertEquals(ListeningStats.MAX_ARTISTS + 10, ListeningStats.totalPlays)
        ListeningStats.reset()
    }

    @Test
    fun formatDurationReadsNaturally() {
        assertEquals("0m", ListeningStats.formatDuration(0L))
        assertEquals("0m", ListeningStats.formatDuration(-5L))
        assertEquals("45m", ListeningStats.formatDuration(45L * 60_000L))
        assertEquals("2h 3m", ListeningStats.formatDuration(7_380_000L))
        assertEquals("1h 0m", ListeningStats.formatDuration(3_600_000L))
    }

    @Test
    fun codecRoundTripsAndDegrades() {
        ListeningStats.reset()
        ListeningStats.record("Ann", "k1", 120_000L)
        val decoded = ListeningStats.decode(ListeningStats.encode())!!
        assertEquals(1, decoded.plays)
        assertEquals(120_000L, decoded.ms)
        assertEquals(mapOf("Ann" to 1), decoded.artists)
        assertEquals(mapOf("k1" to 1), decoded.tracks)
        assertNull(ListeningStats.decode(null))
        assertNull(ListeningStats.decode("  "))
        assertNull(ListeningStats.decode("nope"))
        assertNull(ListeningStats.decode("1,2\nx\u001Fy\u001F-1"))
        ListeningStats.reset()
    }
}
