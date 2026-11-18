package fr.sybers.debout.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import fr.sybers.debout.ui.theme.HeroPalette

/** The character in small: its eyes on a tile of the state's colors. */
@Composable
fun CharacterHead(
    expression: Expression,
    palette: HeroPalette,
    size: Dp,
    modifier: Modifier = Modifier,
    idleMotion: IdleMotion = IdleMotion.Calm,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 30))
            .background(palette.background),
    ) {
        PillEyes(expression, palette.ink, Modifier.fillMaxSize(), idleMotion = idleMotion)
    }
}
