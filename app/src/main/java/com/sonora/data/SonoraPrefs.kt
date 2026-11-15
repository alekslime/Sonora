package com.sonora.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonora.gesture.GestureConfig
import com.sonora.studio.AudioStudioConfig
import com.sonora.studio.ReplayGainMode
import kotlinx.coroutines.flow.first

val Context.sonoraDataStore: DataStore<Preferences> by preferencesDataStore("sonora")

/**
 * M5 persistence for Studio and gesture settings. Reads degrade to
 * defaults entry-by-entry, so a corrupt or half-written store can never
 * break launch; encoding helpers are pure and tested.
 */
object SonoraPrefs {

    private val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
    private val EQ_BANDS = stringPreferencesKey("eq_bands")
    private val EQ_PRESET = stringPreferencesKey("eq_preset")
    private val PREAMP = floatPreferencesKey("preamp_db")
    private val REPLAY_GAIN = stringPreferencesKey("replay_gain")
    private val GAPLESS = booleanPreferencesKey("gapless")
    private val XFADE_ENABLED = booleanPreferencesKey("xfade_enabled")
    private val XFADE_SECS = intPreferencesKey("xfade_secs")

    private val GEST_ENABLED = booleanPreferencesKey("gest_enabled")
    private val GEST_HOLD_MS = longPreferencesKey("gest_hold_ms")
    private val GEST_HAPTICS = booleanPreferencesKey("gest_haptics")
    private val GEST_REDUCED_MOTION = booleanPreferencesKey("gest_reduced_motion")

    suspend fun loadStudio(context: Context): AudioStudioConfig {
        val prefs = context.sonoraDataStore.data.first()
        val defaults = AudioStudioConfig()
        return AudioStudioConfig(
            eqEnabled = prefs[EQ_ENABLED] ?: defaults.eqEnabled,
            bands = decodeBands(prefs[EQ_BANDS]) ?: defaults.bands,
            presetName = prefs[EQ_PRESET] ?: defaults.presetName,
            preampDb = (prefs[PREAMP] ?: defaults.preampDb)
                .coerceIn(AudioStudioConfig.MIN_DB, AudioStudioConfig.MAX_DB),
            replayGain = decodeReplayGain(prefs[REPLAY_GAIN]),
            gapless = prefs[GAPLESS] ?: defaults.gapless,
            crossfadeEnabled = prefs[XFADE_ENABLED] ?: defaults.crossfadeEnabled,
            crossfadeSecs = (prefs[XFADE_SECS] ?: defaults.crossfadeSecs)
                .coerceIn(AudioStudioConfig.MIN_CROSSFADE_SECS, AudioStudioConfig.MAX_CROSSFADE_SECS),
        )
    }

    suspend fun saveStudio(context: Context, config: AudioStudioConfig) {
        context.sonoraDataStore.edit { prefs ->
            prefs[EQ_ENABLED] = config.eqEnabled
            prefs[EQ_BANDS] = encodeBands(config.bands)
            prefs[EQ_PRESET] = config.presetName
            prefs[PREAMP] = config.preampDb
            prefs[REPLAY_GAIN] = config.replayGain.name
            prefs[GAPLESS] = config.gapless
            prefs[XFADE_ENABLED] = config.crossfadeEnabled
            prefs[XFADE_SECS] = config.crossfadeSecs
        }
    }

    suspend fun loadGestures(context: Context): GestureConfig {
        val prefs = context.sonoraDataStore.data.first()
        val defaults = GestureConfig()
        return GestureConfig(
            enabled = prefs[GEST_ENABLED] ?: defaults.enabled,
            holdDurationMs = (prefs[GEST_HOLD_MS] ?: defaults.holdDurationMs)
                .coerceIn(GestureConfig.MIN_HOLD_MS, GestureConfig.MAX_HOLD_MS),
            haptics = prefs[GEST_HAPTICS] ?: defaults.haptics,
            reducedMotion = prefs[GEST_REDUCED_MOTION] ?: defaults.reducedMotion,
        )
    }

    suspend fun saveGestures(context: Context, config: GestureConfig) {
        context.sonoraDataStore.edit { prefs ->
            prefs[GEST_ENABLED] = config.enabled
            prefs[GEST_HOLD_MS] = config.holdDurationMs
            prefs[GEST_HAPTICS] = config.haptics
            prefs[GEST_REDUCED_MOTION] = config.reducedMotion
        }
    }

    /** Comma-joined dB values, clamped to the Studio range. */
    fun encodeBands(bands: List<Float>): String {
        require(bands.size == AudioStudioConfig.BAND_COUNT) {
            "Need ${AudioStudioConfig.BAND_COUNT} bands"
        }
        return bands.joinToString(",") { AudioStudioConfig.clampDb(it).toString() }
    }

    /** Null when missing or malformed, so callers fall back to defaults. */
    fun decodeBands(raw: String?): List<Float>? {
        if (raw.isNullOrBlank()) return null
        val values = raw.split(",").map { part ->
            part.toFloatOrNull()?.let { AudioStudioConfig.clampDb(it) } ?: return null
        }
        return values.takeIf { it.size == AudioStudioConfig.BAND_COUNT }
    }

    fun decodeReplayGain(raw: String?): ReplayGainMode {
        return try {
            if (raw == null) ReplayGainMode.OFF else ReplayGainMode.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            ReplayGainMode.OFF
        }
    }
}
