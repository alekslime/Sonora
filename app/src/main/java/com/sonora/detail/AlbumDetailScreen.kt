package com.sonora.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.components.PrototypePlaybackControls
import com.sonora.data.Album
import com.sonora.data.Track
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

/**
 * M1 album detail: artwork, identity, prototype controls and the track
 * list. Track rows are informational until M5 brings playback.
 */
@Composable
fun AlbumDetailScreen(album: Album, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss))),
    ) {
        item {
            DetailHeader(label = album.label, onBack = onBack)
        }
        item {
            val art = rememberAlbumArtwork(album)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f)
                    .clip(SonoraShapes.medium)
                    .background(Color(0xFF1A1A1E)),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) {
                    Image(
                        bitmap = art,
                        contentDescription = "Cover art for ${album.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        album.title.firstOrNull()?.uppercase() ?: "",
                        style = type.display,
                        color = colors.ivory.copy(alpha = 0.25f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(album.title, style = type.display, color = colors.ivory)
                Spacer(Modifier.height(4.dp))
                Text(album.artist, style = type.title, color = colors.amber)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${album.year}  •  ${album.tracks.size} tracks  •  ${album.totalDurationMs.toMinSec()}",
                    style = type.mono,
                    color = colors.ivoryDim,
                )
                Spacer(Modifier.height(18.dp))
                PrototypePlaybackControls(Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Text("Tracks".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
                Spacer(Modifier.height(4.dp))
            }
        }
        items(album.tracks, key = { it.number }) { track ->
            TrackRow(track = track, modifier = Modifier.padding(horizontal = 24.dp))
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun DetailHeader(label: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back to shelf", tint = colors.ivory)
        }
        Spacer(Modifier.width(4.dp))
        Text(label.uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
    }
}

@Composable
private fun TrackRow(track: Track, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(track.number),
            style = type.mono,
            color = colors.ivoryFaint,
            modifier = Modifier.width(32.dp),
        )
        Text(
            track.title,
            style = type.body,
            color = colors.ivory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(track.durationMs.toMinSec(), style = type.mono, color = colors.ivoryDim)
    }
}
