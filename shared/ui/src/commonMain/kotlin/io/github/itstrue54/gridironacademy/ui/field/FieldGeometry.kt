package io.github.itstrue54.gridironacademy.ui.field

import io.github.itstrue54.gridironacademy.model.Diagram
import io.github.itstrue54.gridironacademy.model.DiagramPlayer
import io.github.itstrue54.gridironacademy.model.DiagramZone
import io.github.itstrue54.gridironacademy.model.BallSpot
import io.github.itstrue54.gridironacademy.model.PathKind
import io.github.itstrue54.gridironacademy.model.Pt
import io.github.itstrue54.gridironacademy.model.Side
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

/**
 * Pinch zoom over the fitted field (DESIGN §7.10). A canvas point `p` at 1x is drawn at
 * `p * scale + pan`; [pan] is kept so the field always covers the canvas.
 */
data class FieldZoom(val scale: Float = 1f, val panX: Float = 0f, val panY: Float = 0f) {
    val zoomed: Boolean get() = scale > 1f

    /** Zoom by [factor] keeping the canvas point ([cx], [cy]) still, as a pinch does. */
    fun zoomAbout(factor: Float, cx: Float, cy: Float, width: Float, height: Float): FieldZoom {
        // Snap to exactly 1x: in then out by the same step can land on 1.0000001, which
        // would leave the field "zoomed" and swallowing one-finger scrolls.
        val s = (scale * factor).let { if (it < 1f + FieldGeometry.ZOOM_SNAP) 1f else it }.coerceAtMost(FieldGeometry.MAX_ZOOM)
        val k = s / scale
        return FieldZoom(s, cx - (cx - panX) * k, cy - (cy - panY) * k).clamped(width, height)
    }

    fun panBy(dx: Float, dy: Float, width: Float, height: Float): FieldZoom =
        copy(panX = panX + dx, panY = panY + dy).clamped(width, height)

    /** The canvas changed size (rotation, a sheet opening): keep the same relative view. */
    fun resized(oldWidth: Float, oldHeight: Float, width: Float, height: Float): FieldZoom =
        if (oldWidth <= 0f || oldHeight <= 0f) clamped(width, height)
        else copy(panX = panX * width / oldWidth, panY = panY * height / oldHeight).clamped(width, height)

    /** Never show past the canvas edge: at 1x pan is always zero. */
    fun clamped(width: Float, height: Float): FieldZoom =
        copy(panX = panX.coerceIn(width * (1 - scale), 0f), panY = panY.coerceIn(height * (1 - scale), 0f))

    /** The fitted transform with this zoom applied; everything downstream just uses it. */
    fun apply(t: FieldTransform): FieldTransform =
        t.copy(scale = t.scale * scale, offsetX = t.offsetX * scale + panX, offsetY = t.offsetY * scale + panY)
}

enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

/**
 * Players out of view while zoomed, shown as one marker on the nearest edge (DESIGN 7.10):
 * the learner can zoom in on the line and still know the corner is out there, and which way
 * to pan. [along] is the marker's position along that edge, in canvas pixels.
 */
