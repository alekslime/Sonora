package com.sonora.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Sonora Nova — twin of the Figma rebrand (file: Sonora — Rebrand).
 * Ink-violet canvas, Iris is the brand/selection voice, Coral is the
 * play/energy action. Token names from M0 are kept so screens re-skin
 * from here; `amber` now holds Iris, new tokens carry the rest of the
 * Figma palette (Sonora / Primitives + Sonora / Color, Dark).
 */
@Immutable
data class SonoraColors(
    /** Canvas — Figma color/canvas, ink/950. */
    val obsidian: Color = Color(0xFF07070C),
    /** Gradient end / deepest. */
    val abyss: Color = Color(0xFF050508),
    /** Card surface — color/card, ink/800. */
    val charcoal: Color = Color(0xFF161622),
    /** Raised surface — color/raised, ink/700. */
    val graphite: Color = Color(0xFF1E1E2E),
    /** Primary text — paper/0. */
    val ivory: Color = Color(0xFFF7F6FF),
    /** Secondary text — paper/40. */
    val ivoryDim: Color = Color(0xFFA7A3B8),
    /** Faint text, captions — paper/25. */
    val ivoryFaint: Color = Color(0xFF6E6A85),
    /** Hairlines and dividers — ink/600. */
    val hairline: Color = Color(0xFF2B2B3D),
    /** Selection / brand — Iris, color/accent. (M0 name kept.) */
    val amber: Color = Color(0xFF7C6CFF),
    /** Pressed Iris — color/accent-strong, iris/600. */
    val amberDeep: Color = Color(0xFF5B4BD6),
    /** Soft Iris glow. */
    val amberGlow: Color = Color(0x667C6CFF),
    /** Scrim over artwork for legibility. */
    val scrim: Color = Color(0x99000000),
    /** Play / energy action — Coral, color/energy. */
    val coral: Color = Color(0xFFFF5C39),
    val coralLight: Color = Color(0xFFFF7E64),
    val mint: Color = Color(0xFF2FE6A7),
    val sun: Color = Color(0xFFFFC24B),
    val sky: Color = Color(0xFF4CC9F0),
) {
    /** Semantic alias: Iris selection colour. */
    val accent: Color get() = amber
    /** Semantic alias: Coral energy colour. */
    val energy: Color get() = coral
}

val LocalSonoraColors = staticCompositionLocalOf { SonoraColors() }
