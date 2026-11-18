package fr.sybers.debout.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** One color-block pair per wake state: the background, and the ink of the eyes and text. */
@Immutable
data class HeroPalette(val background: Color, val ink: Color)

/** The hero view keeps these whatever the system theme or the wallpaper colors. Each pair is above 4.5:1. */
object HeroColors {
    val Asleep = HeroPalette(background = Color(0xFFC9B6FF), ink = Color(0xFF8A1C12))
    val Hey = HeroPalette(background = Color(0xFFFFFFFF), ink = Color(0xFF111111))
    val Waking = HeroPalette(background = Color(0xFFB8F2D8), ink = Color(0xFF1F2FBF))
    val Awake = HeroPalette(background = Color(0xFFE9B949), ink = Color(0xFF4B1F78))
    val Silent = HeroPalette(background = Color(0xFF2B2B5E), ink = Color(0xFFF5A3B8))
    val Checking = HeroPalette(background = Color(0xFFD9D4CC), ink = Color(0xFF2A3FA8))
    val OffNetwork = HeroPalette(background = Color(0xFFD9D4CC), ink = Color(0xFF4A443E))
}

// Material 3 "vibrant" scheme from ultramarine #1F2FBF, with neutrals taken from greige #D9D4CC.
// Generated with material-color-utilities 0.3.0; regenerate rather than hand-tune single roles.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF2A3FFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF001EDF),
    inversePrimary = Color(0xFFBDC2FF),
    secondary = Color(0xFF615A7C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7DEFF),
    onSecondaryContainer = Color(0xFF494263),
    tertiary = Color(0xFF6D5484),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0DBFF),
    onTertiaryContainer = Color(0xFF543C6B),
    background = Color(0xFFFEF9F2),
    onBackground = Color(0xFF1D1B18),
    surface = Color(0xFFFEF9F2),
    onSurface = Color(0xFF1D1B18),
    surfaceVariant = Color(0xFFE8E2D7),
    onSurfaceVariant = Color(0xFF49473E),
    surfaceTint = Color(0xFF2A3FFF),
    inverseSurface = Color(0xFF32302C),
    inverseOnSurface = Color(0xFFF5F0E9),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF7B776D),
    outlineVariant = Color(0xFFCBC6BB),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFEF9F2),
    surfaceContainer = Color(0xFFF2EDE6),
    surfaceContainerHigh = Color(0xFFECE7E1),
    surfaceContainerHighest = Color(0xFFE7E2DB),
    surfaceContainerLow = Color(0xFFF8F3EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDED9D3),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFBDC2FF),
    onPrimary = Color(0xFF0013A0),
    primaryContainer = Color(0xFF001EDF),
    onPrimaryContainer = Color(0xFFDFE0FF),
    inversePrimary = Color(0xFF2A3FFF),
    secondary = Color(0xFFCBC1E9),
    onSecondary = Color(0xFF332C4C),
    secondaryContainer = Color(0xFF494263),
    onSecondaryContainer = Color(0xFFE7DEFF),
    tertiary = Color(0xFFD9BBF2),
    onTertiary = Color(0xFF3D2653),
    tertiaryContainer = Color(0xFF543C6B),
    onTertiaryContainer = Color(0xFFF0DBFF),
    background = Color(0xFF141310),
    onBackground = Color(0xFFE7E2DB),
    surface = Color(0xFF141310),
    onSurface = Color(0xFFE7E2DB),
    surfaceVariant = Color(0xFF49473E),
    onSurfaceVariant = Color(0xFFCBC6BB),
    surfaceTint = Color(0xFFBDC2FF),
    inverseSurface = Color(0xFFE7E2DB),
    inverseOnSurface = Color(0xFF32302C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF959086),
    outlineVariant = Color(0xFF49473E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3B3934),
    surfaceContainer = Color(0xFF211F1B),
    surfaceContainerHigh = Color(0xFF2B2A26),
    surfaceContainerHighest = Color(0xFF363530),
    surfaceContainerLow = Color(0xFF1D1B18),
    surfaceContainerLowest = Color(0xFF0F0E0A),
    surfaceDim = Color(0xFF141310),
)
