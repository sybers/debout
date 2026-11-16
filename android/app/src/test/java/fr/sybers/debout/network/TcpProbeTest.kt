package fr.sybers.debout.network

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.time.Duration.Companion.milliseconds

class TcpProbeTest {

    private val loopback = InetAddress.getLoopbackAddress()
    private val probe = TcpProbe(timeout = 300.milliseconds)
    private val servers = mutableListOf<ServerSocket>()

    private fun openPort(): Int = ServerSocket(0, 50, loopback).also { servers += it }.localPort

    private fun closedPort(): Int = ServerSocket(0, 50, loopback).use { it.localPort }

    @After
    fun closeServers() = servers.forEach { it.close() }

    @Test
    fun `an open port is reported`() = runBlocking {
        val open = openPort()

        assertEquals(ProbeOutcome.PortOpen(open), probe.probe(loopback, listOf(closedPort(), open)))
    }

    @Test
    fun `the first open port in the given order wins`() = runBlocking {
        val first = openPort()
        val second = openPort()

        assertEquals(ProbeOutcome.PortOpen(second), probe.probe(loopback, listOf(closedPort(), second, first)))
    }

    @Test
    fun `refused connections still mean the host is up`() = runBlocking {
        assertEquals(ProbeOutcome.HostUp, probe.probe(loopback, listOf(closedPort(), closedPort())))
    }

    @Test
    fun `a host that never answers is silent`() = runBlocking {
        // TEST-NET-1 (RFC 5737) is never routed, so connections time out or fail without a refusal.
        val nowhere = InetAddress.getByName("192.0.2.1")

        assertEquals(ProbeOutcome.Silent, probe.probe(nowhere, listOf(445, 22)))
    }
}
