package fr.sybers.debout.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ProbeOutcome {
    data class PortOpen(val port: Int) : ProbeOutcome

    /** Every port refused the connection: nothing listens, but the host answered. */
    data object HostUp : ProbeOutcome

    data object Silent : ProbeOutcome
}

class TcpProbe(private val timeout: Duration = 1.seconds) {

    private enum class Attempt { Open, Refused, NoAnswer }

    /** Tries all [ports] at once; an open port wins in the order given. */
    suspend fun probe(address: InetAddress, ports: List<Int>, newSocket: () -> Socket = ::Socket): ProbeOutcome =
        coroutineScope {
            val attempts = ports
                .map { port -> async(Dispatchers.IO) { attempt(newSocket(), address, port) } }
                .awaitAll()
            val openPort = ports.zip(attempts).firstOrNull { (_, attempt) -> attempt == Attempt.Open }?.first
            when {
                openPort != null -> ProbeOutcome.PortOpen(openPort)
                Attempt.Refused in attempts -> ProbeOutcome.HostUp
                else -> ProbeOutcome.Silent
            }
        }

    suspend fun isReachable(address: InetAddress): Boolean = withContext(Dispatchers.IO) {
        try {
            address.isReachable(timeout.inWholeMilliseconds.toInt())
        } catch (_: IOException) {
            false
        }
    }

    private fun attempt(socket: Socket, address: InetAddress, port: Int): Attempt = socket.use {
        try {
            it.connect(InetSocketAddress(address, port), timeout.inWholeMilliseconds.toInt())
            Attempt.Open
        } catch (e: ConnectException) {
            // Only the message tells a refusal (the host is up) from an unreachable host:
            // Android wraps ECONNREFUSED in a ConnectException like any other failure.
            if (e.message?.contains("Connection refused") == true) Attempt.Refused else Attempt.NoAnswer
        } catch (_: IOException) {
            Attempt.NoAnswer
        }
    }
}
