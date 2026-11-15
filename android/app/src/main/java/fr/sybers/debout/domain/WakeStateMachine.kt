package fr.sybers.debout.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class WakeState { Unknown, Offline, Sending, Booting, Online, NoResponse }

enum class WakeRequestResult { Started, AlreadyOnline, NotAllowed }

fun interface MagicPacketSender {
    suspend fun send()
}

data class WakeTiming(
    val packetCount: Int = 3,
    val packetInterval: Duration = 100.milliseconds,
    val bootTimeout: Duration = 90.seconds,
)

class WakeStateMachine(
    private val sender: MagicPacketSender,
    private val scope: CoroutineScope,
    private val timing: WakeTiming = WakeTiming(),
) {
    private val _state = MutableStateFlow(WakeState.Unknown)
    val state: StateFlow<WakeState> = _state.asStateFlow()

    private var wakeJob: Job? = null

    fun requestWake(): WakeRequestResult {
        when (_state.value) {
            WakeState.Online -> return WakeRequestResult.AlreadyOnline
            WakeState.Unknown, WakeState.Sending, WakeState.Booting -> return WakeRequestResult.NotAllowed
            WakeState.Offline, WakeState.NoResponse -> Unit
        }
        _state.value = WakeState.Sending
        wakeJob = scope.launch {
            repeat(timing.packetCount) { index ->
                if (index > 0) delay(timing.packetInterval)
                sender.send()
            }
            _state.value = WakeState.Booting
            delay(timing.bootTimeout)
            _state.value = WakeState.NoResponse
        }
        return WakeRequestResult.Started
    }

    fun onLocalNetworkLost() {
        wakeJob?.cancel()
        _state.value = WakeState.Unknown
    }

    fun onProbeResult(responds: Boolean) {
        if (responds) {
            wakeJob?.cancel()
            _state.value = WakeState.Online
            return
        }
        when (_state.value) {
            WakeState.Unknown, WakeState.Online -> _state.value = WakeState.Offline
            WakeState.Offline, WakeState.Sending, WakeState.Booting, WakeState.NoResponse -> Unit
        }
    }
}
