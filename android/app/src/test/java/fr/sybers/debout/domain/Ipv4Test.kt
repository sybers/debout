package fr.sybers.debout.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ipv4Test {

    @Test
    fun `parses a dotted-quad literal`() {
        val address = Ipv4.parse(" 192.168.1.42 ")

        assertArrayEquals(byteArrayOf(192.toByte(), 168.toByte(), 1, 42), address?.address)
    }

    @Test
    fun `rejects anything that is not four decimal octets`() {
        listOf("", "192.168.1", "192.168.1.42.7", "256.1.1.1", "pc-salon", "1.2.3.+4", "1..2.3", "1.2.3.0042")
            .forEach { assertNull(it, Ipv4.parse(it)) }
    }

    private fun ipv4(literal: String) = Ipv4.parse(literal)!!

    @Test
    fun `addresses of the same slash 24 share a subnet`() {
        assertTrue(Ipv4.sameSubnet(ipv4("192.168.1.42"), ipv4("192.168.1.7"), 24))
        assertFalse(Ipv4.sameSubnet(ipv4("192.168.1.42"), ipv4("192.168.2.42"), 24))
    }

    @Test
    fun `subnet comparison honours prefixes not aligned on a byte`() {
        assertTrue(Ipv4.sameSubnet(ipv4("10.0.4.1"), ipv4("10.0.7.254"), 22))
        assertFalse(Ipv4.sameSubnet(ipv4("10.0.4.1"), ipv4("10.0.8.1"), 22))
    }

    @Test
    fun `slash 0 holds everything and slash 32 only itself`() {
        assertTrue(Ipv4.sameSubnet(ipv4("10.0.0.1"), ipv4("192.168.1.1"), 0))
        assertTrue(Ipv4.sameSubnet(ipv4("10.0.0.1"), ipv4("10.0.0.1"), 32))
        assertFalse(Ipv4.sameSubnet(ipv4("10.0.0.1"), ipv4("10.0.0.2"), 32))
    }
}
