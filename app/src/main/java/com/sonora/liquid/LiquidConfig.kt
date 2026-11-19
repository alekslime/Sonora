package com.sonora.liquid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** M3 visual presets. Each carries deep, body and accent colors. */
enum class LiquidPreset(
    val label: String,
    val deep: Color,
    val body: Color,
    val accent: Color,
) {
    OBSIDIAN("Obsidian", Color(0xFF0B0B0D), Color(0xFF2A2A33), Color(0xFFE0A458)),
    PRISM("Prism", Color(0xFF0A0F1E), Color(0xFF3B2E7A), Color(0xFF9AD1D4)),
    AMBER("Amber", Color(0xFF1A0F06), Color(0xFF7A3F16), Color(0xFFF5C86E)),
    DEEP_SEA("Deep Sea", Color(0xFF04141C), Color(0xFF0E4A5E), Color(0xFF7BD3EA)),
    MONOCHROME("Monochrome", Color(0xFF000000), Color(0xFF3A3A3E), Color(0xFFF2EDE3)),
    AURORA("Aurora", Color(0xFF06121A), Color(0xFF134E4A), Color(0xFF8EF0D2)),
}

/** Render quality: shader octaves and fallback particle counts. */
enum class LiquidQuality { LOW, MEDIUM, HIGH }

/** User-facing Liquid configuration. Persisted in M5 alongside audio prefs. */
data class LiquidConfig(
    val preset: LiquidPreset = LiquidPreset.OBSIDIAN,
    /** 0..1 visual response strength. */
    val intensity: Float = 0.8f,
    val quality: LiquidQuality = LiquidQuality.HIGH,
    /** Opening equation; the show cycles from here. */
    val mode: VizMode = VizMode.MANDELBROT,
    /** Auto-advance through the equations with crossfades. */
    val autoCycle: Boolean = true,
)

/** In-memory M3 store; M5 backs it with persistent preferences. */
object LiquidConfigStore {
    var config by mutableStateOf(LiquidConfig())
        private set

    fun setPreset(preset: LiquidPreset) {
        config = config.copy(preset = preset)
    }

    fun setIntensity(intensity: Float) {
        config = config.copy(intensity = intensity.coerceIn(0f, 1f))
    }

    fun setQuality(quality: LiquidQuality) {
        config = config.copy(quality = quality)
    }

    fun setMode(mode: VizMode) {
        config = config.copy(mode = mode)
    }

    fun setAutoCycle(autoCycle: Boolean) {
        config = config.copy(autoCycle = autoCycle)
    }
}
