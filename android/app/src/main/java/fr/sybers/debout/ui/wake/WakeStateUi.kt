package fr.sybers.debout.ui.wake

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sybers.debout.R
import fr.sybers.debout.domain.WakeState
import fr.sybers.debout.network.WakeCoordinator
import fr.sybers.debout.network.WakeStatus
import kotlinx.coroutines.delay

/** Online stays holdable so the user gets "Already on" rather than a dead button. */
val WakeState.acceptsHold: Boolean
    get() = this == WakeState.Offline || this == WakeState.NoResponse || this == WakeState.Online

/** Unknown on a local network only means the first probe has not answered yet. */
fun WakeState.isChecking(onLocalNetwork: Boolean): Boolean = this == WakeState.Unknown && onLocalNetwork

/** Probes the machine while the calling screen is visible. */
@Composable
fun WakeCoordinator.watchedStatus(machineId: Long): State<WakeStatus> =
    watch(machineId).collectAsStateWithLifecycle(initialValue = currentStatus(machineId))

@StringRes
fun WakeState.heroWord(onLocalNetwork: Boolean): Int =
    when (this) {
        WakeState.Unknown -> if (onLocalNetwork) R.string.hero_word_checking else R.string.hero_word_unknown
        WakeState.Offline -> R.string.hero_word_offline
        WakeState.Sending -> R.string.hero_word_sending
        WakeState.Booting -> R.string.hero_word_booting
        WakeState.Online -> R.string.hero_word_online
        WakeState.NoResponse -> R.string.hero_word_no_response
    }

@StringRes
fun WakeState.statusLabel(onLocalNetwork: Boolean): Int =
    when (this) {
        WakeState.Unknown -> if (onLocalNetwork) R.string.status_checking else R.string.status_unknown
        WakeState.Offline -> R.string.status_offline
        WakeState.Sending -> R.string.status_sending
        WakeState.Booting -> R.string.status_booting
        WakeState.Online -> R.string.status_online
        WakeState.NoResponse -> R.string.status_no_response
    }

/**
 * Double confirmation when a wake succeeds, rejection when it times out. A machine merely
 * found online or offline by the probe stays silent.
 */
@Composable
fun WakeStateHaptics(state: WakeState) {
    val haptics = LocalHapticFeedback.current
    // Starts at the current state so opening a screen never replays a past outcome.
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        val from = previous[0]
        if (state == from) return@LaunchedEffect
        previous[0] = state
        val waking = from == WakeState.Sending || from == WakeState.Booting
        when {
            state == WakeState.Online && waking -> {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                delay(110)
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            state == WakeState.NoResponse && waking -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }
}

/** 0 → 1 → 0 over ~3 s, for the slow pulse of a booting machine. */
@Composable
fun breathing(): Float {
    val transition = rememberInfiniteTransition(label = "breathing")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    return value
}
