package fr.sybers.debout.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MagicPacketTest {

    @Test
    fun `is six FF bytes followed by sixteen repetitions of the MAC`() {
        val macBytes = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0xFF.toByte())
        val expected = ByteArray(6) { 0xFF.toByte() } + List(16) { macBytes }.reduce(ByteArray::plus)

        val packet = MagicPacket.of(MacAddress(0x0011223344FF))

        assertArrayEquals(expected, packet)
    }
}
