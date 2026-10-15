package io.github.itstrue54.gridironacademy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The first glyphs of the icon set (DESIGN 6.2), drawn on a 24-unit grid so they scale
 * cleanly and need no image assets. Decorative: callers give the meaning in text or
 * semantics, never by the glyph alone.
 */
enum class Glyph { CHECK, DASH, CLOSE, BACK, LOCK, STAR, FLAME, UP, DOWN }

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, filled: Boolean = true) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(width = 2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(vararg pts: Float) = Path().apply {
            moveTo(pts[0] * u, pts[1] * u)
            var i = 2
            while (i < pts.size) { lineTo(pts[i] * u, pts[i + 1] * u); i += 2 }
        }
        when (glyph) {
            Glyph.CHECK -> drawPath(p(5f, 12.5f, 10f, 17.5f, 19f, 7f), color, style = stroke)
            Glyph.DASH -> drawLine(color, Offset(6f * u, 12f * u), Offset(18f * u, 12f * u), 2.6f * u, StrokeCap.Round)
            Glyph.CLOSE -> {
                drawLine(color, Offset(6f * u, 6f * u), Offset(18f * u, 18f * u), 2.4f * u, StrokeCap.Round)
                drawLine(color, Offset(18f * u, 6f * u), Offset(6f * u, 18f * u), 2.4f * u, StrokeCap.Round)
            }
            Glyph.BACK -> drawPath(p(15f, 5f, 8f, 12f, 15f, 19f), color, style = stroke)
            Glyph.UP -> drawPath(p(5f, 15f, 12f, 8f, 19f, 15f), color, style = stroke)
            Glyph.DOWN -> drawPath(p(5f, 9f, 12f, 16f, 19f, 9f), color, style = stroke)
            Glyph.LOCK -> lock(u, color)
            Glyph.STAR -> drawPath(star(u), color, style = if (filled) Fill else Stroke(2f * u, join = StrokeJoin.Round))
            Glyph.FLAME -> drawPath(flame(u), color, style = if (filled) Fill else stroke)
        }
    }
}

private fun DrawScope.lock(u: Float, color: Color) {
    drawRoundRect(color, Offset(5f * u, 11f * u), androidx.compose.ui.geometry.Size(14f * u, 10f * u), androidx.compose.ui.geometry.CornerRadius(2f * u))
    val shackle = Path().apply {
        moveTo(8f * u, 11f * u)
        lineTo(8f * u, 8f * u)
        cubicTo(8f * u, 3.5f * u, 16f * u, 3.5f * u, 16f * u, 8f * u)
        lineTo(16f * u, 11f * u)
    }
    drawPath(shackle, color, style = Stroke(2.4f * u, cap = StrokeCap.Round))
}

private fun star(u: Float) = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 10f else 4.3f
        val a = -PI / 2 + i * PI / 5
        val x = 12f + r * cos(a).toFloat()
        val y = 12.8f + r * sin(a).toFloat()
        if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u)
    }
    close()
}

private fun flame(u: Float) = Path().apply {
    moveTo(12f * u, 2.5f * u)
    cubicTo(14f * u, 7f * u, 19.5f * u, 9.5f * u, 19.5f * u, 14.5f * u)
    cubicTo(19.5f * u, 19f * u, 16f * u, 21.5f * u, 12f * u, 21.5f * u)
    cubicTo(8f * u, 21.5f * u, 4.5f * u, 19f * u, 4.5f * u, 14.5f * u)
    cubicTo(4.5f * u, 11f * u, 7f * u, 9f * u, 8.5f * u, 7f * u)
    cubicTo(9f * u, 9.5f * u, 10.5f * u, 10.5f * u, 11.5f * u, 10.5f * u)
    cubicTo(11f * u, 7.5f * u, 11f * u, 5f * u, 12f * u, 2.5f * u)
    close()
}
