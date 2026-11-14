package com.sonora.data

import kotlin.math.abs
import kotlin.random.Random

/**
 * Fictional M1 library. Deterministic: the same seed always yields the same
 * albums, tracks and durations, so layout and screenshots are reproducible.
 */
object MockLibrary {

    private data class Seed(
        val title: String,
        val artist: String,
        val year: Int,
        val label: String,
        val trackCount: Int,
    )

    private val SEEDS = listOf(
        Seed("Velvet Meridian", "The Amber Static", 1973, "Obsidian Press", 9),
        Seed("Night Cartography", "Sable & Pine", 1981, "Northfield", 10),
        Seed("Copper Hymns", "Low Orbit Choir", 1976, "Cathedral Tapes", 8),
        Seed("Glasshouse", "Mira Voss", 1994, "Fern & Fable", 11),
        Seed("Ultraviolet Gardens", "Neon Pastoral", 2019, "Slow Machines", 9),
        Seed("Songs for Slow Machines", "Tape Loop Cathedral", 2004, "Cathedral Tapes", 12),
        Seed("Obsidian", "Black Prism Society", 1988, "Obsidian Press", 8),
        Seed("Half-Light Parade", "The Ivory Divers", 1969, "Marigold Union", 10),
        Seed("Monochrome Rivers", "Ansel Krane", 1978, "Riverine", 9),
        Seed("Deep Sea Cinema", "Pelagic Twin", 2011, "Pelagic", 11),
        Seed("Aurora Static", "Northfield Howl", 1985, "Northfield", 9),
        Seed("Paper Satellites", "June Cartwheel", 1999, "Fern & Fable", 10),
        Seed("The Long Reverberation", "Cathedral of Wires", 2016, "Cathedral Tapes", 7),
        Seed("Saffron Skies", "Marigold Union", 1971, "Marigold Union", 9),
        Seed("Concrete Lullabies", "Grey Meridian", 2008, "Slow Machines", 10),
        Seed("Foxglove", "Wren Halloway", 2022, "Fern & Fable", 11),
        Seed("Tidal Memory", "Salt & Signal", 1991, "Riverine", 9),
        Seed("After the Static", "Low Orbit Choir", 2024, "Cathedral Tapes", 10),
        Seed("Brass Nocturnes", "The Copperfield Nine", 1965, "Marigold Union", 8),
        Seed("Field Recordings from Elsewhere", "Fern Atlas", 2025, "Slow Machines", 12),
    )

    private val TITLE_A = listOf(
        "Velvet", "Amber", "Midnight", "Copper", "Silent", "Paper", "Glass",
        "Ultraviolet", "Slow", "Hollow", "Ivory", "Tidal", "Neon", "Concrete",
        "Golden", "Static", "Lunar", "Electric", "Distant", "Wild",
    )
    private val TITLE_B = listOf(
        "Meridian", "Hymn", "Cartography", "Machine", "River", "Cinema",
        "Parade", "Gardens", "Signal", "Nocturne", "Satellite", "Memory",
        "Cathedral", "Lullaby", "Reverie", "Static", "Compass", "Harbor",
        "Aurora", "Mirage",
    )
    private val TITLE_C = listOf(
        "No. 1", "Reprise", "for Ghosts", "in Blue", "at Dawn", "Interlude",
        "for Wires", "Undertow", "Waltz", "Elegy", "Overture", "Refrain",
    )

    val albums: List<Album> = SEEDS.mapIndexed { index, seed ->
        val artSeed = abs((seed.title + seed.artist).hashCode().toLong()) + index * 7919L
        Album(
            id = "mock-%02d".format(index),
            title = seed.title,
            artist = seed.artist,
            year = seed.year,
            label = seed.label,
            tracks = buildTracks(artSeed, seed.trackCount),
            artSeed = artSeed,
        )
    }

    fun albumById(id: String): Album? = albums.firstOrNull { it.id == id }

    private fun buildTracks(artSeed: Long, count: Int): List<Track> {
        val random = Random(artSeed)
        val used = mutableSetOf<String>()
        return List(count) { i ->
            var title: String
            do {
                title = if (random.nextFloat() < 0.3f) {
                    "${TITLE_A.random(random)} ${TITLE_B.random(random)} ${TITLE_C.random(random)}"
                } else {
                    "${TITLE_A.random(random)} ${TITLE_B.random(random)}"
                }
            } while (!used.add(title))
            val durationMs = (130_000L + random.nextLong(250_000L)) / 1000 * 1000
            Track(number = i + 1, title = title, durationMs = durationMs)
        }
    }
}
