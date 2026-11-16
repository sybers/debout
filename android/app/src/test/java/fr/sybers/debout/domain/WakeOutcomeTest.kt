package fr.sybers.debout.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeOutcomeTest {

    @Test
    fun `a wake that comes online succeeds`() = runTest {
        val outcome = flowOf(WakeState.Sending, WakeState.Booting, WakeState.Online).awaitWakeOutcome()

        assertEquals(WakeOutcome.Online, outcome)
    }

    @Test
    fun `a wake that times out gets no response`() = runTest {
        val outcome = flowOf(WakeState.Sending, WakeState.Booting, WakeState.NoResponse).awaitWakeOutcome()

        assertEquals(WakeOutcome.NoResponse, outcome)
    }

    @Test
    fun `a wake cut by the loss of the local network is abandoned`() = runTest {
        val outcome = flowOf(WakeState.Booting, WakeState.Unknown).awaitWakeOutcome()

        assertEquals(WakeOutcome.Abandoned, outcome)
    }

    @Test
    fun `a machine already online when followed counts as a success`() = runTest {
        val outcome = flowOf(WakeState.Online).awaitWakeOutcome()

        assertEquals(WakeOutcome.Online, outcome)
    }

    @Test
    fun `a state machine that never saw the wake has nothing to report`() = runTest {
        val outcome = flowOf(WakeState.Unknown, WakeState.Offline).awaitWakeOutcome()

        assertEquals(WakeOutcome.Abandoned, outcome)
    }
}
