package com.sonora.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runtime permission guarding the on-device music library. */
fun audioLibraryPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/** One MediaStore audio row, decoupled from [Cursor] so grouping stays pure. */
data class AudioRow(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val year: Int,
    val trackTag: Int,
    val durationMs: Long,
    val uri: String,
)

/**
 * M5 library scan. Queries MediaStore for music files and groups rows into
 * albums. IO stays in [scanLibrary]; [groupIntoAlbums] is pure and tested.
 */
object MediaLibrary {

    /**
     * Groups flat audio rows into albums sorted by artist then title, with
     * tracks ordered by disc/track tag. Missing tags degrade gracefully:
     * unknown names, track numbers falling back to position, year 0.
     */
    fun groupIntoAlbums(rows: List<AudioRow>): List<Album> {
        return rows.groupBy { it.albumId }
            .map { (albumId, albumRows) ->
                val ordered = albumRows.sortedWith(
                    compareBy({ it.trackTag }, { it.title }),
                )
                val artists = ordered.map { it.artist.ifBlank { "Unknown artist" } }.distinct()
                val years = ordered.mapNotNull { it.year.takeIf { y -> y > 0 } }
                Album(
                    id = "media-$albumId",
                    title = ordered.first().album.ifBlank { "Unknown album" },
                    artist = if (artists.size > 1) "Various artists" else artists.first(),
                    year = years.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0,
                    label = "Local files",
                    tracks = ordered.mapIndexed { index, row ->
                        Track(
                            number = row.trackTag.normalizeTrackNumber() ?: (index + 1),
                            title = row.title.ifBlank { "Unknown title" },
                            durationMs = row.durationMs.coerceAtLeast(0L),
                            uri = row.uri,
                        )
                    },
                    artSeed = albumId,
                    artUri = ordered.first().uri,
                )
            }
            .sortedWith(compareBy({ it.artist }, { it.title }))
    }

    /**
     * MediaStore TRACK packs disc and track (disc * 1000 + track). Returns
     * the in-disc track number, or null when the tag is missing/unusable so
     * the caller can fall back to positional numbering.
     */
    fun Int.normalizeTrackNumber(): Int? {
        val track = this % 1000
        return if (track > 0) track else null
    }

    /** Reads every music file on shared storage into albums. Never throws. */
    suspend fun scanLibrary(contentResolver: ContentResolver): List<Album> {
        return withContext(Dispatchers.IO) {
            try {
                groupIntoAlbums(readRows(contentResolver))
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun readRows(contentResolver: ContentResolver): List<AudioRow> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
        )
        val rows = mutableListOf<AudioRow>()
        val cursor: Cursor = contentResolver.query(
            uri,
            projection,
            MediaStore.Audio.Media.IS_MUSIC + " != 0",
            null,
            MediaStore.Audio.Media.ARTIST + ", " + MediaStore.Audio.Media.ALBUM,
        ) ?: return emptyList()
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                rows += AudioRow(
                    id = id,
                    title = it.getString(titleCol).orEmpty(),
                    artist = it.getString(artistCol).orEmpty(),
                    album = it.getString(albumCol).orEmpty(),
                    albumId = it.getLong(albumIdCol),
                    year = it.getInt(yearCol),
                    trackTag = it.getInt(trackCol),
                    durationMs = it.getLong(durationCol),
                    uri = ContentUris.withAppendedId(uri, id).toString(),
                )
            }
        }
        return rows
    }
}
