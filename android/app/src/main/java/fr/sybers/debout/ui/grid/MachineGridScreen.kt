package fr.sybers.debout.ui.grid

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.QuickAccessSlot
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.ui.machine.QuickAccessBadge
import fr.sybers.debout.ui.machine.icon
import fr.sybers.debout.ui.wake.statusLabel
import fr.sybers.debout.ui.wake.watchedStatus
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/** Overview of every machine: tap one to show it in the carousel, long-press to move it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineGridScreen(
    viewModel: MachineGridViewModel,
    wakeCoordinator: WakeCoordinator,
    onBack: () -> Unit,
    onAddMachine: () -> Unit,
    onMachineSelected: () -> Unit,
) {
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val localNetwork by wakeCoordinator.localNetwork.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.grid_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddMachine) {
                        Icon(painterResource(R.drawable.ic_add), stringResource(R.string.home_add_machine))
                    }
                },
            )
        },
    ) { padding ->
        val saved = machines ?: return@Scaffold
        // Local copy so cards move under the finger; the database order follows on drop.
        var ordered by remember(saved) { mutableStateOf(saved) }
        val gridState = rememberLazyGridState()
        val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
            ordered = ordered.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(ordered, key = { _, machine -> machine.id }) { index, machine ->
                ReorderableItem(reorderState, key = machine.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 1.dp, label = "cardElevation")
                    val status by wakeCoordinator.watchedStatus(machine.id)
                    MachineCard(
                        machine = machine,
                        slot = QuickAccessSlot.at(index),
                        state = status.state,
                        onLocalNetwork = localNetwork != null,
                        onClick = {
                            viewModel.select(machine.id)
                            onMachineSelected()
                        },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                            onDragStopped = { viewModel.reorder(ordered.map { it.id }) },
                        ),
                        elevation = elevation,
                    )
                }
            }
        }
    }
}

@Composable
private fun MachineCard(
    machine: Machine,
    slot: QuickAccessSlot?,
    state: WakeState,
    onLocalNetwork: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    elevation: Dp,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(machine.type.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (slot != null) QuickAccessBadge(slot, size = 20.dp)
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
