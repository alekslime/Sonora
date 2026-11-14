package com.sonora.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * M0 color tokens. Dark, restrained: deep charcoal/obsidian surfaces,
 * warm ivory text, a single analog-amber accent. Artwork supplies color.
 */
@Immutable
data class SonoraColors(
    /** Deepest background. */
    val obsidian: Color = Color(0xFF0B0B0D),
    /** Near-black vignette / gradient end. */
    val abyss: Color = Color(0xFF060608),
    /** Raised surface (spines, rows, sheets). */
    val charcoal: Color = Color(0xFF17171B),
    /** Higher raised surface / pressed states. */
    val graphite: Color = Color(0xFF212127),
    /** Primary text. */
    val ivory: Color = Color(0xFFF2EDE3),
    /** Secondary text. */
    val ivoryDim: Color = Color(0xFFB9B2A5),
    /** Faint text, captions. */
    val ivoryFaint: Color = Color(0xFF7C766C),
    /** Hairlines and dividers on dark. */
    val hairline: Color = Color(0xFF2A2A30),
    /** Primary accent. */
    val amber: Color = Color(0xFFE0A458),
    /** Deep accent for gradients / pressed states. */
    val amberDeep: Color = Color(0xFF8A5E26),
    /** Soft amber glow. */
    val amberGlow: Color = Color(0x66E0A458),
    /** Scrim over artwork for legibility. */
    val scrim: Color = Color(0x99000000),
)

val LocalSonoraColors = staticCompositionLocalOf { SonoraColors() }
