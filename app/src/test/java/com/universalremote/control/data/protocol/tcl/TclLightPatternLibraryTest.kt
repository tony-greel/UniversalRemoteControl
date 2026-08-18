package com.universalremote.control.data.protocol.tcl

import com.universalremote.control.domain.model.AcMode
import com.universalremote.control.domain.model.AcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TclLightPatternLibraryTest {

    private val sampleState = AcState(
        powerOn = true,
        mode = AcMode.COOL,
        temperatureCelsius = 26f,
        lightOn = true,
    )

    @Test
    fun allCandidates_produceValidPatterns_forLightOff() {
        TclLightPatternLibrary.allCandidateIds().forEach { id ->
            val pattern = TclLightPatternLibrary.patternFor(id, lightOn = false, sampleState)
            assertNotNull("candidate $id should encode", pattern)
            assertEquals(38_000, pattern!!.carrierFrequencyHz)
            assertTrue(pattern.timingsMicros.size > 50)
        }
    }

    @Test
    fun allCandidates_produceValidPatterns_forLightOn() {
        TclLightPatternLibrary.allCandidateIds().forEach { id ->
            val pattern = TclLightPatternLibrary.patternFor(id, lightOn = true, sampleState)
            assertNotNull("candidate $id should encode", pattern)
        }
    }

    @Test
    fun lightOffAndOn_produceDifferentPatterns_forSameCandidate() {
        val off = TclLightPatternLibrary.patternFor("byte5_0x24_0x64", false, sampleState)
        val on = TclLightPatternLibrary.patternFor("byte5_0x24_0x64", true, sampleState)
        assertNotNull(off)
        assertNotNull(on)
        assertTrue(!off!!.timingsMicros.contentEquals(on!!.timingsMicros))
    }

    @Test
    fun candidateCount_isSix() {
        assertEquals(6, TclLightPatternLibrary.candidates.size)
    }
}
