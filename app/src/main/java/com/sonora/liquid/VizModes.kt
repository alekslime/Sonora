package com.sonora.liquid

/**
 * M10 oscilloscope-first equations in the Windows Media Player spirit:
 * every mode draws the live waveform or spectrum, not a simulation.
 * The overlay cycles through them with seamless crossfades;
 * [LiquidConfig.mode] picks the opening one.
 */
enum class VizMode(val label: String) {
    WAVES("Waves"),
    BARS("Bars"),
    SCOPE("Scope"),
    STORM("Storm"),
}
