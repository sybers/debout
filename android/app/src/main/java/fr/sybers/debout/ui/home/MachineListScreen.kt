package fr.sybers.debout.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.ui.machine.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineListScreen(
    viewModel: MachineListViewModel,
    onAddMachine: () -> Unit,
    onOpenMachine: (Long) -> Unit,
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            if (!machines.isNullOrEmpty()) {
                FloatingActionButton(onClick = onAddMachine) {
                    Icon(painterResource(R.drawable.ic_add), stringResource(R.string.home_add_machine))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val list = machines ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(onAddMachine, Modifier.padding(padding))
        } else {
            LazyColumn(contentPadding = padding) {
                items(list, key = { it.id }) { machine ->
                    MachineRow(machine, onClick = { onOpenMachine(machine.id) })
                }
            }
        }
    }
}

@Composable
private fun MachineRow(machine: Machine, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(machine.name) },
        overlineContent = { Text(stringResource(machine.type.label)) },
        supportingContent = { Text(listOfNotNull(machine.mac.toString(), machine.ip).joinToString(" · ")) },
        trailingContent = {
            if (machine.favorite) {
                Icon(painterResource(R.drawable.ic_star), stringResource(R.string.home_favorite))
            }
        },
    )
}

@Composable
private fun EmptyState(onAddMachine: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(onClick = onAddMachine) { Text(stringResource(R.string.home_add_manually)) }
        }
    }
}
