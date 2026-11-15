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
import fr.sybers.debout.ui.hero.HeroScreen
import fr.sybers.debout.ui.home.HomeScreen
import fr.sybers.debout.ui.home.HomeViewModel
import fr.sybers.debout.ui.machine.MachineEditScreen
import fr.sybers.debout.ui.machine.MachineEditViewModel

private object Routes {
    const val HOME = "home"
    const val ADD = "machines/new"
    const val HERO = "machines/{id}"
    const val EDIT = "machines/{id}/edit"
    fun hero(id: Long) = "machines/$id"
    fun edit(id: Long) = "machines/$id/edit"
}

private val idArgument = listOf(navArgument("id") { type = NavType.LongType })

/** Set on the home entry by the edit screen, so home deletes and offers the undo. */
private const val DELETE_REQUEST = "delete_request"

@Composable
fun DeboutApp() {
    val container = LocalContext.current.appContainer
    val navController = rememberNavController()

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { entry ->
            val viewModel = viewModel { HomeViewModel(container.machines) }
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
                onAddMachine = { navController.navigate(Routes.ADD) },
                onOpenMachine = { navController.navigate(Routes.hero(it)) },
                onEditMachine = { navController.navigate(Routes.edit(it)) },
            )
        }
        composable(Routes.HERO, arguments = idArgument) { entry ->
            val id = entry.arguments!!.getLong("id")
            val machines by container.machines.machines.collectAsStateWithLifecycle(initialValue = null)
            val machine = machines?.find { it.id == id } ?: return@composable
            val status by container.wakeCoordinator.status(id).collectAsStateWithLifecycle()
            HeroScreen(
                machine = machine,
                status = status,
                onWake = { container.wakeCoordinator.wake(id) },
                onEdit = { navController.navigate(Routes.edit(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ADD) {
            MachineEditScreen(
                viewModel = viewModel { MachineEditViewModel(container.machines, null, container.localNetworkMonitor.localNetwork) },
                onBack = { navController.popBackStack() },
                onDelete = {},
            )
        }
        composable(Routes.EDIT, arguments = idArgument) { entry ->
            val id = entry.arguments!!.getLong("id")
            MachineEditScreen(
                viewModel = viewModel { MachineEditViewModel(container.machines, id, container.localNetworkMonitor.localNetwork) },
                onBack = { navController.popBackStack() },
                onDelete = {
                    navController.getBackStackEntry(Routes.HOME).savedStateHandle[DELETE_REQUEST] = id
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
    }
}
