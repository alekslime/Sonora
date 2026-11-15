package com.sonora.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * M1 Shelf: a vertical stack of floating, perspective-projected sleeves.
 * In M5 demo mode a banner offers the way back to the on-device library.
 */
@Composable
fun ShelfScreen(
    albums: List<Album>,
    onOpenAlbum: (Album) -> Unit,
    onBrowseAll: () -> Unit,
    modifier: Modifier = Modifier,
    demo: Boolean = false,
    onExitDemo: (() -> Unit)? = null,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val scope = rememberCoroutineScope()
    val state = rememberShelfState(albums.size)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss)))
            .semantics { contentDescription = "Album shelf" },
    ) {
        ShelfHeader(
            count = albums.size,
            demo = demo,
            onBrowseAll = onBrowseAll,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
        if (demo && onExitDemo != null) {
            DemoBanner(
                onExitDemo = onExitDemo,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        ShelfStack(
            albums = albums,
            state = state,
            onSleeveTap = { index ->
                scope.launch {
                    if (index == state.selectedIndex) {
                        onOpenAlbum(albums[index])
                    } else {
                        state.select(index)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        ShelfTagline(modifier = Modifier.padding(bottom = 28.dp))
    }
}

@Composable
private fun ShelfHeader(count: Int, demo: Boolean, onBrowseAll: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("SONORA".uppercase(), style = type.labelCaps, color = colors.ivory)
            Spacer(Modifier.height(2.dp))
            Text(
                if (demo) "Demo  •  $count records" else "$count records",
                style = type.caption,
                color = colors.ivoryFaint,
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBrowseAll) {
            Text("Browse", style = type.labelCaps, color = colors.amber)
        }
    }
}

@Composable
private fun DemoBanner(onExitDemo: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(role = Role.Button, onClickLabel = "Use my music library", onClick = onExitDemo),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "Demo library — use my music".uppercase(),
            style = type.labelCaps,
            color = colors.amber,
        )
    }
}

@Composable
private fun ShelfTagline(modifier: Modifier = Modifier) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Your music, reimagined as records.",
            style = type.headline,
            color = colors.ivory,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick an album to start.",
            style = type.body,
            color = colors.ivoryDim,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The perspective stack. A custom [Layout] (not a lazy list with
 * transforms) so sleeve geometry is exact: each sleeve is positioned from
 * the shared scroll offset with scale, fade and depth shading.
 */
@Composable
private fun ShelfStack(
    albums: List<Album>,
    state: ShelfState,
    onSleeveTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val artSize = remember(maxWidth, maxHeight) {
            minOf(maxWidth * 0.78f, maxHeight * 0.36f)
        }
        val spineHeight = remember(artSize) { (artSize * 0.15f).coerceIn(34.dp, 54.dp) }
        val spacing = remember(spineHeight) { spineHeight + 44.dp }
        val cameraDistancePx = remember(density) { 14f * density.density }
        state.spacingPx = with(density) { spacing.toPx() }

        val range = ShelfGeometry.visibleRange(state.offset.value, albums.size)
        val gestureScope = rememberCoroutineScope()
        val dragModifier = Modifier.pointerInput(state, albums.size) {
            val tracker = VelocityTracker()
            detectVerticalDragGestures(
                onDragStart = { tracker.resetTracking() },
                onDragCancel = { gestureScope.launch { state.settle() } },
                onDragEnd = { gestureScope.launch { state.fling(tracker.calculateVelocity().y) } },
                onVerticalDrag = { change, dragAmount ->
                    gestureScope.launch { state.dragBy(dragAmount) }
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                },
            )
        }

        Layout(
            content = {
                for (index in range) {
                    key(albums[index].id) {
                        val distance = index - state.offset.value
                        Sleeve(
                            album = albums[index],
                            artSize = artSize,
                            spineHeight = spineHeight,
                            cameraDistancePx = cameraDistancePx,
                            placement = ShelfGeometry.placement(distance, state.spacingPx),
                            distance = distance,
                            selected = index == state.selectedIndex,
                            onTap = { onSleeveTap(index) },
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(dragModifier),
        ) { measurables, constraints ->
            val artPx = with(density) { artSize.roundToPx() }
            val spinePx = with(density) { spineHeight.roundToPx() }
            val shadowPx = with(density) { SleeveShadowHeight.roundToPx() }
            val sleeveH = artPx + spinePx + shadowPx
            val placeables = measurables.map { it.measure(Constraints.fixed(artPx, sleeveH)) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                val anchorX = (constraints.maxWidth - artPx) / 2
                val anchorCenterY = constraints.maxHeight * 0.46f
                // Place bottom-first: upper sleeves overlap those below, so
                // every spine stays readable over the art beneath it.
                for (index in range.reversed()) {
                    val order = index - range.first
                    val distance = index - state.offset.value
                    val p = ShelfGeometry.placement(distance, state.spacingPx)
                    val w = artPx * p.scale
                    val h = sleeveH * p.scale
                    val x = anchorX + (artPx - w) / 2f
                    val y = anchorCenterY + p.yCenterPx - h / 2f
                    placeables[order].placeRelative(x.roundToInt(), y.roundToInt())
                }
            }
        }

        // Static vignette for photographic depth. No input handling,
        // so taps and drags pass straight through to the stack.
        val vignetteRadius = remember(density, maxWidth, maxHeight) {
            val longest = if (maxWidth > maxHeight) maxWidth else maxHeight
            with(density) { longest.toPx() * 0.72f }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        radius = vignetteRadius,
                    ),
                ),
        )
    }
}
