package io.github.itstrue54.gridironacademy.ui.field

import io.github.itstrue54.gridironacademy.model.Diagram
import io.github.itstrue54.gridironacademy.model.DiagramPlayer
import io.github.itstrue54.gridironacademy.model.DiagramZone
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** A rectangle of field in yards: x across (0 = middle), y downfield of the LOS. */
data class FieldWindow(val xMin: Float, val xMax: Float, val yMin: Float, val yMax: Float) {
    val xSpan: Float get() = xMax - xMin
    val ySpan: Float get() = yMax - yMin
    /** Width over height, before any clamping to the card's allowed aspect. */
    val aspect: Float get() = xSpan / ySpan
}

/**
 * Maps yards to pixels. Downfield (+y) is up the screen, so the offense moves up.
 * [scale] is pixels per yard; the window is centred in the canvas.
 */
data class FieldTransform(val window: FieldWindow, val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun x(yards: Float): Float = offsetX + (yards - window.xMin) * scale
    fun y(yards: Float): Float = offsetY + (window.yMax - yards) * scale
    fun xYards(px: Float): Float = window.xMin + (px - offsetX) / scale
    fun yYards(px: Float): Float = window.yMax - (px - offsetY) / scale
}

/** Pure layout maths for [FieldDiagram] (DESIGN §7.1), kept out of Compose so it is unit-tested. */
object FieldGeometry {
    /** Half of the 53.3-yard field width. */
    const val HALF_WIDTH = 26.67f
    /** NFL hash marks sit 3.1 yards either side of the middle. */
    const val HASH_X = 3.1f
    /** Space around the tight bounds of everything drawn. */
    const val MARGIN = 3f
    /** Smallest window, so one player or a short route isn't blown up to fill a card. */
    const val MIN_X_SPAN = 20f
    const val MIN_Y_SPAN = 10f
    /** The lesson card allows 4:3 to 3:4 (DESIGN §7.1). */
    const val MIN_CARD_ASPECT = 3f / 4f
    const val MAX_CARD_ASPECT = 4f / 3f
    /** Corners of a route are rounded with a one-yard radius (DESIGN §7.4). */
    const val CORNER_RADIUS = 1f

    /**
     * The tight bounds of every player, route point and zone, plus [MARGIN], always
     * including the line of scrimmage and clamped to the field's width. A diagram with
     * nothing on it shows [Diagram.behind] to [Diagram.depth] across the whole field.
     */
    fun window(d: Diagram): FieldWindow {
        val xs = mutableListOf<Float>()
        val ys = mutableListOf(0f) // the LOS is always in frame
        d.players.forEach { xs += it.x; ys += it.y }
        d.routes.forEach { r -> r.points.forEach { xs += it.x; ys += it.y } }
        d.zones.forEach { z -> xs += z.x; xs += z.x + z.w; ys += z.y; ys += z.y + z.h }
        if (xs.isEmpty()) return FieldWindow(-HALF_WIDTH, HALF_WIDTH, -d.behind, d.depth)

        var xMin = xs.min() - MARGIN
        var xMax = xs.max() + MARGIN
        var yMin = ys.min() - MARGIN
        var yMax = ys.max() + MARGIN
        if (xMax - xMin < MIN_X_SPAN) {
            val c = (xMin + xMax) / 2
            xMin = c - MIN_X_SPAN / 2; xMax = c + MIN_X_SPAN / 2
        }
        if (yMax - yMin < MIN_Y_SPAN) {
            val c = (yMin + yMax) / 2
            yMin = c - MIN_Y_SPAN / 2; yMax = c + MIN_Y_SPAN / 2
        }
        // Clamp across to the sidelines, shifting rather than squashing when possible.
        if (xMin < -HALF_WIDTH) { xMax = min(HALF_WIDTH, xMax + (-HALF_WIDTH - xMin)); xMin = -HALF_WIDTH }
        if (xMax > HALF_WIDTH) { xMin = max(-HALF_WIDTH, xMin - (xMax - HALF_WIDTH)); xMax = HALF_WIDTH }
        return FieldWindow(xMin, xMax, yMin, yMax)
    }

    /** The aspect a lesson card should give the diagram: its own, clamped to 3:4..4:3. */
    fun cardAspect(w: FieldWindow): Float = w.aspect.coerceIn(MIN_CARD_ASPECT, MAX_CARD_ASPECT)

    /** Fits [w] into a [widthPx] x [heightPx] canvas at one uniform scale, centred. */
    fun transform(w: FieldWindow, widthPx: Float, heightPx: Float): FieldTransform {
        val scale = min(widthPx / w.xSpan, heightPx / w.ySpan)
        return FieldTransform(
            window = w,
            scale = scale,
            offsetX = (widthPx - w.xSpan * scale) / 2,
            offsetY = (heightPx - w.ySpan * scale) / 2,
        )
    }

    /**
     * The player under a tap, or null. Each player owns a circle of [targetRadiusPx]
     * around its centre whatever its drawn size; overlaps go to the nearest centre
     * (DESIGN §7.3).
     */
    fun hitTest(players: List<DiagramPlayer>, t: FieldTransform, tapX: Float, tapY: Float, targetRadiusPx: Float): DiagramPlayer? =
        players
            .map { it to hypot(t.x(it.x) - tapX, t.y(it.y) - tapY) }
            .filter { it.second <= targetRadiusPx }
            .minByOrNull { it.second }
            ?.first

    /** The smallest zone containing a tap, or null (zones are tap targets in diagram_tap steps). */
    fun zoneAt(zones: List<DiagramZone>, t: FieldTransform, tapX: Float, tapY: Float): DiagramZone? {
        val x = t.xYards(tapX)
        val y = t.yYards(tapY)
        return zones
            .filter { x >= it.x && x <= it.x + it.w && y >= it.y && y <= it.y + it.h }
            .minByOrNull { it.w * it.h }
    }

    /**
     * The drawn size of every player mark: the skin's [preferredPx], shrunk so the closest
     * two players are [gapPx] apart edge to edge, but never under [minPx]. One size for
     * all marks, so size never implies importance.
     */
    fun markSize(players: List<DiagramPlayer>, t: FieldTransform, preferredPx: Float, minPx: Float, gapPx: Float): Float {
        var closest = Float.MAX_VALUE
        for (i in players.indices) for (j in i + 1 until players.size) {
            closest = min(closest, hypot(t.x(players[i].x) - t.x(players[j].x), t.y(players[i].y) - t.y(players[j].y)))
        }
        return (closest - gapPx).coerceIn(minPx, preferredPx)
    }

    /**
     * True when two players' centres are closer than [minPx]: the screen should then
     * offer list mode as an alternative to tapping (DESIGN §7.3).
     */
    fun crowded(players: List<DiagramPlayer>, t: FieldTransform, minPx: Float): Boolean =
        players.indices.any { i ->
            (i + 1 until players.size).any { j ->
                hypot(t.x(players[i].x) - t.x(players[j].x), t.y(players[i].y) - t.y(players[j].y)) < minPx
            }
        }
}
