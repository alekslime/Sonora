package com.sonora.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sonora Nova shapes — twin of Figma radius tokens:
 * sm 12 (chips/thumbs), md 20 (cards), lg 28 (sheets/hero),
 * xl 36 (cover art). Soft by default, never bubbly.
 */
val SonoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
