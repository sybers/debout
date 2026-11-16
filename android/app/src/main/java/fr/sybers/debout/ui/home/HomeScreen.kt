package fr.sybers.debout.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.ui.hero.HeroScreen
import fr.sybers.debout.ui.wake.watchedStatus

/** No machine: guide to pairing. One machine: its hero view. More: a grid of cards. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    wakeCoordinator: WakeCoordinator,
    onAddMachine: () -> Unit,
    onOpenMachine: (Long) -> Unit,
    onEditMachine: (Long) -> Unit,
) {
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    recentlyDeleted?.let { deleted ->
        val message = stringResource(R.string.home_machine_deleted, deleted.name)
        val undo = stringResource(R.string.undo)
        LaunchedEffect(deleted) {
            val result = snackbarHostState.showSnackbar(message, undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.dismissDeleted()
        }
    }

    val list = machines ?: return
    when (list.size) {
        0 -> Box(Modifier.fillMaxSize()) {
            EmptyState(onAddMachine)
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
        1 -> Box(Modifier.fillMaxSize()) {
            val machine = list.single()
            val status by wakeCoordinator.watchedStatus(machine.id)
            val localNetwork by wakeCoordinator.localNetwork.collectAsStateWithLifecycle()
            HeroScreen(
                machine = machine,
                status = status,
                localNetwork = localNetwork,
                onWake = { wakeCoordinator.wake(machine.id) },
                onEdit = { onEditMachine(machine.id) },
                onAddMachine = onAddMachine,
            )
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
        else -> MachineGridScreen(
            machines = list,
            wakeCoordinator = wakeCoordinator,
            snackbarHostState = snackbarHostState,
            onAddMachine = onAddMachine,
            onOpenMachine = onOpenMachine,
        )
    }
}

@Composable
private fun EmptyState(onAddMachine: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(
                painterResource(R.drawable.ic_power),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            // TODO: enable once QR pairing exists (plan step 8).
            Button(onClick = {}, enabled = false) { Text(stringResource(R.string.home_scan_qr)) }
            Text(
                stringResource(R.string.home_scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onAddMachine) { Text(stringResource(R.string.home_add_manually)) }
        }
    }
}
