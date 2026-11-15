package fr.sybers.debout

import android.content.Context
import android.net.ConnectivityManager
import fr.sybers.debout.data.DeboutDatabase
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.network.LocalNetworkMonitor
import fr.sybers.debout.network.UdpMagicPacketSender

class AppContainer(context: Context) {
    private val database = DeboutDatabase.create(context)

    val machines = MachineRepository(database.machineDao())
    val localNetworkMonitor = LocalNetworkMonitor(context.getSystemService(ConnectivityManager::class.java))
    val packetSender = UdpMagicPacketSender()
}
