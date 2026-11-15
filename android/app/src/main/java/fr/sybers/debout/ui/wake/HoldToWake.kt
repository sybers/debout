package fr.sybers.debout.ui.wake

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import fr.sybers.debout.R
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

const val HOLD_DURATION_MILLIS = 500
private const val RETRACT_MILLIS = 250
private const val FADE_MILLIS = 600

@Stable
class HoldToWake internal constructor(
    private val fill: Animatable<Float, AnimationVector1D>,
    private val fade: Animatable<Float, AnimationVector1D>,
    private val origin: MutableState<Offset>,
    /** Apply to the hold area: pointer gesture plus the accessibility "Wake" action. */
    val modifier: Modifier,
) {
    /** Paints the colour spreading from the finger; draw it behind the hold area's content. */
    fun DrawScope.drawFlood(color: Color) {
        val progress = fill.value
        if (progress <= 0f) return
        val center = origin.value
        val farthestCorner = max(
            max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
            max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
        )
        drawCircle(color, radius = farthestCorner * progress * progress, center = center, alpha = fade.value)
    }
}

/**
 * Holding for [HOLD_DURATION_MILLIS] calls [onWake]; releasing earlier retracts the colour
 * and sends nothing. TalkBack users get a standard click action instead.
 */
@Composable
fun rememberHoldToWake(enabled: Boolean, onWake: () -> Unit): HoldToWake {
    val fill = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val origin = remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnWake by rememberUpdatedState(onWake)
    val wakeLabel = stringResource(R.string.action_wake)

    val wake = {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        currentOnWake()
    }

    val modifier = Modifier
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (enabled) onClick(label = wakeLabel) { wake(); true } else disabled()
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(onPress = { offset ->
                origin.value = offset
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                var completed = false
                val filling = scope.launch {
                    // A press during the post-wake fade starts over instead of re-firing at once.
                    if (fade.value < 1f || fill.value >= 1f) fill.snapTo(0f)
                    fade.snapTo(1f)
                    val remaining = (HOLD_DURATION_MILLIS * (1f - fill.value)).toInt()
                    fill.animateTo(1f, tween(remaining, easing = LinearEasing))
                    completed = true
                    wake()
                    fade.animateTo(0f, tween(FADE_MILLIS))
                    fill.snapTo(0f)
                    fade.snapTo(1f)
                }
                tryAwaitRelease()
                if (!completed) {
                    filling.cancel()
                    scope.launch { fill.animateTo(0f, tween(RETRACT_MILLIS, easing = FastOutSlowInEasing)) }
                }
            })
        }

    return HoldToWake(fill, fade, origin, modifier)
}
