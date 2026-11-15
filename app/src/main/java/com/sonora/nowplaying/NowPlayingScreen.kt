package com.sonora.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.liquid.LiquidConfigStore
import com.sonora.liquid.LiquidSettingsSheet
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

private val ArtShape = RoundedCornerShape(8.dp)

/**
 * Now Playing: a clean player in SONORA's voice. Large artwork (tap to
 * open Liquid), track identity, minimal progress, and full transport:
 * shuffle, previous, play/pause, next, repeat. Playback is simulated
 * (see [NowPlayingState]); the audio engine arrives in M5.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    album: Album,
    startTrackIndex: Int,
    autoplay: Boolean,
    onBack: () -> Unit,
    onOpenLiquid: () -> Unit,
    onSession: (NowPlayingState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val state = remember(album.id, startTrackIndex) { NowPlayingState(album, startTrackIndex) }
    BackHandler(onBack = onBack)

    var persistedTrack by rememberSaveable(album.id, startTrackIndex) { mutableIntStateOf(startTrackIndex) }
    var persistedPosition by rememberSaveable(album.id, startTrackIndex) { mutableLongStateOf(0L) }
    var persistedPlaying by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(autoplay) }
    var persistedShuffle by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(false) }
    var persistedRepeat by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(RepeatMode.OFF) }
    LaunchedEffect(state) {
        state.restore(persistedTrack, persistedPosition, persistedPlaying, persistedShuffle, persistedRepeat)
        onSession(state)
        state.ticker()
    }
    LaunchedEffect(state.trackIndex, state.positionMs, state.isPlaying, state.shuffle, state.repeatMode) {
        persistedTrack = state.trackIndex
        persistedPosition = state.positionMs
        persistedPlaying = state.isPlaying
        persistedShuffle = state.shuffle
        persistedRepeat = state.repeatMode
    }
    var liquidSheetOpen by rememberSaveable { mutableStateOf(false) }

    val tint = remember(album.artSeed) {
        Color.hsv(((album.artSeed % 360 + 360) % 360).toFloat(), 0.55f, 0.20f)
    }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to tint,
                    0.45f to colors.obsidian,
                    1f to colors.abyss,
                ),
            )
            .semantics { contentDescription = "Now playing" },
    ) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Artwork(
                        album = album,
                        onOpenLiquid = onOpenLiquid,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(end = 28.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlayerBody(state = state, onOpenLiquidSettings = { liquidSheetOpen = true })
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopBar(albumTitle = album.title, onBack = onBack)
                Spacer(Modifier.height(8.dp))
                Artwork(
                    album = album,
                    onOpenLiquid = onOpenLiquid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .aspectRatio(1f),
                )
                Spacer(Modifier.height(20.dp))
                PlayerBody(
                    state = state,
                    onOpenLiquidSettings = { liquidSheetOpen = true },
                    modifier = Modifier.padding(horizontal = 28.dp),
                )
            }
        }
        if (liquidSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { liquidSheetOpen = false },
                containerColor = colors.charcoal,
                contentColor = colors.ivory,
            ) {
                LiquidSettingsSheet()
            }
        }
    }
}

@Composable
private fun TopBar(albumTitle: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close player", tint = colors.ivory)
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Playing from album".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
            Spacer(Modifier.height(2.dp))
            Text(
                albumTitle,
                style = type.caption,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun Artwork(album: Album, onOpenLiquid: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    Box(
        modifier
            .shadow(24.dp, ArtShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(ArtShape)
            .background(Color(0xFF1A1A1E))
            .clickable(
                role = Role.Button,
                onClickLabel = "Open Liquid visualizer",
                onClick = onOpenLiquid,
            ),
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
}

@Composable
private fun PlayerBody(
    state: NowPlayingState,
    onOpenLiquidSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(modifier.fillMaxWidth()) {
        Text(
            "Track %02d / %02d".format(state.trackIndex + 1, state.album.tracks.size),
            style = type.labelCaps,
            color = colors.ivoryFaint,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            state.track.title,
            style = type.title.copy(fontSize = 22.sp, lineHeight = 26.sp),
            color = colors.ivory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            state.album.artist,
            style = type.body,
            color = colors.ivoryDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(state.positionMs.toMinSec(), style = type.mono, color = colors.ivoryDim)
            Spacer(Modifier.weight(1f))
            Text(state.durationMs.toMinSec(), style = type.mono, color = colors.ivoryDim)
        }
        SeekBar(
            fraction = state.fraction,
            onSeek = { state.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        ControlsRow(
            isPlaying = state.isPlaying,
            shuffleOn = state.shuffle,
            repeatMode = state.repeatMode,
            onToggleShuffle = { state.toggleShuffle() },
            onPrev = { state.prev() },
            onToggle = { state.toggle() },
            onNext = { state.next() },
            onCycleRepeat = { state.cycleRepeat() },
        )
        Spacer(Modifier.height(4.dp))
        LiquidRow(onOpen = onOpenLiquidSettings)
        Spacer(Modifier.height(8.dp))
        Text(
            "Simulated playback — real audio arrives in M5.",
            style = type.caption,
            color = colors.ivoryFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LiquidRow(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Liquid visualizer settings", onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("Liquid".uppercase(), style = type.labelCaps, color = colors.ivoryDim)
        Spacer(Modifier.width(8.dp))
        Text(
            LiquidConfigStore.config.preset.label,
            style = type.caption,
            color = colors.amber,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.ivoryFaint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    BoxWithConstraints(modifier.height(32.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val knobStart = remember(fraction, constraints.maxWidth) {
            with(density) { (fraction.coerceIn(0f, 1f) * (constraints.maxWidth - 12.dp.toPx())).toDp() }
        }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput("tap", widthPx) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / widthPx).coerceIn(0f, 1f))
                    }
                }
                .pointerInput("drag", widthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                        change.consume()
                    }
                }
                .semantics { contentDescription = "Seek" },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(colors.graphite),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(colors.ivory),
            )
            Box(
                Modifier
                    .padding(start = knobStart)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(colors.ivory),
            )
        }
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    onToggleShuffle: () -> Unit,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleControl(
            active = shuffleOn,
            onClick = onToggleShuffle,
            description = if (shuffleOn) "Shuffle on" else "Shuffle off",
            icon = { tint -> Icon(Icons.Filled.Shuffle, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp)) },
        )
        IconButton(onClick = onPrev, modifier = Modifier.size(60.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track", tint = colors.ivory, modifier = Modifier.size(36.dp))
        }
        Surface(
            onClick = onToggle,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = colors.ivory,
            contentColor = colors.obsidian,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(60.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next track", tint = colors.ivory, modifier = Modifier.size(36.dp))
        }
        ToggleControl(
            active = repeatMode != RepeatMode.OFF,
            onClick = onCycleRepeat,
            description = "Repeat ${repeatMode.name.lowercase()}",
            icon = { tint ->
                Icon(
                    if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp),
                )
            },
        )
    }
}

@Composable
private fun ToggleControl(
    active: Boolean,
    onClick: () -> Unit,
    description: String,
    icon: @Composable (tint: Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    Column(
        modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = description },
        ) {
            icon(if (active) colors.amber else colors.ivoryDim)
        }
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) colors.amber else Color.Transparent),
        )
    }
}
