package fr.sybers.debout.ui.hero

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.sybers.debout.R
import kotlin.math.min

private const val MAX_VISIBLE_DOTS = 7

/** Beyond [MAX_VISIBLE_DOTS] pages, the dots slide with the current page and shrink at the cut edges. */
@Composable
fun PageDots(count: Int, current: Int, color: Color, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.page_indicator, current + 1, count)
    val first = if (count <= MAX_VISIBLE_DOTS) 0 else (current - MAX_VISIBLE_DOTS / 2).coerceIn(0, count - MAX_VISIBLE_DOTS)
    val last = min(count, first + MAX_VISIBLE_DOTS) - 1
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (page in first..last) {
            val cutEdge = (page == first && first > 0) || (page == last && last < count - 1)
            val size by animateDpAsState(
                when {
                    page == current -> 10.dp
                    cutEdge -> 4.dp
                    else -> 7.dp
                },
                label = "dotSize",
            )
            val alpha by animateFloatAsState(if (page == current) 1f else 0.4f, label = "dotAlpha")
            Box(
                Modifier
                    .size(size)
                    .background(color.copy(alpha = alpha), CircleShape),
            )
        }
    }
}
