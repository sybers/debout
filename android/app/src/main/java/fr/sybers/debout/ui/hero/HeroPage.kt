package fr.sybers.debout.ui.hero

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.sybers.debout.R
import fr.sybers.debout.domain.Ipv4
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.QuickAccessSlot
import fr.sybers.debout.domain.WakeRequestResult
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.LocalNetwork
import fr.sybers.debout.network.WakeStatus
import fr.sybers.debout.ui.machine.QuickAccessBadge
import fr.sybers.debout.ui.theme.HeroColors
import fr.sybers.debout.ui.theme.HeroPalette
import fr.sybers.debout.ui.theme.Unbounded
import fr.sybers.debout.ui.wake.WakeStateHaptics
import fr.sybers.debout.ui.wake.acceptsHold
import fr.sybers.debout.ui.wake.breathing
import fr.sybers.debout.ui.wake.heroWord
import fr.sybers.debout.ui.wake.isChecking
import fr.sybers.debout.ui.wake.rememberHoldToWake
import fr.sybers.debout.ui.wake.rememberLocalNetworkAccess
import fr.sybers.debout.ui.wake.rememberWakeNotificationsPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark

fun heroPalette(state: WakeState, onLocalNetwork: Boolean): HeroPalette = when (state) {
    WakeState.Unknown -> if (onLocalNetwork) HeroColors.Checking else HeroColors.OffNetwork
    WakeState.Offline -> HeroColors.Asleep
    WakeState.Sending -> HeroColors.Hey
    WakeState.Booting -> HeroColors.Waking
    WakeState.Online -> HeroColors.Awake
    WakeState.NoResponse -> HeroColors.Silent
}

/** The state word gets heavier as the machine wakes. */
private fun heroWordWeight(state: WakeState): FontWeight = when (state) {
    WakeState.Offline, WakeState.NoResponse -> FontWeight.Light
    WakeState.Unknown -> FontWeight.Normal
    WakeState.Booting -> FontWeight.Medium
    WakeState.Sending, WakeState.Online -> FontWeight.Black
}

/** One machine, full screen: the whole page is the wake button. */
@Composable
fun HeroPage(
    machine: Machine,
    slot: QuickAccessSlot?,
    status: WakeStatus,
    localNetwork: LocalNetwork?,
    onWake: suspend () -> WakeRequestResult,
    wakeRequested: Boolean,
    onWakeRequestHandled: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val state = status.state
    val onLocalNetwork = localNetwork != null
    var notice by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(4.seconds)
            notice = null
        }
    }
    val withLocalNetwork = rememberLocalNetworkAccess(onDenied = { notice = R.string.local_network_denied })
    val askForNotifications = rememberWakeNotificationsPermission()
    val scope = rememberCoroutineScope()
    val wake = {
        withLocalNetwork {
            scope.launch {
                when (onWake()) {
                    WakeRequestResult.Started -> askForNotifications()
                    WakeRequestResult.AlreadyOnline -> notice = R.string.already_on
                    WakeRequestResult.NotAllowed -> Unit
                }
            }
        }
    }
    val hold = rememberHoldToWake(enabled = state.acceptsHold, onWake = wake)
    LaunchedEffect(wakeRequested) {
        if (wakeRequested) {
            onWakeRequestHandled()
            wake()
        }
    }
    WakeStateHaptics(state)

    val palette = heroPalette(state, onLocalNetwork)
    val breath = if (state == WakeState.Booting) breathing() else 0f
    val background by animateColorAsState(
        lerp(palette.background, palette.ink, 0.08f * breath),
        tween(700),
        label = "heroBackground",
    )
    val foreground by animateColorAsState(palette.ink, tween(700), label = "heroInk")

    // The help button sits beside the hold zone, not inside: the zone merges its children's semantics.
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(background)
                .then(hold.modifier),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                with(hold) { drawFlood(HeroColors.Waking.background) }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                if (slot != null) {
                    QuickAccessBadge(slot, size = 40.dp, outline = foreground)
                    Spacer(Modifier.height(16.dp))
                }
                // Every text uses the full ink: each pair is tuned for 4.5:1, which transparency would break.
                Text(
                    machine.name.uppercase(),
                    color = foreground,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 3.sp,
                )
                BasicText(
                    stringResource(state.heroWord(onLocalNetwork)),
                    color = { foreground },
                    style = TextStyle(
                        fontFamily = Unbounded,
                        fontWeight = heroWordWeight(state),
                        lineHeight = 1.05.em,
                        letterSpacing = (-0.02).em,
                    ),
                    autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 64.sp),
                    maxLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    notice?.let { stringResource(it) } ?: hint(status, onLocalNetwork),
                    color = foreground,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (localNetwork != null && machine.seemsOutside(localNetwork)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.hero_other_network),
                        color = foreground,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (state == WakeState.NoResponse) {
            TextButton(
                onClick = onOpenHelp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 56.dp),
            ) {
                Text(stringResource(R.string.hero_help), color = foreground, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun hint(status: WakeStatus, onLocalNetwork: Boolean): String = when (status.state) {
    WakeState.Unknown ->
        stringResource(if (status.state.isChecking(onLocalNetwork)) R.string.hero_hint_checking else R.string.hero_hint_not_local)
    WakeState.Offline -> stringResource(R.string.hero_hint_hold)
    WakeState.Sending -> stringResource(R.string.hero_hint_sending)
    WakeState.Booting -> stringResource(R.string.hero_hint_elapsed, elapsedSeconds(status.since))
    WakeState.Online -> stringResource(R.string.hero_hint_online)
    WakeState.NoResponse -> stringResource(R.string.hero_hint_retry)
}

private fun Machine.seemsOutside(network: LocalNetwork): Boolean {
    val address = ip?.let(Ipv4::parse) ?: return false
    return !Ipv4.sameSubnet(address, network.address, network.prefixLength)
}

@Composable
private fun elapsedSeconds(since: TimeMark): Long {
    val seconds by produceState(since.elapsedNow().inWholeSeconds, since) {
        while (true) {
            value = since.elapsedNow().inWholeSeconds
            delay(250)
        }
    }
    return seconds
}
