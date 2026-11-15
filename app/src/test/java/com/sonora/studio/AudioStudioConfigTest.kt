package com.sonora.studio

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioStudioConfigTest {

    @Test
    fun defaultsMatchSpec() {
        val config = AudioStudioConfig()
        assertTrue(config.eqEnabled)
        assertTrue(config.bands.all { it == 0f })
        assertEquals(ReplayGainMode.OFF, config.replayGain)
        assertTrue(config.gapless)
        assertEquals(false, config.crossfadeEnabled)
    }

    @Test
    fun bandsAndPreampClamp() {
        AudioStudioStore.setBand(0, 99f)
        AudioStudioStore.setBand(9, -99f)
        AudioStudioStore.setPreamp(30f)
        assertEquals(12f, AudioStudioStore.config.bands[0], 0.001f)
        assertEquals(-12f, AudioStudioStore.config.bands[9], 0.001f)
        assertEquals(12f, AudioStudioStore.config.preampDb, 0.001f)
        assertEquals("Custom", AudioStudioStore.config.presetName)
        AudioStudioStore.setBand(-1, 5f)
        AudioStudioStore.setBand(10, 5f)
        assertEquals(10, AudioStudioStore.config.bands.size)
    }

    @Test
    fun presetsApplyCleanCurves() {
        AudioStudioStore.applyPreset(AudioStudioConfig.BUILT_IN_PRESETS[1])
        assertEquals("Bass Boost", AudioStudioStore.config.presetName)
        assertTrue(AudioStudioStore.config.bands[0] > 0f)
        AudioStudioStore.applyPreset(AudioStudioConfig.BUILT_IN_PRESETS[0])
        assertTrue(AudioStudioStore.config.bands.all { it == 0f })
    }

    @Test
    fun crossfadeNeverFiresIntoGaplessBoundary() {
        val on = AudioStudioConfig(crossfadeEnabled = true, crossfadeSecs = 6)
        assertEquals(6000L, AudioStudioConfig.effectiveCrossfadeMs(on, gaplessBoundary = false))
        assertEquals(0L, AudioStudioConfig.effectiveCrossfadeMs(on, gaplessBoundary = true))
        val off = AudioStudioConfig(crossfadeEnabled = false, crossfadeSecs = 6)
        assertEquals(0L, AudioStudioConfig.effectiveCrossfadeMs(off, gaplessBoundary = false))
    }

    @Test
    fun crossfadeSecondsClamp() {
        AudioStudioStore.setCrossfadeSecs(99)
        assertEquals(
            AudioStudioConfig.MAX_CROSSFADE_SECS,
            AudioStudioStore.config.crossfadeSecs,
        )
        AudioStudioStore.setCrossfadeSecs(0)
        assertEquals(
            AudioStudioConfig.MIN_CROSSFADE_SECS,
            AudioStudioStore.config.crossfadeSecs,
        )
        AudioStudioStore.setCrossfadeSecs(4)
    }

    @Test
    fun outputTypeNames() {
        assertEquals("Phone speaker", OutputRoutes.typeName(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertEquals("Wired headphones", OutputRoutes.typeName(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        assertEquals("Bluetooth audio", OutputRoutes.typeName(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertEquals("USB audio", OutputRoutes.typeName(AudioDeviceInfo.TYPE_USB_DEVICE))
        assertEquals("Audio output", OutputRoutes.typeName(-12345))
    }
}
