package fr.sybers.debout

import android.content.Context
import android.net.ConnectivityManager
import fr.sybers.debout.data.DeboutDatabase
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.network.LocalNetworkMonitor
import fr.sybers.debout.network.UdpMagicPacketSender
import fr.sybers.debout.network.WakeCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val database = DeboutDatabase.create(context)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val machines = MachineRepository(database.machineDao())
    val localNetworkMonitor = LocalNetworkMonitor(context.getSystemService(ConnectivityManager::class.java))
    val wakeCoordinator = WakeCoordinator(machines, localNetworkMonitor, UdpMagicPacketSender(), applicationScope)
}
