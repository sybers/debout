package fr.sybers.debout.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
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
}
