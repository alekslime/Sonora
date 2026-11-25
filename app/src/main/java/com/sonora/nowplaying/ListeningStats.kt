package com.sonora.nowplaying

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Top-artist row. */
data class ArtistCount(val name: String, val plays: Int)

/** Render-ready lifetime totals. */
data class StatsSummary(
    val totalPlays: Int,
    val totalMs: Long,
    val topArtists: List<ArtistCount>,
    val topTrackKey: String?,
)

/** Decoded store contents including the per-day play counts. */
data class StatsData(
    val plays: Int,
    val ms: Long,
    val artists: Map<String, Int>,
    val tracks: Map<String, Int>,
    val dayPlays: Map<String, Int> = emptyMap(),
)

/**
 * M12 lifetime listening counters. Recorded alongside history on
 * every play; maps are capped so storage stays flat. Persisted
 * through [com.sonora.data.SonoraPrefs]; codec and summary are pure.
 */
object ListeningStats {

    const val MAX_ARTISTS = 100
    const val MAX_TRACKS = 300
    const val MAX_DAYS = 365

    var totalPlays by mutableStateOf(0)
        private set

    var totalMs by mutableLongStateOf(0L)
        private set

    var artistPlays by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var trackPlays by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** Plays per day ("YYYY-MM-DD"), capped for storage. */
    var dayPlays by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    fun replace(
        plays: Int,
        ms: Long,
        artists: Map<String, Int>,
        tracks: Map<String, Int>,
        days: Map<String, Int> = emptyMap(),
    ) {
        totalPlays = plays.coerceAtLeast(0)
        totalMs = ms.coerceAtLeast(0L)
        artistPlays = cap(artists, MAX_ARTISTS)
        trackPlays = cap(tracks, MAX_TRACKS)
        dayPlays = cap(days, MAX_DAYS)
    }

    /**
     * Plays per day over the last [days], oldest-first, aligned to the
     * current calendar date.
     */
    fun playsByDay(days: Int = 7): List<Int> {
        val key = dayKey()
        return List(days) { i ->
            dayPlays[key.minusDays(i)] ?: 0
        }.asReversed()
    }

    fun reset() {
        totalPlays = 0
        totalMs = 0L
        artistPlays = emptyMap()
        trackPlays = emptyMap()
    }

    fun record(artist: String, key: String, durationMs: Long) {
        totalPlays += 1
        totalMs += durationMs.coerceAtLeast(0L)
        val today = dayKey()
        dayPlays = cap(dayPlays + (today to (dayPlays[today] ?: 0) + 1), MAX_DAYS)
        artistPlays = cap(artistPlays + (artist to (artistPlays[artist] ?: 0) + 1), MAX_ARTISTS)
        trackPlays = cap(trackPlays + (key to (trackPlays[key] ?: 0) + 1), MAX_TRACKS)
    }

    /** "YYYY-MM-DD" of the local current date. */
    fun dayKey(): String = java.time.LocalDate.now().toString()

    /** A [java.time.LocalDate] that is [daysAgo] days back. */
    private fun String.minusDays(daysAgo: Int): String =
        java.time.LocalDate.parse(this).minusDays(daysAgo.toLong()).toString()

    fun summary(): StatsSummary {
        val artists = artistPlays.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(3)
            .map { ArtistCount(it.key, it.value) }
        val topTrack = trackPlays.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key
        return StatsSummary(totalPlays, totalMs, artists, topTrack)
    }

    /** 7384000 -> "2h 3m"; under an hour -> "45m"; silent -> "0m". */
    fun formatDuration(ms: Long): String {
        val minutes = (ms.coerceAtLeast(0L) / 60_000L).toInt()
        if (minutes < 60) return "${minutes}m"
        return "${minutes / 60}h ${minutes % 60}m"
    }

    private fun cap(counts: Map<String, Int>, max: Int): Map<String, Int> {
        if (counts.size <= max) return counts
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(max)
            .associate { it.key to it.value }
    }

    private const val FIELD = '\u001F'

    fun encode(): String {
        val lines = mutableListOf("$totalPlays,$totalMs")
        artistPlays.forEach { (name, count) -> lines += "a$FIELD${scrub(name)}$FIELD$count" }
        trackPlays.forEach { (key, count) -> lines += "t$FIELD${scrub(key)}$FIELD$count" }
        dayPlays.forEach { (key, count) -> lines += "d$FIELD$key$FIELD$count" }
        return lines.joinToString("\n")
    }

    /** Null when blank or malformed, so callers fall back to zeros. */
    fun decode(raw: String?): StatsData? {
        if (raw.isNullOrBlank()) return null
        return try {
            val lines = raw.split("\n")
            val totals = lines[0].split(",")
            if (totals.size != 2) return null
            val artists = mutableMapOf<String, Int>()
            val tracks = mutableMapOf<String, Int>()
            val days = mutableMapOf<String, Int>()
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val fields = line.split(FIELD)
                if (fields.size != 3) return null
                val count = fields[2].toInt()
                if (count < 0) return null
                when (fields[0]) {
                    "a" -> artists[fields[1]] = count
                    "t" -> tracks[fields[1]] = count
                    "d" -> days[fields[1]] = count
                    else -> return null
                }
            }
            StatsData(totals[0].toInt(), totals[1].toLong(), artists, tracks, days)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun scrub(text: String): String {
        return text.replace(FIELD, ' ').replace('\n', ' ')
    }
}
