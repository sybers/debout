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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import fr.sybers.debout.R
import fr.sybers.debout.domain.WakeState
import kotlinx.coroutines.delay

/** Online stays holdable so the user gets "Already on" rather than a dead button. */
val WakeState.acceptsHold: Boolean
    get() = this == WakeState.Offline || this == WakeState.NoResponse || this == WakeState.Online

@get:StringRes
val WakeState.heroWord: Int
    get() = when (this) {
        WakeState.Unknown -> R.string.hero_word_unknown
        WakeState.Offline -> R.string.hero_word_offline
        WakeState.Sending -> R.string.hero_word_sending
        WakeState.Booting -> R.string.hero_word_booting
        WakeState.Online -> R.string.hero_word_online
        WakeState.NoResponse -> R.string.hero_word_no_response
    }

@get:StringRes
val WakeState.statusLabel: Int
    get() = when (this) {
        WakeState.Unknown -> R.string.status_unknown
        WakeState.Offline -> R.string.status_offline
        WakeState.Sending -> R.string.status_sending
        WakeState.Booting -> R.string.status_booting
        WakeState.Online -> R.string.status_online
        WakeState.NoResponse -> R.string.status_no_response
    }

/** Double confirmation when the machine comes online, rejection when it stays silent. */
@Composable
fun WakeStateHaptics(state: WakeState) {
    val haptics = LocalHapticFeedback.current
    // Starts at the current state so opening a screen never replays a past outcome.
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        if (state == previous[0]) return@LaunchedEffect
        previous[0] = state
        when (state) {
            WakeState.Online -> {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                delay(110)
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            WakeState.NoResponse -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            else -> Unit
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
