package com.sonora.nowplaying

import com.sonora.data.Album

/** One lyric line: millisecond offset since track start + text. */
data class LyricLine(val startMs: Int, val text: String)

/**
 * Lyrics lookup. There is no lyrics source on-device yet, so demo
 * tracks are seeded with a sample — everyone else sees "No lyrics" in
 * the lyrics view rather than a fake empty timeline.
 */
object LyricsStore {
    private val seedLyrics: List<LyricLine> by lazy {
        listOf(
            LyricLine(0, "Static breathes in the wires"),
            LyricLine(2000, "A slow machine starts to hum"),
            LyricLine(4000, "Remember the stillness"),
            LyricLine(6200, "Between the stations"),
            LyricLine(8000, "We were paper satellites"),
            LyricLine(10000, "Drifting through the static sky"),
            LyricLine(12000, "But always back where they should be"),
            LyricLine(14000, "In the signal, find the light"),
        )
    }

    fun linesFor(album: Album, trackIndex: Int): List<LyricLine>? {
        return if (album.id == "mock-00") seedLyrics else null
    }
}
