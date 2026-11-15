package com.sonora.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioStudioConfigTest {

    private fun reset() {
        AudioStudioStore.setEqEnabled(true)
        AudioStudioStore.setPreset(EqPreset.FLAT)
        AudioStudioStore.setPreamp(0f)
        AudioStudioStore.setReplayGain(ReplayGainMode.OFF)
        AudioStudioStore.setGapless(true)
        AudioStudioStore.setCrossfadeEnabled(false)
        AudioStudioStore.setCrossfadeSec(6)
    }

    @Test
    fun defaultsMatchSpec() {
        reset()
        val config = AudioStudioStore.config
        assertEquals(AudioStudioConfig(), config)
        assertTrue(config.eqEnabled)
        assertTrue(config.gapless)
        assertFalse(config.crossfadeEnabled)
        assertEquals(ReplayGainMode.OFF, config.replayGain)
        assertEquals(EqPreset.FLAT, config.preset)
        assertEquals(List(EQ_BAND_COUNT) { 0f }, config.bandGainsDb)
    }

    @Test
    fun factoryPresetsHaveTenBandsInRange() {
        EQ_FACTORY_PRESETS.forEach { (preset, gains) ->
            assertEquals("$preset", EQ_BAND_COUNT, gains.size)
            gains.forEach { assertTrue("$preset", it in EQ_MIN_DB..EQ_MAX_DB) }
        }
        assertEquals(EQ_BAND_COUNT, EQ_FREQUENCY_LABELS.size)
    }

    @Test
    fun setPresetAppliesFactoryCurve() {
        reset()
        AudioStudioStore.setPreset(EqPreset.BASS_BOOST)
        val config = AudioStudioStore.config
        assertEquals(EqPreset.BASS_BOOST, config.preset)
        assertEquals(EQ_FACTORY_PRESETS.getValue(EqPreset.BASS_BOOST), config.bandGainsDb)
        reset()
    }

    @Test
    fun manualBandEditSelectsCustomAndClamps() {
        reset()
        AudioStudioStore.setPreset(EqPreset.VOCAL)
        AudioStudioStore.setBand(0, 99f)
        var config = AudioStudioStore.config
        assertEquals(EqPreset.CUSTOM, config.preset)
        assertEquals(EQ_MAX_DB, config.bandGainsDb[0], 0.001f)
        AudioStudioStore.setBand(9, -99f)
        config = AudioStudioStore.config
        assertEquals(EQ_MIN_DB, config.bandGainsDb[9], 0.001f)
        // Custom curve survives a preset round-trip.
        AudioStudioStore.setPreset(EqPreset.FLAT)
        AudioStudioStore.setPreset(EqPreset.CUSTOM)
        assertEquals(EQ_MAX_DB, AudioStudioStore.config.bandGainsDb[0], 0.001f)
        reset()
    }

    @Test
    fun outOfRangeBandIsIgnored() {
        reset()
        val before = AudioStudioStore.config
        AudioStudioStore.setBand(-1, 5f)
        AudioStudioStore.setBand(EQ_BAND_COUNT, 5f)
        assertEquals(before, AudioStudioStore.config)
    }

    @Test
    fun preampAndCrossfadeClamp() {
        reset()
        AudioStudioStore.setPreamp(99f)
        assertEquals(PREAMP_MAX_DB, AudioStudioStore.config.preampDb, 0.001f)
        AudioStudioStore.setPreamp(-99f)
        assertEquals(PREAMP_MIN_DB, AudioStudioStore.config.preampDb, 0.001f)
        AudioStudioStore.setCrossfadeSec(99)
        assertEquals(CROSSFADE_MAX_SEC, AudioStudioStore.config.crossfadeSec)
        AudioStudioStore.setCrossfadeSec(-1)
        assertEquals(CROSSFADE_MIN_SEC, AudioStudioStore.config.crossfadeSec)
        reset()
    }

    @Test
    fun togglesAreIndependent() {
        reset()
        AudioStudioStore.setCrossfadeEnabled(true)
        assertTrue(AudioStudioStore.config.crossfadeEnabled)
        assertTrue(AudioStudioStore.config.gapless)
        AudioStudioStore.setGapless(false)
        assertFalse(AudioStudioStore.config.gapless)
        assertTrue(AudioStudioStore.config.crossfadeEnabled)
        reset()
    }
}
