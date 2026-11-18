package fr.sybers.debout.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.ui.theme.SystemBarIcons
import fr.sybers.debout.ui.theme.HeroColors
import fr.sybers.debout.ui.theme.Grain
import fr.sybers.debout.ui.character.PillEyes
import fr.sybers.debout.ui.character.IdleMotion
import fr.sybers.debout.ui.character.Expression
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.ui.hero.MachineCarousel

/** No machine: guide to pairing. Otherwise: the full-screen carousel. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    wakeCoordinator: WakeCoordinator,
    wakeRequest: Long?,
    onWakeRequestHandled: () -> Unit,
    onAddMachine: () -> Unit,
    onScanQrCode: () -> Unit,
    onOpenGrid: () -> Unit,
    onEditMachine: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: (Machine) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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

    val home = state ?: return
    Box(Modifier.fillMaxSize()) {
        if (home.machines.isEmpty()) {
            EmptyState(onScanQrCode = onScanQrCode, onAddMachine = onAddMachine)
        } else {
            MachineCarousel(
                machines = home.machines,
                currentMachineId = home.currentMachineId,
                wakeCoordinator = wakeCoordinator,
                wakeRequest = wakeRequest,
                onWakeRequestHandled = onWakeRequestHandled,
                onMachineViewed = viewModel::onMachineViewed,
                onOpenGrid = onOpenGrid,
                onAddMachine = onAddMachine,
                onEditMachine = onEditMachine,
                onOpenSettings = onOpenSettings,
                onOpenHelp = onOpenHelp,
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}

@Composable
private fun EmptyState(onScanQrCode: () -> Unit, onAddMachine: () -> Unit) {
    val palette = HeroColors.Asleep
    SystemBarIcons(dark = true)
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Grain(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            PillEyes(
                Expression.Asleep,
                palette.ink,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.4f),
                idleMotion = IdleMotion.Expectant,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onScanQrCode,
                colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.background),
            ) {
                Text(stringResource(R.string.home_scan_qr))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.home_scan_hint),
                color = palette.ink,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAddMachine, colors = ButtonDefaults.textButtonColors(contentColor = palette.ink)) {
                Text(stringResource(R.string.home_add_manually))
            }
        }
    }
}
