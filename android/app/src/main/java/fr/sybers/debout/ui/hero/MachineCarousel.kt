package fr.sybers.debout.ui.hero

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.drop
import fr.sybers.debout.R
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.QuickAccessSlot
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.ui.wake.watchedStatus

/**
 * One full-screen page per machine, swiped horizontally. Opens on [currentMachineId] and
 * follows it when it changes from elsewhere (grid, add, delete, shortcut); reports each page
 * the user settles on through [onMachineViewed]. [wakeRequest] wakes its machine once the
 * carousel has settled on it.
 */
@Composable
fun MachineCarousel(
    machines: List<Machine>,
    currentMachineId: Long?,
    wakeCoordinator: WakeCoordinator,
    wakeRequest: Long?,
    onWakeRequestHandled: () -> Unit,
    onMachineViewed: (Long) -> Unit,
    onOpenGrid: () -> Unit,
    onAddMachine: () -> Unit,
    onEditMachine: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val currentMachines by rememberUpdatedState(machines)
    val currentOnMachineViewed by rememberUpdatedState(onMachineViewed)
    val targetPage = machines.indexOfFirst { it.id == currentMachineId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = targetPage) { currentMachines.size }

    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) pagerState.scrollToPage(targetPage)
    }
    LaunchedEffect(pagerState) {
        // Skipping the page shown on entry keeps a choice made elsewhere (grid, add) from being overwritten.
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            currentMachines.getOrNull(page)?.let { currentOnMachineViewed(it.id) }
        }
    }

    val localNetwork by wakeCoordinator.localNetwork.collectAsStateWithLifecycle()
    val current = machines[pagerState.currentPage.coerceIn(machines.indices)]
    val currentStatus by wakeCoordinator.watchedStatus(current.id)
    val foreground = heroForeground(currentStatus.state)
    StatusBarIcons(dark = currentStatus.state == WakeState.Online)

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            key = { machines[it].id },
            // Composing the neighbours probes them too, so their status is ready on arrival.
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val machine = machines[page]
            val status by wakeCoordinator.watchedStatus(machine.id)
            HeroPage(
                machine = machine,
                slot = QuickAccessSlot.at(page),
                status = status,
                localNetwork = localNetwork,
                onWake = { wakeCoordinator.wake(machine.id) },
                wakeRequested = machine.id == wakeRequest && pagerState.settledPage == page,
                onWakeRequestHandled = onWakeRequestHandled,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (machines.size > 1) {
                IconButton(onClick = onOpenGrid) {
                    Icon(painterResource(R.drawable.ic_grid), stringResource(R.string.open_grid), tint = foreground)
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAddMachine) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.home_add_machine), tint = foreground)
            }
            IconButton(onClick = { onEditMachine(current.id) }) {
                Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.edit_machine), tint = foreground)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings_title), tint = foreground)
            }
        }
        if (machines.size > 1) {
            PageDots(
                count = machines.size,
                current = pagerState.currentPage,
                color = foreground,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

/** The hero paints behind the system bars, so their icons follow the hero background, not the theme. */
@Composable
private fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(window, dark) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = dark
        controller.isAppearanceLightNavigationBars = dark
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}
