package fr.sybers.debout.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import fr.sybers.debout.MainActivity
import fr.sybers.debout.R
import fr.sybers.debout.WakeIntent
import fr.sybers.debout.appContainer
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.QuickAccessSlot
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.hasLocalNetworkAccess
import fr.sybers.debout.ui.machine.tileIcon
import fr.sybers.debout.ui.wake.statusLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FirstMachineTileService : QuickAccessTileService(QuickAccessSlot.First)

class SecondMachineTileService : QuickAccessTileService(QuickAccessSlot.Second)

class ThirdMachineTileService : QuickAccessTileService(QuickAccessSlot.Third)

/** Wakes the machine at [slot] in the list on a tap, and probes it while the tile is visible. */
abstract class QuickAccessTileService(private val slot: QuickAccessSlot) : TileService() {
    private val scope = MainScope()
    private var listening: Job? = null
    private var machine: Machine? = null

    override fun onStartListening() {
        val container = appContainer
        listening = scope.launch {
            container.machines.machines
                .map { it.getOrNull(slot.ordinal) }
                .distinctUntilChanged { old, new -> old?.id == new?.id && old?.name == new?.name }
                .collectLatest { machine ->
                    this@QuickAccessTileService.machine = machine
                    if (machine == null) {
                        showEmpty()
                        return@collectLatest
                    }
                    val coordinator = container.wakeCoordinator
                    combine(coordinator.watch(machine.id), coordinator.localNetwork) { status, network ->
                        status.state to (network != null)
                    }.collect { (state, onLocalNetwork) -> show(machine, state, onLocalNetwork) }
                }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        val machine = machine ?: return openApp(Intent(this, MainActivity::class.java))
        // A service cannot ask for a permission: the app asks, then wakes.
        if (!hasLocalNetworkAccess()) return openApp(WakeIntent.create(this, machine.id))
        val container = appContainer
        // The application scope, so the wake outlives this tile once the panel closes.
        container.applicationScope.launch { container.wakeCoordinator.wake(machine.id) }
    }

    private fun showEmpty() = update(
        label = getString(slot.label),
        subtitle = getString(R.string.tile_no_machine),
        state = Tile.STATE_INACTIVE,
    )

    private fun show(machine: Machine, wakeState: WakeState, onLocalNetwork: Boolean) = update(
        label = machine.name,
        subtitle = getString(wakeState.statusLabel(onLocalNetwork)),
        state = when (wakeState) {
            WakeState.Sending, WakeState.Booting, WakeState.Online -> Tile.STATE_ACTIVE
            WakeState.Unknown -> if (onLocalNetwork) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            WakeState.Offline, WakeState.NoResponse -> Tile.STATE_INACTIVE
        },
    )

    private fun update(label: String, subtitle: String, state: Int) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, slot.tileIcon)
        tile.label = label
        tile.subtitle = subtitle
        tile.state = state
        tile.updateTile()
    }

    private fun openApp(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            startActivityAndCollapse(PendingIntent.getActivity(this, slot.ordinal, intent, flags))
        } else {
            // Lint ignores the SDK check above: the PendingIntent overload only exists from API 34.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private val QuickAccessSlot.label: Int
        get() = when (this) {
            QuickAccessSlot.First -> R.string.tile_first
            QuickAccessSlot.Second -> R.string.tile_second
            QuickAccessSlot.Third -> R.string.tile_third
        }
}
