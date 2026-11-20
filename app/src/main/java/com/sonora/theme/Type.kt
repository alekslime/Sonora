package com.sonora.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sonora Nova typography — twin of the Figma Sonora text styles.
 * Display face is Space Grotesk in Figma; on-device it resolves to
 * the platform grotesque (bundled-font hook: see SonoraFonts) with
 * the same ramp: Display 32/36 Bold -0.6, Headline 24/29 Medium,
 * Title 17 ExtraBold-ish, Body 15 Medium, caps labels + mono.
 */
@Immutable
data class SonoraTypography(
    val display: TextStyle = TextStyle(
        fontFamily = SonoraFonts.display,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp,
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = SonoraFonts.display,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.4).sp,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = SonoraFonts.ui,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = SonoraFonts.ui,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = SonoraFonts.ui,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    /** Small caps labels: apply .uppercase() at the call site. */
    val labelCaps: TextStyle = TextStyle(
        fontFamily = SonoraFonts.ui,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

val LocalSonoraTypography = staticCompositionLocalOf { SonoraTypography() }
