package fr.sybers.debout.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class BroadcastTest {

    private fun ipv4(literal: String) = InetAddress.getByName(literal) as Inet4Address

    @Test
    fun `computes broadcast of a slash 24 network`() {
        assertEquals(ipv4("192.168.1.255"), Broadcast.of(ipv4("192.168.1.42"), 24))
    }

    @Test
    fun `computes broadcast of a prefix not aligned on a byte`() {
        assertEquals(ipv4("10.0.7.255"), Broadcast.of(ipv4("10.0.5.9"), 22))
    }

    @Test
    fun `a slash 32 host is its own broadcast`() {
        assertEquals(ipv4("10.0.5.9"), Broadcast.of(ipv4("10.0.5.9"), 32))
    }

    @Test
    fun `a slash 0 network broadcasts everywhere`() {
        assertEquals(ipv4("255.255.255.255"), Broadcast.of(ipv4("10.0.5.9"), 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a prefix length above 32`() {
        Broadcast.of(ipv4("10.0.5.9"), 33)
    }
}
