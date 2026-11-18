package fr.sybers.debout.ui.character

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val HOLDABLE = setOf(Expression.Asleep, Expression.Sad, Expression.Awake)

/** How much the eyes move on their own. */
enum class IdleMotion {
    /** The hero view: every motion of the expression. */
    Lively,

    /** Small faces seen many at once: an occasional blink only, so a screen full of them stays restful. */
    Calm,

    /** Asleep, waiting for a first machine: breathes and now and then opens an eye. */
    Expectant,
}

/** Distance from the face's middle to each eye's center, in face widths. */
private const val EYE_SPACING = 0.16f

/**
 * The character's two pill-shaped eyes. They move on their own at rest (breathing, blinking,
 * glancing), and [holdProgress] opens them as the finger holds. Decorative: the state word
 * carries the meaning for accessibility.
 */
@Composable
fun PillEyes(
    expression: Expression,
    ink: Color,
    modifier: Modifier = Modifier,
    holdProgress: () -> Float = { 0f },
    idleMotion: IdleMotion = IdleMotion.Lively,
) {
    // Honors "Remove animations": the expression stays, the idle motion stops.
    val motion = remember { ValueAnimator.areAnimatorsEnabled() }
    val pose = expression.pose
    val shapeSpring = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    val width by animateFloatAsState(pose.width, shapeSpring, label = "eyeWidth")
    val height by animateFloatAsState(pose.height, shapeSpring, label = "eyeHeight")
    val y by animateFloatAsState(pose.y, shapeSpring, label = "eyeY")
    val tilt by animateFloatAsState(pose.tilt, shapeSpring, label = "eyeTilt")

    val blink = remember { Animatable(1f) }
    val glance = remember { Animatable(0f) }
    val peek = remember { Animatable(0f) }
    val twitch = remember { Animatable(1f) }
    var clock by remember { mutableLongStateOf(0L) }

    LaunchedEffect(expression, motion, idleMotion) {
        blink.snapTo(1f)
        glance.snapTo(0f)
        peek.snapTo(0f)
        twitch.snapTo(1f)
        if (!motion) return@LaunchedEffect
        val idle: suspend () -> Unit = if (idleMotion == IdleMotion.Calm) {
            { if (expression.eyesOpen) blinks(blink, minGap = 4_000, maxGap = 9_000) }
        } else {
            {
                coroutineScope {
                    if (expression == Expression.Asleep || expression == Expression.Searching) {
                        launch { while (true) withFrameMillis { clock = it } }
                    }
                    when (expression) {
                        Expression.Asleep -> {
                            launch { twitches(twitch) }
                            if (idleMotion == IdleMotion.Expectant) launch { peeks(peek) }
                        }
                        Expression.Drowsy -> launch { heavyBlinks(blink) }
                        Expression.Awake -> {
                            launch { blinks(blink, minGap = 2_500, maxGap = 6_000) }
                            launch { glances(glance) }
                        }
                        Expression.Sad -> launch { peeks(peek) }
                        Expression.Searching -> launch { blinks(blink, minGap = 3_000, maxGap = 7_000) }
                        Expression.Blank -> launch { blinks(blink, minGap = 8_000, maxGap = 12_000) }
                        Expression.Surprised -> Unit
                    }
                }
            }
        }
        // The idle motion never ends: declaring it lets UI tests skip it instead of waiting forever.
        val policy = coroutineContext[InfiniteAnimationPolicy]
        if (policy == null) idle() else policy.onInfiniteOperation(idle)
    }

    Canvas(modifier) {
        val face = size.width
        // The hold completes before the state changes: once it has, the new expression wins.
        val progress = if (expression in HOLDABLE) holdProgress() else 0f
        val base = EyePose(width, height, y, tilt)
        val current = if (progress > 0f) lerp(base, expression.holdTarget.pose, progress.coerceAtMost(1f)) else base
        val breathe = if (expression == Expression.Asleep) -0.022f * (0.5f - 0.5f * cos(2 * PI * clock / 4_200).toFloat()) else 0f
        val search = if (expression == Expression.Searching) 0.05f * sin(2 * PI * clock / 2_400).toFloat() else 0f
        val shiftX = (glance.value + search) * face
        val centerY = size.height / 2 + (current.y + breathe) * face
        val openness = max(blink.value, progress)
        drawEye(Offset(size.width / 2 - EYE_SPACING * face + shiftX, centerY), current, face, openness, twitch.value, tiltSign = 1f, extraHeight = 0f, ink)
        drawEye(Offset(size.width / 2 + EYE_SPACING * face + shiftX, centerY), current, face, openness, twitch.value, tiltSign = -1f, extraHeight = 0.065f * peek.value, ink)
    }
}

private fun DrawScope.drawEye(
    center: Offset,
    pose: EyePose,
    face: Float,
    openness: Float,
    stretch: Float,
    tiltSign: Float,
    extraHeight: Float,
    ink: Color,
) {
    val width = pose.width * face * stretch
    val height = max((pose.height + extraHeight) * face * openness, face * 0.02f)
    rotate(pose.tilt * tiltSign, pivot = center) {
        drawRoundRect(
            color = ink,
            topLeft = Offset(center.x - width / 2, center.y - height / 2),
            size = Size(width, height),
            cornerRadius = CornerRadius(minOf(width, height) / 2),
        )
    }
}

private val Expression.eyesOpen: Boolean
    get() = this != Expression.Asleep && this != Expression.Sad && this != Expression.Surprised

private suspend fun blinks(blink: Animatable<Float, *>, minGap: Long, maxGap: Long) {
    while (true) {
        delay(Random.nextLong(minGap, maxGap))
        blink.animateTo(0.08f, tween(70))
        blink.animateTo(1f, tween(120))
    }
}

private suspend fun heavyBlinks(blink: Animatable<Float, *>) {
    while (true) {
        delay(Random.nextLong(1_500, 3_000))
        blink.animateTo(0.3f, tween(900, easing = FastOutSlowInEasing))
        delay(300)
        blink.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
}

private suspend fun glances(glance: Animatable<Float, *>) {
    while (true) {
        delay(Random.nextLong(5_000, 9_000))
        glance.animateTo(if (Random.nextBoolean()) 0.04f else -0.04f, tween(350, easing = FastOutSlowInEasing))
        delay(Random.nextLong(800, 1_600))
        glance.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
    }
}

private suspend fun peeks(peek: Animatable<Float, *>) {
    while (true) {
        delay(Random.nextLong(5_000, 9_000))
        peek.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        delay(700)
        peek.animateTo(0f, tween(300))
    }
}

private suspend fun twitches(twitch: Animatable<Float, *>) {
    while (true) {
        delay(Random.nextLong(6_000, 12_000))
        twitch.animateTo(1.08f, tween(90))
        twitch.animateTo(1f, tween(160))
    }
}
