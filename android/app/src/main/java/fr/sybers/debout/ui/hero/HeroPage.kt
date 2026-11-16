package fr.sybers.debout.ui.hero

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import fr.sybers.debout.ui.wake.WakeStateHaptics
import fr.sybers.debout.ui.wake.acceptsHold
import fr.sybers.debout.ui.wake.breathing
import fr.sybers.debout.ui.wake.heroWord
import fr.sybers.debout.ui.wake.isChecking
import fr.sybers.debout.ui.wake.rememberHoldToWake
import fr.sybers.debout.ui.wake.rememberLocalNetworkAccess
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark

/** Text colour over the hero background of [state]. */
fun heroForeground(state: WakeState): Color = if (state == WakeState.Online) HeroColors.OnEmber else HeroColors.OnNight

/** One machine, full screen: the whole page is the wake button. */
@Composable
fun HeroPage(
    machine: Machine,
    slot: QuickAccessSlot?,
    status: WakeStatus,
    localNetwork: LocalNetwork?,
    onWake: () -> WakeRequestResult,
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
    val hold = rememberHoldToWake(enabled = state.acceptsHold) {
        withLocalNetwork {
            if (onWake() == WakeRequestResult.AlreadyOnline) notice = R.string.already_on
        }
    }
    WakeStateHaptics(state)

    val breath = if (state == WakeState.Booting) breathing() else 0f
    val background by animateColorAsState(
        when (state) {
            WakeState.Unknown -> HeroColors.OffNetwork
            WakeState.Offline -> HeroColors.Asleep
            WakeState.Sending, WakeState.Booting -> lerp(HeroColors.Smoulder, HeroColors.EmberDeep, 0.25f + 0.25f * breath)
            WakeState.Online -> HeroColors.Ember
            WakeState.NoResponse -> HeroColors.Ash
        },
        tween(700),
        label = "heroBackground",
    )
    val foreground = heroForeground(state)

    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .then(hold.modifier),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            with(hold) { drawFlood(HeroColors.EmberDeep.copy(alpha = 0.9f)) }
        }
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (slot != null) {
                QuickAccessBadge(slot, size = 40.dp, outline = foreground.copy(alpha = 0.6f))
                Spacer(Modifier.height(16.dp))
            }
            Text(
                machine.name.uppercase(),
                color = foreground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp,
            )
            Text(
                stringResource(state.heroWord(onLocalNetwork)),
                color = foreground,
                fontSize = 72.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                notice?.let { stringResource(it) } ?: hint(status, onLocalNetwork),
                color = foreground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium,
            )
            if (localNetwork != null && machine.seemsOutside(localNetwork)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.hero_other_network),
                    color = foreground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
