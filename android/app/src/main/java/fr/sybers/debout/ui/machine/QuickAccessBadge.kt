package fr.sybers.debout.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import fr.sybers.debout.R
import fr.sybers.debout.domain.QuickAccessSlot

val QuickAccessSlot.shape: Shape
    get() = when (this) {
        QuickAccessSlot.First -> CircleShape
        QuickAccessSlot.Second -> RoundedTriangle
        QuickAccessSlot.Third -> RoundedSquare
    }

private val RoundedTriangle: Shape = PolygonShape(
    RoundedPolygon(
        vertices = floatArrayOf(0.5f, 0f, 1f, 0.87f, 0f, 0.87f),
        rounding = CornerRounding(radius = 0.18f),
    ),
)

private val RoundedSquare: Shape = PolygonShape(
    RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = CornerRounding(radius = 0.3f)),
)

/** Draws a graphics-shapes polygon, the shape library behind Material 3's rounded shapes. */
private class PolygonShape(polygon: RoundedPolygon) : Shape {
    private val unitPolygon = polygon.normalized()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = unitPolygon.toPath().asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

/** Ties a machine to the tile and app shortcut of the same shape. Only the shape tells the slots apart. */
@Composable
fun QuickAccessBadge(slot: QuickAccessSlot, color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    val description = stringResource(R.string.quick_access_slot, slot.number)
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = description }
            .clip(slot.shape)
            .background(color),
    )
}
