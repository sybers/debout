package fr.sybers.debout.ui

import android.net.Uri
import android.widget.Toast
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
import fr.sybers.debout.R
import fr.sybers.debout.appContainer
import fr.sybers.debout.domain.PairingParseResult
import fr.sybers.debout.domain.PairingPayload
import fr.sybers.debout.ui.grid.MachineGridScreen
import fr.sybers.debout.ui.grid.MachineGridViewModel
import fr.sybers.debout.ui.help.HelpOs
import fr.sybers.debout.ui.help.TroubleshootingScreen
import fr.sybers.debout.ui.home.HomeScreen
import fr.sybers.debout.ui.home.HomeViewModel
import fr.sybers.debout.ui.machine.MachineEditScreen
import fr.sybers.debout.ui.machine.MachineEditViewModel
import fr.sybers.debout.ui.pairing.PairingScanScreen
import fr.sybers.debout.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val GRID = "machines"
    const val ADD = "machines/new?pairing={pairing}"
    const val EDIT = "machines/{id}/edit"
    const val SETTINGS = "settings"
    const val SCAN = "scan"
    const val HELP = "help?os={os}"
    fun help(os: HelpOs) = "help?os=${os.anchor}"
    fun add(pairing: String? = null) = if (pairing == null) "machines/new" else "machines/new?pairing=${Uri.encode(pairing)}"
    fun edit(id: Long) = "machines/$id/edit"
}

/** Set on the home entry by the edit screen, so home deletes and offers the undo. */
private const val DELETE_REQUEST = "delete_request"

/**
 * [wakeRequest] is a machine to show and wake, from an app shortcut or a tile.
 * [pairingRequest] is a pairing payload opened from outside the app, such as the system camera.
 */
@Composable
fun DeboutApp(
    wakeRequest: Long?,
    onWakeRequestHandled: () -> Unit,
    pairingRequest: String?,
    onPairingRequestHandled: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val navController = rememberNavController()

    LaunchedEffect(wakeRequest) {
        if (wakeRequest != null) navController.popBackStack(Routes.HOME, inclusive = false)
    }

    LaunchedEffect(pairingRequest) {
        if (pairingRequest == null) return@LaunchedEffect
        when (PairingPayload.parse(pairingRequest)) {
            is PairingParseResult.Valid -> navController.navigate(Routes.add(pairingRequest)) {
                popUpTo(Routes.HOME)
            }
            PairingParseResult.UnsupportedVersion -> Toast.makeText(context, R.string.scan_update_app, Toast.LENGTH_LONG).show()
            PairingParseResult.Invalid -> Toast.makeText(context, R.string.scan_not_debout, Toast.LENGTH_LONG).show()
        }
        onPairingRequestHandled()
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
                onAddMachine = { navController.navigate(Routes.add()) },
                onScanQrCode = { navController.navigate(Routes.SCAN) },
                onOpenGrid = { navController.navigate(Routes.GRID) },
                onEditMachine = { navController.navigate(Routes.edit(it)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHelp = { navController.navigate(Routes.help(HelpOs.likelyFor(it.type))) },
            )
        }
        composable(Routes.GRID) {
            MachineGridScreen(
                viewModel = viewModel { MachineGridViewModel(container.machines, container.preferences) },
                wakeCoordinator = container.wakeCoordinator,
                onBack = { navController.popBackStack() },
                onAddMachine = { navController.navigate(Routes.add()) },
                onMachineSelected = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.SCAN) {
            PairingScanScreen(
                onBack = { navController.popBackStack() },
                onPaired = { payload ->
                    navController.navigate(Routes.add(payload)) { popUpTo(Routes.HOME) }
                },
            )
        }
        composable(
            Routes.ADD,
            arguments = listOf(navArgument("pairing") { type = NavType.StringType; nullable = true }),
        ) { entry ->
            val pairing = entry.arguments?.getString("pairing")
                ?.let { PairingPayload.parse(it) as? PairingParseResult.Valid }
                ?.payload
            MachineEditScreen(
                viewModel = viewModel {
                    MachineEditViewModel(container.machines, null, container.localNetworkMonitor.localNetwork, pairing)
                },
                onBack = { navController.popBackStack() },
                onSaved = { showMachine(it) },
                onDelete = {},
                onScanQrCode = { navController.navigate(Routes.SCAN) }.takeIf { pairing == null },
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
        composable(Routes.HELP, arguments = listOf(navArgument("os") { type = NavType.StringType; nullable = true })) { entry ->
            TroubleshootingScreen(
                initialOs = HelpOs.fromAnchor(entry.arguments?.getString("os")),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(preferences = container.preferences, onBack = { navController.popBackStack() })
        }
    }
}
