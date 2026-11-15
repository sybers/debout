package fr.sybers.debout.domain

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WakeStateMachineTest {

    private val sentAt = mutableListOf<Long>()

    private fun TestScope.newMachine() = WakeStateMachine(
        sender = { sentAt += testScheduler.currentTime },
        scope = backgroundScope,
    )

    @Test
    fun `starts in Unknown`() = runTest {
        val machine = newMachine()

        assertEquals(WakeState.Unknown, machine.state.value)
    }

    @Test
    fun `responding probe makes an Unknown machine Online`() = runTest {
        val machine = newMachine()

        machine.onProbeResult(responds = true)

        assertEquals(WakeState.Online, machine.state.value)
    }

    @Test
    fun `silent probe makes an Unknown machine Offline`() = runTest {
        val machine = newMachine()

        machine.onProbeResult(responds = false)

        assertEquals(WakeState.Offline, machine.state.value)
    }

    @Test
    fun `waking an Offline machine sends three packets 100 ms apart`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)

        val result = machine.requestWake()
        advanceTimeBy(1.seconds)

        assertEquals(WakeRequestResult.Started, result)
        assertEquals(listOf(0L, 100L, 200L), sentAt)
    }

    @Test
    fun `waking goes through Sending then Booting once the last packet is sent`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)

        machine.requestWake()
        assertEquals(WakeState.Sending, machine.state.value)

        advanceTimeBy(199.milliseconds)
        assertEquals(WakeState.Sending, machine.state.value)

        advanceTimeBy(2.milliseconds)
        assertEquals(WakeState.Booting, machine.state.value)
    }

    @Test
    fun `Booting gives up with NoResponse 90 s after the last packet`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(201.milliseconds)

        advanceTimeBy(90.seconds - 2.milliseconds)
        assertEquals(WakeState.Booting, machine.state.value)

        advanceTimeBy(2.milliseconds)
        assertEquals(WakeState.NoResponse, machine.state.value)
    }

    @Test
    fun `responding probe while Booting makes the machine Online for good`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(30.seconds)

        machine.onProbeResult(responds = true)
        assertEquals(WakeState.Online, machine.state.value)

        advanceTimeBy(5.minutes)
        assertEquals(WakeState.Online, machine.state.value)
    }

    @Test
    fun `silent probe while Booting keeps waiting until the timeout`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(30.seconds)

        machine.onProbeResult(responds = false)
        assertEquals(WakeState.Booting, machine.state.value)

        advanceTimeBy(61.seconds)
        assertEquals(WakeState.NoResponse, machine.state.value)
    }

    private fun TestScope.machineWithNoResponse(): WakeStateMachine {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(91.seconds)
        check(machine.state.value == WakeState.NoResponse)
        return machine
    }

    @Test
    fun `silent probe keeps NoResponse so the user still sees the failure`() = runTest {
        val machine = machineWithNoResponse()

        machine.onProbeResult(responds = false)

        assertEquals(WakeState.NoResponse, machine.state.value)
    }

    @Test
    fun `late responding probe turns NoResponse into Online`() = runTest {
        val machine = machineWithNoResponse()

        machine.onProbeResult(responds = true)

        assertEquals(WakeState.Online, machine.state.value)
    }

    @Test
    fun `waking an Online machine sends nothing and reports it is already on`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = true)

        val result = machine.requestWake()
        advanceTimeBy(1.seconds)

        assertEquals(WakeRequestResult.AlreadyOnline, result)
        assertEquals(WakeState.Online, machine.state.value)
        assertEquals(emptyList<Long>(), sentAt)
    }

    @Test
    fun `cannot wake an Unknown machine`() = runTest {
        val machine = newMachine()

        val result = machine.requestWake()
        advanceTimeBy(1.seconds)

        assertEquals(WakeRequestResult.NotAllowed, result)
        assertEquals(WakeState.Unknown, machine.state.value)
        assertEquals(emptyList<Long>(), sentAt)
    }

    @Test
    fun `cannot wake again while Sending`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(50.milliseconds)

        val result = machine.requestWake()
        advanceTimeBy(1.seconds)

        assertEquals(WakeRequestResult.NotAllowed, result)
        assertEquals(listOf(0L, 100L, 200L), sentAt)
    }

    @Test
    fun `cannot wake again while Booting`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(30.seconds)

        val result = machine.requestWake()
        advanceTimeBy(1.seconds)

        assertEquals(WakeRequestResult.NotAllowed, result)
        assertEquals(WakeState.Booting, machine.state.value)
        assertEquals(listOf(0L, 100L, 200L), sentAt)
    }

    @Test
    fun `can retry from NoResponse`() = runTest {
        val machine = machineWithNoResponse()
        sentAt.clear()

        val result = machine.requestWake()
        assertEquals(WakeState.Sending, machine.state.value)
        advanceTimeBy(1.seconds)

        assertEquals(WakeRequestResult.Started, result)
        assertEquals(WakeState.Booting, machine.state.value)
        assertEquals(3, sentAt.size)
    }

    @Test
    fun `losing the local network makes any machine Unknown`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = true)

        machine.onLocalNetworkLost()

        assertEquals(WakeState.Unknown, machine.state.value)
    }

    @Test
    fun `losing the local network while Sending stops the remaining packets`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(150.milliseconds)

        machine.onLocalNetworkLost()
        advanceTimeBy(5.minutes)

        assertEquals(listOf(0L, 100L), sentAt)
        assertEquals(WakeState.Unknown, machine.state.value)
    }

    @Test
    fun `silent probe while Sending does not interrupt the wake`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = false)
        machine.requestWake()
        advanceTimeBy(50.milliseconds)

        machine.onProbeResult(responds = false)
        assertEquals(WakeState.Sending, machine.state.value)

        advanceTimeBy(1.seconds)
        assertEquals(WakeState.Booting, machine.state.value)
    }

    @Test
    fun `silent probe makes an Online machine Offline`() = runTest {
        val machine = newMachine()
        machine.onProbeResult(responds = true)

        machine.onProbeResult(responds = false)

        assertEquals(WakeState.Offline, machine.state.value)
    }
}
