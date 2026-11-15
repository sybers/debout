package fr.sybers.debout.network

import android.util.Log
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.domain.Ipv4
import fr.sybers.debout.domain.WakeRequestResult
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.domain.WakeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.time.TimeMark
import kotlin.time.TimeSource

data class WakeStatus(val state: WakeState, val since: TimeMark)

/**
 * Owns one [WakeStateMachine] per machine, in an application-wide scope, so a wake keeps
 * going while the user moves between screens. Must be called from the main thread.
 */
class WakeCoordinator(
    private val machines: MachineRepository,
    localNetworkMonitor: LocalNetworkMonitor,
    private val packetSender: UdpMagicPacketSender,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val localNetwork: StateFlow<LocalNetwork?> =
        localNetworkMonitor.localNetwork.stateIn(scope, SharingStarted.Eagerly, null)

    private class Tracked(val stateMachine: WakeStateMachine, val status: StateFlow<WakeStatus>)

    private val tracked = HashMap<Long, Tracked>()

    fun status(machineId: Long): StateFlow<WakeStatus> = track(machineId).status

    fun wake(machineId: Long): WakeRequestResult = track(machineId).stateMachine.requestWake()

    private fun track(machineId: Long): Tracked = tracked.getOrPut(machineId) {
        val stateMachine = WakeStateMachine(sender = { send(machineId) }, scope = scope)
        scope.launch {
            localNetwork.collect { network ->
                // TODO: feed the TCP probe result instead (plan step 5). Until then a local
                // network only tells us the machine can be woken, never that it is online.
                if (network == null) stateMachine.onLocalNetworkLost() else stateMachine.onProbeResult(responds = false)
            }
        }
        val status = stateMachine.state
            .map { WakeStatus(it, timeSource.markNow()) }
            .stateIn(scope, SharingStarted.Eagerly, WakeStatus(stateMachine.state.value, timeSource.markNow()))
        Tracked(stateMachine, status)
    }

    private suspend fun send(machineId: Long) {
        val network = localNetwork.value ?: return
        val machine = machines.get(machineId) ?: return
        val target = machine.broadcast?.let(Ipv4::parse) ?: network.broadcast
        try {
            packetSender.send(machine.mac, target, machine.wolPort, network)
        } catch (e: IOException) {
            // The wake still ends in NoResponse, which is the honest outcome to show.
            Log.w(TAG, "Magic packet to ${machine.name} failed", e)
        }
    }

    private companion object {
        const val TAG = "WakeCoordinator"
    }
}
