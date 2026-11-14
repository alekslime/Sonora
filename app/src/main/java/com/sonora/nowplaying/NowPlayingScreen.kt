package com.sonora.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.data.toMinSec
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraMotion
import kotlin.math.roundToInt

/**
 * M2 Now Playing: the record player. Vinyl, tonearm and prism on
 * near-black, with track info, a minimal progress indicator and
 * immediately understandable transport controls. Playback is simulated
 * (see [NowPlayingState]); the audio engine arrives in M5.
 */
@Composable
fun NowPlayingScreen(
    album: Album,
    startTrackIndex: Int,
    autoplay: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val state = remember(album.id, startTrackIndex) { NowPlayingState(album, startTrackIndex) }
    BackHandler(onBack = onBack)

    // Persist transport position across rotation and process death.
    var persistedTrack by rememberSaveable(album.id, startTrackIndex) { mutableIntStateOf(startTrackIndex) }
    var persistedPosition by rememberSaveable(album.id, startTrackIndex) { mutableLongStateOf(0L) }
    var persistedPlaying by rememberSaveable(album.id, startTrackIndex) { mutableStateOf(autoplay) }
    LaunchedEffect(state) {
        state.restore(persistedTrack, persistedPosition, persistedPlaying)
        state.ticker()
    }
    LaunchedEffect(state.trackIndex, state.positionMs, state.isPlaying) {
        persistedTrack = state.trackIndex
        persistedPosition = state.positionMs
        persistedPlaying = state.isPlaying
    }

    // Needle cue on entry and on every track change.
    val cue = remember { Animatable(0f) }
    LaunchedEffect(state.trackIndex) {
        cue.snapTo(0f)
        cue.animateTo(1f, tween(180))
        cue.animateTo(0f, tween(300))
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .semantics { contentDescription = "Now playing" },
    ) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Stage(
                    state = state,
                    cueLift = cue.value,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                )
                InfoBlock(
                    state = state,
                    onBack = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopBar(onBack = onBack)
                Stage(
                    state = state,
                    cueLift = cue.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                InfoBlock(
                    state = state,
                    onBack = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.ivory)
        }
        Spacer(Modifier.width(4.dp))
        Text("Now playing".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
    }
}

@Composable
private fun InfoBlock(state: NowPlayingState, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (onBack != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.ivory)
                }
                Spacer(Modifier.width(4.dp))
                Text("Now playing".uppercase(), style = type.labelCaps, color = colors.ivoryFaint)
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            "Track %02d / %02d".format(state.trackIndex + 1, state.album.tracks.size),
            style = type.labelCaps,
            color = colors.ivoryFaint,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            state.track.title,
            style = type.headline,
            color = colors.ivory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            state.album.artist,
            style = type.title,
            color = colors.amber,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${state.album.title}  •  ${state.album.year}",
            style = type.caption,
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
        Spacer(Modifier.height(6.dp))
        ControlsRow(
            isPlaying = state.isPlaying,
            onPrev = { state.prev() },
            onToggle = { state.toggle() },
            onNext = { state.next() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Simulated playback — real audio arrives in M5.",
            style = type.caption,
            color = colors.ivoryFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Stage(
    state: NowPlayingState,
    cueLift: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val diameter = remember(maxWidth, maxHeight) { minOf(maxWidth * 0.96f, maxHeight * 0.68f) }
        val diameterPx = remember(density, diameter) { with(density) { diameter.toPx() } }
        val vinylCx = constraints.maxWidth * 0.45f
        val vinylCy = constraints.maxHeight * 0.46f
        val vinylLeft = vinylCx - diameterPx / 2f
        val vinylTop = vinylCy - diameterPx / 2f
        val pivot = remember(constraints.maxWidth, vinylTop, density) {
            Offset(constraints.maxWidth * 0.90f, vinylTop + with(density) { 28.dp.toPx() })
        }
        val stylus = remember(state.fraction, vinylCx, vinylCy, diameterPx) {
            stylusPoint(
                fraction = state.fraction,
                center = Offset(vinylCx, vinylCy),
                outerR = diameterPx / 2f,
                labelR = diameterPx / 2f * 0.32f,
            )
        }
        val cueLiftPx = remember(density, cueLift) { with(density) { 10.dp.toPx() } * cueLift }
        val prismLeft = vinylLeft + diameterPx * 0.14f
        val prismTop = vinylTop + diameterPx * 0.58f
        val art = rememberAlbumArtwork(state.album)
        val glowSize = diameter * 1.5f
        val glowPx = remember(density, glowSize) { with(density) { glowSize.toPx() } }
        val glowBrush = remember(colors) {
            Brush.radialGradient(
                colors = listOf(colors.amberGlow.copy(alpha = 0.35f), Color.Transparent),
                radius = glowPx / 2f,
            )
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (vinylCx - glowPx / 2f).roundToInt(),
                            (vinylCy - glowPx / 2f).roundToInt(),
                        )
                    }
                    .size(glowSize)
                    .background(glowBrush),
            )
            AnimatedContent(
                targetState = state.trackIndex,
                transitionSpec = {
                    (fadeIn(tween(SonoraMotion.SCREEN_MILLIS)) + scaleIn(tween(SonoraMotion.SCREEN_MILLIS), 0.96f))
                        .togetherWith(fadeOut(tween(SonoraMotion.SCREEN_MILLIS)))
                },
                label = "vinyl-change",
                modifier = Modifier
                    .offset { IntOffset(vinylLeft.roundToInt(), vinylTop.roundToInt()) }
                    .size(diameter),
            ) { track ->
                // Keyed so each track gets a fresh record (needle drop).
                key(track) {
                    Vinyl(album = state.album, diameter = diameter, isPlaying = state.isPlaying, art = art)
                }
            }
            Tonearm(pivot = pivot, stylus = stylus, cueLiftPx = cueLiftPx)
            PrismMotif(
                size = 88.dp,
                energy = mockEnergy(state.positionMs, state.isPlaying),
                modifier = Modifier.offset { IntOffset(prismLeft.roundToInt(), prismTop.roundToInt()) },
            )
        }
    }
}

@Composable
private fun SeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    BoxWithConstraints(modifier.height(32.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val knobStart = remember(fraction, constraints.maxWidth) {
            with(density) { (fraction.coerceIn(0f, 1f) * (constraints.maxWidth - 10.dp.toPx())).toDp() }
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
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(colors.graphite),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(colors.amber),
            )
            Box(
                Modifier
                    .padding(start = knobStart)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.amber),
            )
        }
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = {}, enabled = false, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.QueueMusic, contentDescription = "Queue, arrives in M5", tint = colors.ivoryFaint.copy(alpha = 0.5f))
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track", tint = colors.ivory, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            onClick = onToggle,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = colors.amber,
            contentColor = colors.obsidian,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next track", tint = colors.ivory, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = {}, enabled = false, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "Volume, arrives in M5", tint = colors.ivoryFaint.copy(alpha = 0.5f))
        }
    }
}
