package com.sonora.liquid

/**
 * M9 mathematical visualizer equations. The overlay cycles through them
 * with seamless crossfades; [LiquidConfig.mode] picks the opening one.
 */
enum class VizMode(val label: String) {
    MANDELBROT("Mandelbrot"),
    RIBBONS("Ribbons"),
    POLAR("Polar Scope"),
    STAR("Star"),
}
