package fr.sybers.debout.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.WakeRequestResult
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.ui.machine.icon
import fr.sybers.debout.ui.theme.HeroColors
import fr.sybers.debout.ui.wake.WakeStateHaptics
import fr.sybers.debout.ui.wake.acceptsHold
import fr.sybers.debout.ui.wake.breathing
import fr.sybers.debout.ui.wake.rememberHoldToWake
import fr.sybers.debout.ui.wake.rememberLocalNetworkAccess
import fr.sybers.debout.ui.wake.statusLabel
import fr.sybers.debout.ui.wake.watchedStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineGridScreen(
    machines: List<Machine>,
    wakeCoordinator: WakeCoordinator,
    snackbarHostState: SnackbarHostState,
    onAddMachine: () -> Unit,
    onOpenMachine: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val localNetwork by wakeCoordinator.localNetwork.collectAsStateWithLifecycle()
    val alreadyOn = stringResource(R.string.already_on)
    val denied = stringResource(R.string.local_network_denied)
    val withLocalNetwork = rememberLocalNetworkAccess(
        onDenied = { scope.launch { snackbarHostState.showSnackbar(denied, withDismissAction = true) } },
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMachine) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.home_add_machine))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(machines, key = { it.id }) { machine ->
                val status by wakeCoordinator.watchedStatus(machine.id)
                MachineCard(
                    machine = machine,
                    state = status.state,
                    onLocalNetwork = localNetwork != null,
                    onOpen = { onOpenMachine(machine.id) },
                    onWake = {
                        withLocalNetwork {
                            if (wakeCoordinator.wake(machine.id) == WakeRequestResult.AlreadyOnline) {
                                scope.launch { snackbarHostState.showSnackbar(alreadyOn) }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MachineCard(
    machine: Machine,
    state: WakeState,
    onLocalNetwork: Boolean,
    onOpen: () -> Unit,
    onWake: () -> Unit,
) {
    WakeStateHaptics(state)
    ElevatedCard(onClick = onOpen) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painterResource(machine.type.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (machine.favorite) {
                    Icon(
                        painterResource(R.drawable.ic_star),
                        contentDescription = stringResource(R.string.home_favorite),
                        modifier = Modifier.padding(start = 4.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.weight(1f))
                CompactWakeButton(state, onWake)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                machine.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(state.statusLabel(onLocalNetwork)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state == WakeState.Online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactWakeButton(state: WakeState, onWake: () -> Unit) {
    val hold = rememberHoldToWake(enabled = state.acceptsHold, onWake = onWake)
    val breath = if (state == WakeState.Booting) breathing() else 0f
    val container = when (state) {
        WakeState.Online -> HeroColors.Ember
        WakeState.Sending, WakeState.Booting -> HeroColors.EmberDeep.copy(alpha = 0.35f + 0.25f * breath)
        WakeState.Unknown -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        WakeState.Offline, WakeState.NoResponse -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (state) {
        WakeState.Online -> HeroColors.OnEmber
        WakeState.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(container)
            .then(hold.modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            with(hold) { drawFlood(HeroColors.EmberDeep) }
        }
        Icon(painterResource(R.drawable.ic_power), stringResource(R.string.action_wake), tint = content)
    }
}
