package com.sonora.data

import com.sonora.studio.AudioStudioConfig
import com.sonora.studio.ReplayGainMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SonoraPrefsTest {

    @Test
    fun bandsRoundTrip() {
        val bands = listOf(-12f, -6f, -1.5f, 0f, 0.5f, 2f, 4f, 6f, 8f, 12f)
        assertEquals(bands, SonoraPrefs.decodeBands(SonoraPrefs.encodeBands(bands)))
    }

    @Test
    fun bandsEncodeClamps() {
        val bands = listOf(99f, -99f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val decoded = SonoraPrefs.decodeBands(SonoraPrefs.encodeBands(bands))!!
        assertEquals(12f, decoded[0], 0.001f)
        assertEquals(-12f, decoded[1], 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun bandsEncodeRejectsWrongSize() {
        SonoraPrefs.encodeBands(listOf(1f, 2f))
    }

    @Test
    fun bandsDecodeRejectsMalformed() {
        assertEquals(null, SonoraPrefs.decodeBands(null))
        assertEquals(null, SonoraPrefs.decodeBands(""))
        assertEquals(null, SonoraPrefs.decodeBands("1,2,3"))
        assertEquals(null, SonoraPrefs.decodeBands("0,0,0,0,0,0,0,0,0,0,0"))
        assertEquals(null, SonoraPrefs.decodeBands("a,b,c,d,e,f,g,h,i,j"))
        assertEquals(null, SonoraPrefs.decodeBands("0,0,0,0,0,0,0,0,0,x"))
    }

    @Test
    fun bandsDecodeClampsOutOfRange() {
        val decoded = SonoraPrefs.decodeBands("99,-99,0,0,0,0,0,0,0,0")!!
        assertEquals(AudioStudioConfig.MAX_DB, decoded[0], 0.001f)
        assertEquals(AudioStudioConfig.MIN_DB, decoded[1], 0.001f)
    }

    @Test
    fun replayGainDecodeFallsBackToOff() {
        assertEquals(ReplayGainMode.OFF, SonoraPrefs.decodeReplayGain(null))
        assertEquals(ReplayGainMode.TRACK, SonoraPrefs.decodeReplayGain("TRACK"))
        assertEquals(ReplayGainMode.ALBUM, SonoraPrefs.decodeReplayGain("ALBUM"))
        assertEquals(ReplayGainMode.OFF, SonoraPrefs.decodeReplayGain("BOGUS"))
    }
}
