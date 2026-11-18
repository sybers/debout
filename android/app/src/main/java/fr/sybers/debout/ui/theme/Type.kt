package fr.sybers.debout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import fr.sybers.debout.R

/** Unbounded is a variable font: each weight is an instance of the same file. */
@OptIn(ExperimentalTextApi::class)
val Unbounded = FontFamily(
    (200..900 step 100).map { weight ->
        Font(
            R.font.unbounded,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

/** For MAC and IP addresses. */
val RecursiveMono = FontFamily(Font(R.font.recursive_mono))

private val defaults = Typography()

/** Unbounded for display text and titles, the system font for everything people read at length. */
val Typography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = Unbounded, fontWeight = FontWeight.ExtraBold),
    displayMedium = defaults.displayMedium.copy(fontFamily = Unbounded, fontWeight = FontWeight.ExtraBold),
    displaySmall = defaults.displaySmall.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold),
    titleLarge = defaults.titleLarge.copy(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold),
)
