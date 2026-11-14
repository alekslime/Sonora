package com.sonora.shelf

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography

private val SleeveShape = RoundedCornerShape(5.dp)

/** Soft contact shadow each sleeve casts on the sleeve below it. */
internal val SleeveShadowHeight = 12.dp

/**
 * One floating record sleeve: a perspective-tilted artwork surface with a
 * front-facing spine carrying the album identity, plus the depth cues that
 * sell the physicality — top-down lighting, distance desaturation, contact
 * shadows and a press response. Artwork is always square.
 */
@Composable
fun Sleeve(
    album: Album,
    artSize: Dp,
    spineHeight: Dp,
    cameraDistancePx: Float,
    placement: SleevePlacement,
    distance: Float,
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale = if (pressed) 0.965f else 1f
    val saturation = remember(distance) {
        ColorMatrix().apply { setToSaturation(ShelfGeometry.saturation(distance)) }
    }
    val accent = remember(album.artSeed) {
        Color.hsv(((album.artSeed % 360 + 360) % 360).toFloat(), 0.62f, 0.58f)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = placement.scale * pressScale
                scaleY = placement.scale * pressScale
                alpha = placement.alpha
            }
            .clip(SleeveShape)
            .semantics(mergeDescendants = true) {
                contentDescription = "${album.title} by ${album.artist}"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = if (selected) "Open album" else "Select album",
                onClick = onTap,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Artwork surface, tilted back around its bottom edge.
        Box(
            modifier = Modifier
                .size(artSize)
                .graphicsLayer {
                    rotationX = ShelfGeometry.TILT_DEGREES
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    cameraDistance = cameraDistancePx
                    shadowElevation = if (selected) 20.dp.toPx() else 12.dp.toPx()
                    ambientShadowColor = Color.Black
                    spotShadowColor = Color.Black
                }
                .clip(SleeveShape)
                .background(placeholderColor(album.artSeed)),
        ) {
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(saturation),
                )
            } else {
                PlaceholderInitial(album)
            }
            // Top-down lighting: sheen above, falloff below.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.16f),
                            ),
                        ),
                    ),
            )
            // Depth shading.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = placement.dim)),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            if (selected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, colors.amber.copy(alpha = 0.55f), SleeveShape),
                )
            }
        }
        // Front-facing spine with the album identity.
        Box(
            Modifier
                .size(width = artSize, height = spineHeight)
                .background(colors.charcoal),
        ) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(accent.copy(alpha = if (selected) 1f else 0.6f)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = album.title,
                    style = type.caption,
                    color = if (selected) colors.ivory else colors.ivoryDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = album.artist,
                    style = type.caption,
                    color = colors.ivoryFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 11.dp),
                )
            }
            // Physical thickness: lit top edge, dark bottom edge.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
            if (selected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, colors.amber.copy(alpha = 0.55f), SleeveShape),
                )
            }
        }
        // Contact shadow falling on the sleeve below.
        Box(
            Modifier
                .size(width = artSize, height = SleeveShadowHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
private fun PlaceholderInitial(album: Album) {
    val type = LocalSonoraTypography.current
    val colors = LocalSonoraColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = album.title.firstOrNull()?.uppercase() ?: "",
            style = type.display,
            color = colors.ivory.copy(alpha = 0.25f),
        )
    }
}

/** Stable dark placeholder derived from the seed (no per-frame allocation). */
private fun placeholderColor(seed: Long): Color {
    val hues = listOf(0xFF1E2A30, 0xFF2A1E33, 0xFF1E2A1E, 0xFF33231A, 0xFF1A2433)
    val index = ((seed % hues.size).toInt() + hues.size) % hues.size
    return Color(hues[index])
}
