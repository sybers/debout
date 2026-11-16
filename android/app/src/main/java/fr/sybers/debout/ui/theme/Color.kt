package fr.sybers.debout.ui.theme

import androidx.compose.ui.graphics.Color

// TODO: replace with Debout's own palette once the visual identity is designed.
val Primary80 = Color(0xFFFFB68A)
val Secondary80 = Color(0xFFE5BFA8)
val Tertiary80 = Color(0xFFC9CA93)

val Primary40 = Color(0xFF944A15)
val Secondary40 = Color(0xFF765848)
val Tertiary40 = Color(0xFF5F6134)

/** The hero view keeps its own dark palette whatever the system theme. */
object HeroColors {
    val Ember = Color(0xFFFF9F5A)
    val EmberDeep = Color(0xFFB4541A)
    val OnEmber = Color(0xFF2A1406)
    val Asleep = Color(0xFF101114)
    val Smoulder = Color(0xFF1A1310)
    val Ash = Color(0xFF221A1A)
    val OffNetwork = Color(0xFF26272A)
    val OnNight = Color(0xFFEDE0D8)
}

/** Provisional: chosen to stay apart in light, dark and for colour-blind eyes; the shape carries the meaning. */
object QuickAccessColors {
    val First = Color(0xFF4C8DF6)
    val Second = Color(0xFFE0559A)
    val Third = Color(0xFF2BB39A)
}
