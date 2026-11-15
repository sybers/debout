package fr.sybers.debout.domain

import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer

object Broadcast {
    fun of(ip: Inet4Address, prefixLength: Int): Inet4Address {
        require(prefixLength in 0..32) { "IPv4 prefix length must be in 0..32, was $prefixLength" }
        val address = ByteBuffer.wrap(ip.address).int
        // The JVM masks shift distances to 5 bits, so `-1 ushr 32` would be -1 instead of 0.
        val hostMask = if (prefixLength == 32) 0 else -1 ushr prefixLength
        val broadcast = ByteBuffer.allocate(4).putInt(address or hostMask).array()
        return InetAddress.getByAddress(broadcast) as Inet4Address
    }
}
