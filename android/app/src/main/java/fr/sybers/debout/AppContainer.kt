package fr.sybers.debout

import android.content.Context
import android.net.ConnectivityManager
import fr.sybers.debout.data.DeboutDatabase
import fr.sybers.debout.data.MachineRepository
import fr.sybers.debout.data.UserPreferences
import fr.sybers.debout.network.LocalNetworkMonitor
import fr.sybers.debout.network.TcpProbe
import fr.sybers.debout.network.UdpMagicPacketSender
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.network.hasLocalNetworkAccess
import fr.sybers.debout.tile.QuickAccessShortcuts
import fr.sybers.debout.tracking.WakeTrackingWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database = DeboutDatabase.create(context)
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val machines = MachineRepository(database.machineDao())
    val preferences = UserPreferences(context, applicationScope)
    val localNetworkMonitor = LocalNetworkMonitor(context.getSystemService(ConnectivityManager::class.java))
    val wakeCoordinator = WakeCoordinator(
        machines = machines,
        localNetworkMonitor = localNetworkMonitor,
        packetSender = UdpMagicPacketSender(),
        probe = TcpProbe(),
        hasLocalNetworkAccess = { context.applicationContext.hasLocalNetworkAccess() },
        scope = applicationScope,
        onWakeStarted = { machineId ->
            applicationScope.launch {
                if (preferences.wakeNotificationsEnabled.first()) WakeTrackingWorker.enqueue(context, machineId)
            }
        },
    )

    init {
        applicationScope.launch { QuickAccessShortcuts(context).follow(machines.machines) }
    }
}
