package fr.sybers.debout.domain

import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer

object Ipv4 {
    private val octet = Regex("""\d{1,3}""")

    fun parse(input: String): Inet4Address? {
        val parts = input.trim().split('.')
        if (parts.size != 4 || parts.any { !octet.matches(it) }) return null
        val values = parts.map { it.toInt() }
        if (values.any { it > 255 }) return null
        // getByAddress never resolves a hostname, unlike getByName.
        return InetAddress.getByAddress(ByteArray(4) { values[it].toByte() }) as Inet4Address
    }

    fun sameSubnet(a: Inet4Address, b: Inet4Address, prefixLength: Int): Boolean {
        require(prefixLength in 0..32) { "IPv4 prefix length must be in 0..32, was $prefixLength" }
        // The JVM masks shift distances to 5 bits, so `-1 shl 32` would be -1 instead of 0.
        val networkMask = if (prefixLength == 0) 0 else -1 shl (32 - prefixLength)
        return (a.asInt() and networkMask) == (b.asInt() and networkMask)
    }

    private fun Inet4Address.asInt(): Int = ByteBuffer.wrap(address).int
}
