package fr.sybers.debout.domain

import java.net.Inet4Address
import java.net.InetAddress

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
}
