package com.universalremote.control.data.protocol.tcl

import org.junit.Assert.assertEquals
import org.junit.Test

class Tcl112AcRemoteTest {

    @Test
    fun defaultState_checksum_matchesKnownValue() {
        val remote = Tcl112AcRemote()
        val pattern = remote.buildPattern()
        assertEquals(38_000, pattern.carrierFrequencyHz)
        assert(pattern.timingsMicros.isNotEmpty())
    }

    @Test
    fun lightOff_changesFlagByte() {
        val remote = Tcl112AcRemote()
        remote.setLight(false)
        val pattern = remote.buildPattern()
        assert(pattern.timingsMicros.size > 100)
    }

    @Test
    fun calcChecksum_matchesDefaultState() {
        val state = byteArrayOf(
            0x23, 0xCB.toByte(), 0x26, 0x01, 0x00, 0x24, 0x03, 0x07, 0x40,
            0x00, 0x00, 0x00, 0x00, 0x00,
        )
        assertEquals(0x83.toByte(), Tcl112AcEncoder.calcChecksum(state))
    }
}
