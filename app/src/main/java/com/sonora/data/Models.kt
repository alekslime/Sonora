package com.sonora.data

/** A single track on an album. */
data class Track(
    val number: Int,
    val title: String,
    val durationMs: Long,
    /** Content URI string for real (M5) tracks; null for mock/demo tracks. */
    val uri: String? = null,
)

/** An album in the library (mock for M1 demo, MediaStore-backed for M5). */
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int,
    val label: String,
    val tracks: List<Track>,
    /** Stable seed driving procedural artwork and derived details. */
    val artSeed: Long,
    /** Content URI string used to load embedded art; null for mock albums. */
    val artUri: String? = null,
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }

    /** True when every track points at a real playable file. */
    val isReal: Boolean get() = tracks.isNotEmpty() && tracks.all { it.uri != null }

    /** Year for display; real files often have no year tag. */
    val displayYear: String get() = if (year > 0) year.toString() else "----"
}

/** 184_000 -> "3:04". */
fun Long.toMinSec(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:%02d".format(seconds)
}
