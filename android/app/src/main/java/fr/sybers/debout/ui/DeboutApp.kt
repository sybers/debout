package fr.sybers.debout.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.sybers.debout.appContainer
import fr.sybers.debout.ui.grid.MachineGridScreen
import fr.sybers.debout.ui.grid.MachineGridViewModel
import fr.sybers.debout.ui.home.HomeScreen
import fr.sybers.debout.ui.home.HomeViewModel
import fr.sybers.debout.ui.machine.MachineEditScreen
import fr.sybers.debout.ui.machine.MachineEditViewModel

private object Routes {
    const val HOME = "home"
    const val GRID = "machines"
    const val ADD = "machines/new"
    const val EDIT = "machines/{id}/edit"
    fun edit(id: Long) = "machines/$id/edit"
}

/** Set on the home entry by the edit screen, so home deletes and offers the undo. */
private const val DELETE_REQUEST = "delete_request"

/** [wakeRequest] is a machine to show and wake, from an app shortcut or a tile. */
@Composable
fun DeboutApp(wakeRequest: Long?, onWakeRequestHandled: () -> Unit) {
    val container = LocalContext.current.appContainer
    val navController = rememberNavController()

    LaunchedEffect(wakeRequest) {
        if (wakeRequest != null) navController.popBackStack(Routes.HOME, inclusive = false)
    }

    // Home shows the last viewed machine, so pointing it at one and returning is enough.
    val showMachine = { id: Long ->
        container.preferences.setLastViewedMachine(id)
        navController.popBackStack(Routes.HOME, inclusive = false)
    }

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { entry ->
            val viewModel = viewModel { HomeViewModel(container.machines, container.preferences) }
            val deleteRequest by entry.savedStateHandle
                .getStateFlow<Long?>(DELETE_REQUEST, null)
                .collectAsStateWithLifecycle()
            LaunchedEffect(deleteRequest) {
                deleteRequest?.let {
                    viewModel.delete(it)
                    entry.savedStateHandle[DELETE_REQUEST] = null
                }
            }
            HomeScreen(
                viewModel = viewModel,
                wakeCoordinator = container.wakeCoordinator,
                wakeRequest = wakeRequest,
                onWakeRequestHandled = onWakeRequestHandled,
                onAddMachine = { navController.navigate(Routes.ADD) },
                onOpenGrid = { navController.navigate(Routes.GRID) },
                onEditMachine = { navController.navigate(Routes.edit(it)) },
            )
        }
        composable(Routes.GRID) {
            MachineGridScreen(
                viewModel = viewModel { MachineGridViewModel(container.machines, container.preferences) },
                wakeCoordinator = container.wakeCoordinator,
                onBack = { navController.popBackStack() },
                onAddMachine = { navController.navigate(Routes.ADD) },
                onMachineSelected = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.ADD) {
            MachineEditScreen(
                viewModel = viewModel { MachineEditViewModel(container.machines, null, container.localNetworkMonitor.localNetwork) },
                onBack = { navController.popBackStack() },
                onSaved = { showMachine(it) },
                onDelete = {},
            )
        }
        composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            MachineEditScreen(
                viewModel = viewModel { MachineEditViewModel(container.machines, id, container.localNetworkMonitor.localNetwork) },
                onBack = { navController.popBackStack() },
                onSaved = { showMachine(it) },
                onDelete = {
                    navController.getBackStackEntry(Routes.HOME).savedStateHandle[DELETE_REQUEST] = id
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
    }
}
