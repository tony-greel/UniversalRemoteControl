package com.universalremote.control.domain.repository

import com.universalremote.control.data.ir.IrPattern
import com.universalremote.control.data.ir.IrTransmitter
import com.universalremote.control.data.preference.AcStateStore
import com.universalremote.control.data.preference.DevicePairingStore
import com.universalremote.control.data.preference.LightControlConfigStore
import com.universalremote.control.data.protocol.tcl.Tcl112AcRemote
import com.universalremote.control.data.protocol.tcl.TclLightPatternLibrary
import com.universalremote.control.domain.model.AcFanSpeed
import com.universalremote.control.domain.model.AcMode
import com.universalremote.control.domain.model.AcState
import com.universalremote.control.domain.model.DeviceConnectionResult
import com.universalremote.control.domain.model.DeviceProfile
import com.universalremote.control.domain.model.LightControlConfig
import com.universalremote.control.domain.model.LightControlMethod
import com.universalremote.control.domain.model.RemoteAction
import com.universalremote.control.domain.model.TransmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteControlRepositoryTest {

    private lateinit var fakeTransmitter: RecordingIrTransmitter
    private lateinit var pairingStore: InMemoryDevicePairingStore
    private lateinit var repository: RemoteControlRepository

    @Before
    fun setUp() {
        fakeTransmitter = RecordingIrTransmitter(hasEmitter = true)
        pairingStore = InMemoryDevicePairingStore()
        repository = createRepository(pairingStore)
    }

    private fun createRepository(
        pairing: InMemoryDevicePairingStore = pairingStore,
        acState: InMemoryAcStateStore = InMemoryAcStateStore(),
    ): RemoteControlRepository {
        return RemoteControlRepository(
            irTransmitter = fakeTransmitter,
            tcl112AcRemote = Tcl112AcRemote(),
            lightControlPreferences = InMemoryLightControlConfigStore(
                LightControlConfig(method = LightControlMethod.HYBRID_ALL),
            ),
            acStateStore = acState,
            devicePairingStore = pairing,
        )
    }

    @Test
    fun checkDeviceConnection_whenNotPaired_returnsFailed() {
        val device = repository.getPresetDevices().first()
        val result = repository.checkDeviceConnection(device)
        assertTrue(result is DeviceConnectionResult.Failed)
    }

    @Test
    fun checkDeviceConnection_whenPaired_returnsConnected() {
        val device = repository.getPresetDevices().first()
        repository.confirmDevicePairing(device.id)
        val result = repository.checkDeviceConnection(device)
        assertTrue(result is DeviceConnectionResult.Connected)
    }

    @Test
    fun checkDeviceConnection_withoutIrEmitter_returnsFailed() {
        fakeTransmitter.hasEmitter = false
        val device = repository.getPresetDevices().first()
        val result = repository.checkDeviceConnection(device)
        assertTrue(result is DeviceConnectionResult.Failed)
    }

    @Test
    fun sendConnectionProbe_transmitsIrPattern() {
        val device = repository.getPresetDevices().first()
        val result = repository.sendConnectionProbe(device)
        assertTrue(result is TransmitResult.Success)
        assertEquals(1, fakeTransmitter.transmitCount)
    }

    @Test
    fun init_restoresPersistedAcState() {
        val saved = AcState(
            powerOn = false,
            mode = AcMode.HEAT,
            temperatureCelsius = 26.5f,
            fanSpeed = AcFanSpeed.MED,
            lightOn = false,
        )
        val restored = createRepository(
            pairing = InMemoryDevicePairingStore(),
            acState = InMemoryAcStateStore(saved),
        )
        assertEquals(saved, restored.getAcState())
    }

    @Test
    fun sendAction_persistsUpdatedAcState() {
        val store = InMemoryAcStateStore()
        repository = createRepository(acState = store)

        repository.sendAcAction(RemoteAction.MODE_HEAT)

        assertEquals(AcMode.HEAT, store.load()?.mode)
    }

    @Test
    fun hybridLightOff_sendsAllStrategies() {
        val result = repository.sendAcAction(RemoteAction.LIGHT_OFF)
        assertTrue(result is TransmitResult.Success)
        val expectedCount = 1 + TclLightPatternLibrary.candidates.size
        assertEquals(expectedCount, fakeTransmitter.transmitCount)
    }

    @Test
    fun hybridLightOn_sendsAllStrategies() {
        val result = repository.sendAcAction(RemoteAction.LIGHT_ON)
        assertTrue(result is TransmitResult.Success)
        assertEquals(1 + TclLightPatternLibrary.candidates.size, fakeTransmitter.transmitCount)
    }

    @Test
    fun hybridSummary_includesBitVariantAndAllCandidates() {
        val summary = repository.hybridAttemptSummary()
        TclLightPatternLibrary.candidates.forEach { candidate ->
            assertTrue(summary.contains(candidate.label))
        }
    }

    @Test
    fun noIrEmitter_returnsFailure() {
        fakeTransmitter.hasEmitter = false
        val result = repository.sendAcAction(RemoteAction.POWER_TOGGLE)
        assertTrue(result is TransmitResult.Failure)
    }

    @Test
    fun sendAction_whenTransmitFails_doesNotPersistState() {
        val initial = repository.getAcState()
        fakeTransmitter.hasEmitter = false
        val result = repository.sendAcAction(RemoteAction.POWER_TOGGLE)
        assertTrue(result is TransmitResult.Failure)
        assertEquals(initial, repository.getAcState())
    }

    @Test
    fun sendAction_whenTransmitFails_acStateStoreUnchanged() {
        val saved = AcState(
            powerOn = true,
            mode = AcMode.COOL,
            temperatureCelsius = 24f,
            fanSpeed = AcFanSpeed.AUTO,
            lightOn = true,
        )
        val store = InMemoryAcStateStore(saved)
        repository = createRepository(acState = store)
        fakeTransmitter.hasEmitter = false
        repository.sendAcAction(RemoteAction.MODE_HEAT)
        assertEquals(saved, store.load())
    }

    private class RecordingIrTransmitter(
        var hasEmitter: Boolean,
    ) : IrTransmitter {
        var transmitCount = 0

        override fun hasIrEmitter(): Boolean = hasEmitter

        override fun transmit(pattern: IrPattern): Result<Unit> {
            transmitCount++
            return if (hasEmitter) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("no emitter"))
            }
        }
    }

    private class InMemoryLightControlConfigStore(
        private var config: LightControlConfig,
    ) : LightControlConfigStore {
        override fun load(): LightControlConfig = config

        override fun save(config: LightControlConfig) {
            this.config = config
        }
    }

    private class InMemoryAcStateStore(
        initial: AcState? = null,
    ) : AcStateStore {
        private var state: AcState? = initial

        override fun load(): AcState? = state

        override fun save(state: AcState) {
            this.state = state
        }
    }

    private class InMemoryDevicePairingStore : DevicePairingStore {
        private val pairedIds = mutableSetOf<String>()

        override fun isPaired(deviceId: String): Boolean = deviceId in pairedIds

        override fun markPaired(deviceId: String) {
            pairedIds.add(deviceId)
        }

        override fun clearPairing(deviceId: String) {
            pairedIds.remove(deviceId)
        }
    }
}
