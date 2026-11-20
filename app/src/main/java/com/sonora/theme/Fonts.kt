package com.sonora.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sonora.R

/**
 * M13 faces: bundled Source Serif 4 (OFL) for display headlines with
 * an Anthropic register, platform grotesque for UI text.
 */
object SonoraFonts {
    val display: FontFamily = FontFamily(
        Font(R.font.source_serif4_semibold, FontWeight.SemiBold),
        Font(R.font.source_serif4_bold, FontWeight.Bold),
    )
    val ui: FontFamily = FontFamily.SansSerif
}
