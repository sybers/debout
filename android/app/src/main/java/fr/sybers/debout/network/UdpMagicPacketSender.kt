package fr.sybers.debout.network

import fr.sybers.debout.domain.MacAddress
import fr.sybers.debout.domain.MagicPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address

class UdpMagicPacketSender {

    suspend fun send(mac: MacAddress, target: Inet4Address, port: Int, via: LocalNetwork) =
        withContext(Dispatchers.IO) {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                // Without binding, Android may route the packet over mobile data.
                via.network.bindSocket(socket)
                val packet = MagicPacket.of(mac)
                socket.send(DatagramPacket(packet, packet.size, target, port))
            }
        }
}