data class EdgeMarker(val edge: Edge, val along: Float, val side: Side, val labels: List<String>)

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
    /** Pinch zoom limit: 4x makes linemen 1.6 yards apart about 40dp apart on a phone. */
    const val MAX_ZOOM = 4f
    /** Each tap of the zoom buttons. */
    const val ZOOM_STEP = 1.5f
    /** A zoom within this of 1x is 1x. */
    const val ZOOM_SNAP = 0.01f
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

    /**
     * One window covering several diagrams, e.g. a read-the-defense pre-snap and its
     * reveal, so both draw at the same scale and a zoom means the same spot in each.
     */
    fun window(vararg diagrams: Diagram): FieldWindow {
        val ws = diagrams.map { window(it) }
        return FieldWindow(ws.minOf { it.xMin }, ws.maxOf { it.xMax }, ws.minOf { it.yMin }, ws.maxOf { it.yMax })
    }

    /**
     * Markers for every player outside a [width] x [height] canvas under [t]. Players off
     * the same edge, on the same side, within [groupPx] of each other share a marker;
     * markers stay [insetPx] from the corners. Empty when everyone is in view.
     */
    fun offscreen(players: List<DiagramPlayer>, t: FieldTransform, width: Float, height: Float, insetPx: Float, groupPx: Float): List<EdgeMarker> {
        data class Off(val edge: Edge, val along: Float, val side: Side, val label: String)
        val off = players.mapNotNull { p ->
            val x = t.x(p.x)
            val y = t.y(p.y)
            val edge = when {
                x < 0f -> Edge.LEFT
                x > width -> Edge.RIGHT
                y < 0f -> Edge.TOP
                y > height -> Edge.BOTTOM
                else -> return@mapNotNull null
            }
            val along = if (edge == Edge.LEFT || edge == Edge.RIGHT) y.coerceIn(insetPx, height - insetPx) else x.coerceIn(insetPx, width - insetPx)
            Off(edge, along, p.side, p.label)
        }
        val markers = mutableListOf<EdgeMarker>()
        off.groupBy { it.edge to it.side }.forEach { (key, group) ->
            var current = mutableListOf<Off>()
            fun flush() {
                if (current.isNotEmpty()) {
                    markers += EdgeMarker(key.first, current.map { it.along }.average().toFloat(), key.second, current.map { it.label }.distinct())
                }
                current = mutableListOf()
            }
            group.sortedBy { it.along }.forEach { o ->
                if (current.isNotEmpty() && o.along - current.first().along > groupPx) flush()
                current += o
            }
            flush()
        }
        return markers.sortedWith(compareBy({ it.edge }, { it.along }))
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

    /** Paths a player travels along in the reveal (DESIGN 7.7); others are drawn, not driven. */
    val REVEAL_KINDS = setOf(PathKind.ROTATE, PathKind.DROP, PathKind.BLITZ)

    /** Film mode (DESIGN 7.8): every path runs, routes, runs, blocks and motion alike. */
    val FILM_KINDS = PathKind.entries.toSet()

    /**
     * Where the ball is at [p] of the play (DESIGN 7.8), or null when the diagram has none.
     * [spot] says where a player is at a given moment. Held, the ball rides with its
     * holder; between holders it flies, leaving the hand earlier on a longer throw.
     */
    fun ballAt(ball: List<BallSpot>, p: Float, spot: (playerId: String, time: Float) -> Pt?): Pt? {
        if (ball.isEmpty()) return null
        val i = ball.indexOfLast { it.at <= p }.coerceAtLeast(0)
        val held = ball[i]
        val next = ball.getOrNull(i + 1) ?: return spot(held.playerId, p)
        val to = spot(next.playerId, next.at) ?: return spot(held.playerId, p)
        val near = spot(held.playerId, next.at) ?: return null
        val flight = (hypot(to.x - near.x, to.y - near.y) * FLIGHT_PER_YARD).coerceIn(MIN_FLIGHT, MAX_FLIGHT)
        val leave = maxOf(held.at, next.at - flight)
        if (p <= leave) return spot(held.playerId, p)
        val from = spot(held.playerId, leave) ?: return null
        val k = (p - leave) / (next.at - leave)
        return Pt(from.x + (to.x - from.x) * k, from.y + (to.y - from.y) * k)
    }

    /** A ball in the air covers a yard in this share of the play, within these bounds. */
    private const val FLIGHT_PER_YARD = 0.012f
    private const val MIN_FLIGHT = 0.03f
    private const val MAX_FLIGHT = 0.3f
    /** Each reveal path starts this long after the one before it, in `routes` order. */
    const val REVEAL_STAGGER_MS = 60

    /** The point [f] (0..1) of the way from ([x], [y]) through [points], by distance travelled. */
    fun along(x: Float, y: Float, points: List<Pt>, f: Float): Pt {
        val legs = (listOf(Pt(x, y)) + points).zipWithNext()
        val total = legs.sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y).toDouble() }.toFloat()
        if (total == 0f || f <= 0f) return Pt(x, y)
        if (f >= 1f) return points.last()
        var left = total * f
        for ((a, b) in legs) {
            val len = hypot(b.x - a.x, b.y - a.y)
            if (left <= len) {
                val k = if (len == 0f) 0f else left / len
                return Pt(a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k)
            }
            left -= len
        }
        return points.last()
    }

    /**
     * How far along path [i] of [n] is at overall reveal progress [p], when each path
     * starts [staggerMs] after the last within [durationMs]. All finish together at p = 1.
     */
    fun staggered(p: Float, i: Int, n: Int, durationMs: Int, staggerMs: Int = REVEAL_STAGGER_MS): Float {
        if (n <= 1) return p.coerceIn(0f, 1f)
        val delay = (i * staggerMs).coerceAtMost(durationMs / 2)
        val span = (durationMs - (n - 1) * staggerMs).coerceAtLeast(durationMs / 2)
        return ((p * durationMs - delay) / span).coerceIn(0f, 1f)
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
