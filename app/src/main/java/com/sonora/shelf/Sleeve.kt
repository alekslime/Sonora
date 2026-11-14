package com.sonora.shelf

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography

private val SleeveShape = RoundedCornerShape(5.dp)

/**
 * One floating record sleeve: a perspective-tilted artwork surface with a
 * front-facing spine carrying the album identity. Artwork is always square.
 */
@Composable
fun Sleeve(
    album: Album,
    artSize: Dp,
    spineHeight: Dp,
    cameraDistancePx: Float,
    placement: SleevePlacement,
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = placement.scale
                scaleY = placement.scale
                alpha = placement.alpha
            }
            .clip(SleeveShape)
            .semantics(mergeDescendants = true) {
                contentDescription = "${album.title} by ${album.artist}"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
                    shadowElevation = if (selected) 18.dp.toPx() else 10.dp.toPx()
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
                )
            } else {
                PlaceholderInitial(album)
            }
            // Depth shading + top light for a physical read.
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
        Row(
            modifier = Modifier
                .size(width = artSize, height = spineHeight)
                .background(colors.charcoal)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 1.dp,
                            color = colors.amber.copy(alpha = 0.55f),
                            shape = SleeveShape,
                        )
                    } else Modifier,
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(2.dp))
            Text(
                text = album.title,
                style = type.caption,
                color = if (selected) colors.ivory else colors.ivoryDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = album.artist,
                style = type.caption,
                color = colors.ivoryFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
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
