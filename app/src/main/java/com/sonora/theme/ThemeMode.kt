package com.sonora.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** M15 appearance mode. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** In-memory theme store; persisted alongside the other prefs. */
object ThemeStore {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun replace(mode: ThemeMode) {
        this.mode = mode
    }

    fun cycle() {
        mode = when (mode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }
}
