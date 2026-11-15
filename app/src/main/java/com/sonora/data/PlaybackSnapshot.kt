package com.sonora.data

/**
 * M7 resumable session: everything the service needs to rebuild the queue
 * and policy after a process death. Persisted on track change, play-state
 * flips, and every few seconds while playing, so a restart resumes where
 * listening stopped. Encoding is plain delimited text with control
 * characters scrubbed from free text; anything malformed decodes to null.
 */
data class PlaybackSnapshot(
    val albumId: String,
    val albumTitle: String,
    val albumArtist: String,
    val albumYear: Int,
    val albumLabel: String,
    val artSeed: Long,
    val artUri: String?,
    val trackNumbers: List<Int>,
    val trackTitles: List<String>,
    val trackDurations: List<Long>,
    val trackUris: List<String>,
    val trackIndex: Int,
    val positionMs: Long,
    val wasPlaying: Boolean,
    val shuffle: Boolean,
    val repeatOne: Boolean,
) {
    /** Rebuilds the album; null when the snapshot holds no tracks. */
    fun toAlbum(): Album? {
        if (trackUris.isEmpty()) return null
        return Album(
            id = albumId,
            title = albumTitle,
            artist = albumArtist,
            year = albumYear,
            label = albumLabel,
            tracks = trackUris.indices.map { i ->
                Track(
                    number = trackNumbers[i],
                    title = trackTitles[i],
                    durationMs = trackDurations[i],
                    uri = trackUris[i],
                )
            },
            artSeed = artSeed,
            artUri = artUri,
        )
    }

    companion object {
        private const val FIELD = '\u001F'
        private const val ITEM = '\u001E'

        /**
         * Captures [album] at [trackIndex]. Null for demo albums (nothing
         * real to resume) and empty track lists.
         */
        fun from(
            album: Album,
            trackIndex: Int,
            positionMs: Long,
            playing: Boolean,
            shuffle: Boolean,
            repeatOne: Boolean,
        ): PlaybackSnapshot? {
            if (!album.isReal) return null
            return PlaybackSnapshot(
                albumId = album.id,
                albumTitle = album.title,
                albumArtist = album.artist,
                albumYear = album.year,
                albumLabel = album.label,
                artSeed = album.artSeed,
                artUri = album.artUri,
                trackNumbers = album.tracks.map { it.number },
                trackTitles = album.tracks.map { it.title },
                trackDurations = album.tracks.map { it.durationMs },
                trackUris = album.tracks.map { it.uri!! },
                trackIndex = trackIndex.coerceIn(album.tracks.indices),
                positionMs = positionMs.coerceAtLeast(0L),
                wasPlaying = playing,
                shuffle = shuffle,
                repeatOne = repeatOne,
            )
        }

        fun encode(snapshot: PlaybackSnapshot): String {
            return listOf(
                scrub(snapshot.albumId),
                scrub(snapshot.albumTitle),
                scrub(snapshot.albumArtist),
                snapshot.albumYear.toString(),
                scrub(snapshot.albumLabel),
                snapshot.artSeed.toString(),
                scrub(snapshot.artUri ?: ""),
                snapshot.trackNumbers.joinToString(ITEM.toString()),
                snapshot.trackTitles.joinToString(ITEM.toString()) { scrub(it) },
                snapshot.trackDurations.joinToString(ITEM.toString()),
                snapshot.trackUris.joinToString(ITEM.toString()) { scrub(it) },
                snapshot.trackIndex.toString(),
                snapshot.positionMs.toString(),
                if (snapshot.wasPlaying) "1" else "0",
                if (snapshot.shuffle) "1" else "0",
                if (snapshot.repeatOne) "1" else "0",
            ).joinToString(FIELD.toString())
        }

        fun decode(raw: String?): PlaybackSnapshot? {
            if (raw.isNullOrEmpty()) return null
            val fields = raw.split(FIELD)
            if (fields.size != 16) return null
            try {
                val numbers = fields[7].split(ITEM).map { it.toInt() }
                val titles = fields[8].split(ITEM)
                val durations = fields[9].split(ITEM).map { it.toLong() }
                val uris = fields[10].split(ITEM)
                if (numbers.isEmpty() || titles.size != numbers.size ||
                    durations.size != numbers.size || uris.size != numbers.size
                ) {
                    return null
                }
                if (uris.any { it.isBlank() }) return null
                return PlaybackSnapshot(
                    albumId = fields[0],
                    albumTitle = fields[1],
                    albumArtist = fields[2],
                    albumYear = fields[3].toInt(),
                    albumLabel = fields[4],
                    artSeed = fields[5].toLong(),
                    artUri = fields[6].ifEmpty { null },
                    trackNumbers = numbers,
                    trackTitles = titles,
                    trackDurations = durations,
                    trackUris = uris,
                    trackIndex = fields[11].toInt().coerceIn(uris.indices),
                    positionMs = fields[12].toLong().coerceAtLeast(0L),
                    wasPlaying = fields[13] == "1",
                    shuffle = fields[14] == "1",
                    repeatOne = fields[15] == "1",
                )
            } catch (_: NumberFormatException) {
                return null
            } catch (_: IndexOutOfBoundsException) {
                return null
            }
        }

        private fun scrub(text: String): String {
            return text.replace(FIELD, ' ').replace(ITEM, ' ')
        }
    }
}
