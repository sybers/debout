package fr.sybers.debout.ui.character

import fr.sybers.debout.domain.WakeState

/** How the character looks: one expression per wake state, see the Design section of the spec. */
enum class Expression { Asleep, Surprised, Drowsy, Awake, Sad, Searching, Blank }

fun WakeState.expression(onLocalNetwork: Boolean): Expression = when (this) {
    WakeState.Unknown -> if (onLocalNetwork) Expression.Searching else Expression.Blank
    WakeState.Offline -> Expression.Asleep
    WakeState.Sending -> Expression.Surprised
    WakeState.Booting -> Expression.Drowsy
    WakeState.Online -> Expression.Awake
    WakeState.NoResponse -> Expression.Sad
}

/**
 * One eye, in fractions of the face width so the character scales with the screen.
 * [y] moves the eye down from the face's middle; [tilt] is in degrees for the left eye,
 * mirrored on the right one.
 */
data class EyePose(val width: Float, val height: Float, val y: Float = 0f, val tilt: Float = 0f)

val Expression.pose: EyePose
    get() = when (this) {
        Expression.Asleep -> EyePose(width = 0.17f, height = 0.04f)
        Expression.Surprised -> EyePose(width = 0.24f, height = 0.24f)
        Expression.Drowsy -> EyePose(width = 0.16f, height = 0.11f, y = 0.02f)
        Expression.Awake -> EyePose(width = 0.15f, height = 0.34f)
        Expression.Sad -> EyePose(width = 0.15f, height = 0.035f, y = 0.03f, tilt = 12f)
        Expression.Searching -> EyePose(width = 0.15f, height = 0.2f)
        Expression.Blank -> EyePose(width = 0.14f, height = 0.2f)
    }

/** Holding opens the eyes: toward [Expression.Awake], or wider still when the machine is already up. */
val Expression.holdTarget: Expression
    get() = if (this == Expression.Awake) Expression.Surprised else Expression.Awake

fun Expression.poseWhileHolding(progress: Float): EyePose =
    if (progress <= 0f) pose else lerp(pose, holdTarget.pose, progress.coerceAtMost(1f))

fun lerp(from: EyePose, to: EyePose, fraction: Float): EyePose = EyePose(
    width = from.width + (to.width - from.width) * fraction,
    height = from.height + (to.height - from.height) * fraction,
    y = from.y + (to.y - from.y) * fraction,
    tilt = from.tilt + (to.tilt - from.tilt) * fraction,
)
