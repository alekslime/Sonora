package com.sonora.data

/** A single track on an album. */
data class Track(
    val number: Int,
    val title: String,
    val durationMs: Long,
)

/** An album in the (mock, for M1) library. */
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int,
    val label: String,
    val tracks: List<Track>,
    /** Stable seed driving procedural artwork and derived details. */
    val artSeed: Long,
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

/** 184_000 -> "3:04". */
fun Long.toMinSec(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:%02d".format(seconds)
}
