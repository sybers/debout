package fr.sybers.debout.network

import android.util.Log
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.domain.Ipv4
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.ProbePorts
import fr.sybers.debout.domain.WakeRequestResult
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.domain.WakeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.Inet4Address
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.seconds
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
    private val probe: TcpProbe,
    private val hasLocalNetworkAccess: () -> Boolean,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    val localNetwork: StateFlow<LocalNetwork?> =
        localNetworkMonitor.localNetwork.stateIn(scope, SharingStarted.Eagerly, null)

    private inner class Tracked(val machineId: Long) {
        val stateMachine = WakeStateMachine(sender = { send(machineId) }, scope = scope)

        val status: StateFlow<WakeStatus> = stateMachine.state
            .map { WakeStatus(it, timeSource.markNow()) }
            .stateIn(scope, SharingStarted.Eagerly, WakeStatus(stateMachine.state.value, timeSource.markNow()))

        private var watchers = 0
        private var probing: Job? = null

        val watched: Flow<WakeStatus> = status
            .onStart { if (watchers++ == 0) probing = scope.launch { probeWhileOnLocalNetwork(this@Tracked) } }
            .onCompletion {
                if (--watchers == 0) {
                    probing?.cancel()
                    probing = null
                }
            }

        init {
            scope.launch {
                localNetwork.collect { if (it == null) stateMachine.onLocalNetworkLost() }
            }
        }
    }

    private val tracked = HashMap<Long, Tracked>()

    fun status(machineId: Long): StateFlow<WakeStatus> = track(machineId).status

    /** Same as [status], but probes the machine while collected: collect it only while visible. */
    fun watch(machineId: Long): Flow<WakeStatus> = track(machineId).watched

    fun currentStatus(machineId: Long): WakeStatus = track(machineId).status.value

    fun wake(machineId: Long): WakeRequestResult = track(machineId).stateMachine.requestWake()

    private fun track(machineId: Long): Tracked = tracked.getOrPut(machineId) { Tracked(machineId) }

    private suspend fun probeWhileOnLocalNetwork(tracked: Tracked) {
        localNetwork.collectLatest { network ->
            if (network == null) return@collectLatest
            while (true) {
                tracked.stateMachine.onProbeResult(responds = isUp(tracked.machineId, network))
                val waking = tracked.stateMachine.state.value.let { it == WakeState.Sending || it == WakeState.Booting }
                delay(if (waking) WAKING_PROBE_INTERVAL else PROBE_INTERVAL)
            }
        }
    }

    /**
     * Without the permission or any address to probe, the answer is "not seen up", which
     * still lets the user wake the machine: a broadcast needs neither.
     */
    private suspend fun isUp(machineId: Long, network: LocalNetwork): Boolean {
        if (!hasLocalNetworkAccess()) return false
        val machine = machines.get(machineId) ?: return false
        val candidates = (resolve(machine.hostname, network) + listOfNotNull(machine.ip?.let(Ipv4::parse))).distinct()
        val ports = ProbePorts.inOrder(machine.probePort)
        for (address in candidates) {
            when (val outcome = probe.probe(address, ports) { network.network.socketFactory.createSocket() }) {
                is ProbeOutcome.PortOpen -> return true.also { remember(machine, address, outcome.port) }
                ProbeOutcome.HostUp -> return true.also { remember(machine, address, machine.probePort) }
                ProbeOutcome.Silent -> Unit
            }
        }
        return candidates.any { probe.isReachable(it) }
    }

    /** Keeps the address that answered, so a DHCP change found by hostname sticks. */
    private suspend fun remember(machine: Machine, address: Inet4Address, port: Int?) {
        val ip = address.hostAddress
        if (ip != machine.ip || port != machine.probePort) machines.rememberProbeResult(machine.id, ip, port)
    }

    /** Tries the name as typed, then as an mDNS name, which Windows and macOS answer to. */
    private suspend fun resolve(hostname: String?, network: LocalNetwork): List<Inet4Address> {
        if (hostname == null) return emptyList()
        val names = if ('.' in hostname) listOf(hostname) else listOf(hostname, "$hostname.local")
        for (name in names) {
            // getAllByName cannot be interrupted, so let a slow lookup finish in the background.
            val lookup = scope.async(Dispatchers.IO) {
                try {
                    network.network.getAllByName(name).filterIsInstance<Inet4Address>()
                } catch (_: UnknownHostException) {
                    emptyList()
                }
            }
            val addresses = withTimeoutOrNull(RESOLVE_TIMEOUT) { lookup.await() }.orEmpty()
            if (addresses.isNotEmpty()) return addresses
        }
        return emptyList()
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
        val PROBE_INTERVAL = 5.seconds
        val WAKING_PROBE_INTERVAL = 2.seconds
        val RESOLVE_TIMEOUT = 2.seconds
    }
}
