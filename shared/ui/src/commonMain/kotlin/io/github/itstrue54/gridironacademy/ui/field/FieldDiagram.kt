package io.github.itstrue54.gridironacademy.ui.field

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.itstrue54.gridironacademy.model.Diagram
import io.github.itstrue54.gridironacademy.model.DiagramPlayer
import io.github.itstrue54.gridironacademy.model.DiagramRoute
import io.github.itstrue54.gridironacademy.model.PathKind
import io.github.itstrue54.gridironacademy.model.Side
import io.github.itstrue54.gridironacademy.ui.theme.DiagramColors
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Words the field draws or announces, supplied by the caller in the learner's language. */
@Immutable
data class FieldLabels(
    /** The Adult skin's line-of-scrimmage tag. */
    val los: String = "LOS",
    val zoomIn: String = "Zoom in",
    val zoomOut: String = "Zoom out",
)

/** Hoistable zoom for one diagram: reset by giving a new key (DESIGN §7.10). */
@Stable
class FieldZoomState {
    var value by mutableStateOf(FieldZoom())
}

@Composable
fun rememberFieldZoomState(key: Any?): FieldZoomState = remember(key) { FieldZoomState() }

/** How a player mark is shown after a tap or grading (DESIGN §7.3). */
enum class MarkState { SELECTED, CORRECT, WRONG, MISSED }

/**
 * Draws a [Diagram]: a formation, a route concept, a coverage shell or a rotation, on a
 * dark-turf field in both themes (DESIGN §7). Every picture in the app goes through here.
 *
 * @param description the diagram as text for screen readers (DESIGN §7.6).
 * @param marks per player or zone id, how to show it: selected, or graded.
 * @param highlightZone with more than four zones, only this one is filled (DESIGN §7.5).
 * @param onTap called with the id of the tapped player, else the tapped zone.
 * @param fitToCard true: size to the diagram's own aspect, clamped to 3:4..4:3 (lesson
 *   card); false: fill the height the caller gives it (game screens).
 * @param labels the words drawn on or around the field, localised by the caller.
 * @param zoom pinch-zoom state; one per diagram by default. To keep zoom across a
 *   read-the-defense pre-snap and its reveal, hoist it and pass the same [window].
 * @param window the field area to fit; by default this diagram's own. Pass
 *   `FieldGeometry.window(preSnap, reveal)` so both pictures share one scale.
 */
@Composable
fun FieldDiagram(
    diagram: Diagram,
    description: String,
    modifier: Modifier = Modifier,
    marks: Map<String, MarkState> = emptyMap(),
    highlightZone: String? = null,
    onTap: ((String) -> Unit)? = null,
    fitToCard: Boolean = true,
    labels: FieldLabels = FieldLabels(),
    zoom: FieldZoomState = rememberFieldZoomState(diagram),
    window: FieldWindow = remember(diagram) { FieldGeometry.window(diagram) },
) {
    val skin = Gridiron.skin
    val colors = Gridiron.colors
    val type = Gridiron.type
    val reducedMotion = Gridiron.reducedMotion
    // Big enough to hold every label on a full 22-player diagram, so redraws during the
    // reveal hit the cache instead of re-measuring text each frame.
    val measurer = rememberTextMeasurer(cacheSize = TEXT_CACHE)
    val currentOnTap = rememberUpdatedState(onTap)
    val tapRadius = rememberUpdatedState(skin.touchTarget)
    val pill = remember(colors) { colors.surface.copy(alpha = PILL_ALPHA) }
    val labelStyle = type.label.copy(color = colors.ink)
    val tagStyle = labelStyle.copy(fontSize = type.label.fontSize * TAG_SCALE, lineHeight = type.label.lineHeight * TAG_SCALE)
    val baseLabelSp = if (skin.helmets) KID_MARK_LABEL_SP else ADULT_MARK_LABEL_SP
    val bodyFamily = type.body.fontFamily

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    // Zoom controls appear only when the fitted picture had to shrink its marks.
    val crowded = remember(diagram, window, canvasSize, skin.playerMark, density) {
        canvasSize.width > 0 && with(density) {
            val base = FieldGeometry.transform(window, canvasSize.width.toFloat(), canvasSize.height.toFloat())
            FieldGeometry.markSize(diagram.players, base, skin.playerMark.toPx(), 0f, MARK_GAP.dp.toPx()) < skin.playerMark.toPx()
        }
    }
    // Read through derivedStateOf so a pinch redraws the canvas without recomposing this.
    val zoomed by remember(zoom) { derivedStateOf { zoom.value.zoomed } }

    Column(modifier) {
        val sized = if (fitToCard) Modifier.fillMaxWidth().aspectRatio(FieldGeometry.cardAspect(window)) else Modifier.fillMaxWidth().weight(1f)
        Box(sized) {
            Canvas(
                Modifier.fillMaxSize()
                    .onSizeChanged { new ->
                        val old = canvasSize
                        if (zoom.value.zoomed) {
                            zoom.value = zoom.value.resized(old.width.toFloat(), old.height.toFloat(), new.width.toFloat(), new.height.toFloat())
                        }
                        canvasSize = new
                    }
                    .clipToBounds()
                    .semantics { contentDescription = description }
                    .pointerInput(window, zoom) { detectZoom(zoom) }
                    .then(
                        if (onTap == null) Modifier
                        else Modifier.pointerInput(window, diagram) {
                            detectTapGestures { tap ->
                                if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                                val t = zoom.value.apply(FieldGeometry.transform(window, size.width.toFloat(), size.height.toFloat()))
                                val radius = tapRadius.value.toPx() / 2
                                val id = FieldGeometry.hitTest(diagram.players, t, tap.x, tap.y, radius)?.id
                                    ?: FieldGeometry.zoneAt(diagram.zones, t, tap.x, tap.y)?.id
                                if (id != null) currentOnTap.value?.invoke(id)
                            }
                        },
                    ),
            ) {
                // A zero-size canvas (no height left on a game screen) would make the scale 0
                // and every yards-from-pixels conversion infinite.
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val t = zoom.value.apply(FieldGeometry.transform(window, size.width, size.height))
                // Marks shrink to the tightest spacing (a full-width formation on a phone puts
                // linemen ~10dp apart), but never below a size that holds a two-digit number.
                val markPx = FieldGeometry.markSize(diagram.players, t, skin.playerMark.toPx(), MIN_MARK.dp.toPx(), MARK_GAP.dp.toPx())
                val labelSp = (baseLabelSp * markPx / skin.playerMark.toPx()).coerceAtLeast(MIN_MARK_LABEL_SP)
                val markLabel = TextStyle(color = DiagramColors.label, fontFamily = bodyFamily, fontWeight = FontWeight.Bold, fontSize = labelSp.sp)

                // Back to front: field, zones, paths, labels, then players on top so no label
                // ever hides a player (DESIGN 7.2-7.5).
                drawTurf(t)
                drawZones(diagram, t, highlightZone, marks)
                diagram.routes.forEach { route ->
                    diagram.players.firstOrNull { it.id == route.playerId }?.let { drawRoute(route, it, t) }
                }
                diagram.zones.forEach { z ->
                    if (z.label.isNotEmpty()) {
                        drawPill(measurer, z.label, labelStyle, pill, Offset(t.x(z.x + z.w / 2), t.y(z.y + z.h / 2)))
                    }
                }
                diagram.routes.filter { it.label.isNotEmpty() && it.points.isNotEmpty() }.forEach { r ->
                    val end = r.points.last()
                    drawPill(measurer, r.label, labelStyle, pill, Offset(t.x(end.x), t.y(end.y) - ROUTE_LABEL_LIFT.dp.toPx()))
                }
                if (skin.losTag) {
                    // Just under the line at whichever end has no player on it; none if both do.
                    val tag = measurer.measure(labels.los, tagStyle)
                    val w = tag.size.width + 12.dp.toPx()
                    val h = tag.size.height + 4.dp.toPx()
                    val y = t.y(0f) + LOS_TAG_DROP.dp.toPx()
                    val inset = LOS_TAG_INSET.dp.toPx()
                    val clearance = markPx / 2
                    fun free(left: Float) = diagram.players.none { pl ->
                        val px = t.x(pl.x); val py = t.y(pl.y)
                        px + clearance > left && px - clearance < left + w && py + clearance > y - h / 2 && py - clearance < y + h / 2
                    }
                    val left = listOf(inset, size.width - inset - w).firstOrNull { free(it) }
                    if (left != null) drawPill(measurer, labels.los, tagStyle, pill, Offset(left, y), anchorLeft = true)
                }
                diagram.players.forEach { drawPlayer(it, t, markPx / 2, skin.helmets, marks[it.id], reducedMotion, measurer, markLabel) }
            }
            if (crowded || zoomed) {
                ZoomButtons(
                    zoom = zoom,
                    size = canvasSize,
                    labels = labels,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(ZOOM_BUTTON_INSET.dp),
                )
            }
        }
        if (diagram.caption.isNotEmpty()) {
            Text(diagram.caption, style = type.label, color = colors.inkDim, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private const val PILL_ALPHA = 0.7f
private const val ZOOM_BUTTON_INSET = 6
private const val ZOOM_BUTTON_ALPHA = 0.92f
private const val TEXT_CACHE = 64
private const val ROUTE_LABEL_LIFT = 14
private const val LOS_TAG_INSET = 6
private const val LOS_TAG_DROP = 10
private const val TAG_SCALE = 0.8f
private const val KID_MARK_LABEL_SP = 13f
private const val ADULT_MARK_LABEL_SP = 11f
private const val MIN_MARK_LABEL_SP = 9f
private const val MIN_MARK = 18
private const val MARK_GAP = 2
private const val STRIPE_YARDS = 5
private const val MAJOR_LINE_YARDS = 10
private const val MINOR_LINE_ALPHA = 0.35f
private const val MAJOR_LINE_ALPHA = 0.6f
private const val HASH_ALPHA = 0.4f
private const val SELECTED_SCALE = 1.15f

// ---- Zoom (DESIGN §7.10) ----

/**
 * Two fingers pinch and pan. One finger pans only once zoomed in, and only while the
 * view can still move that way: at 1x, or pinned against an edge, a drag passes through
 * and scrolls the lesson card.
 */
private suspend fun PointerInputScope.detectZoom(zoom: FieldZoomState) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false)
    do {
        val event = awaitPointerEvent()
        val fingers = event.changes.count { it.pressed }
        val current = zoom.value
        if (fingers >= 2 || current.zoomed) {
            val factor = event.calculateZoom()
            val pan = event.calculatePan()
            if (factor != 1f || pan != Offset.Zero) {
                val c = event.calculateCentroid(useCurrent = true)
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val next = current.zoomAbout(factor, c.x, c.y, w, h).panBy(pan.x, pan.y, w, h)
                if (fingers >= 2 || next != current) {
                    zoom.value = next
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        }
    } while (event.changes.any { it.pressed })
}

/** The accessible alternative to pinching (DESIGN §12): zoom out, zoom in, about the centre. */
@Composable
private fun ZoomButtons(zoom: FieldZoomState, size: IntSize, labels: FieldLabels, modifier: Modifier) {
    val skin = Gridiron.skin
    val colors = Gridiron.colors
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("−", labels.zoomOut, 1f / FieldGeometry.ZOOM_STEP),
            Triple("+", labels.zoomIn, FieldGeometry.ZOOM_STEP),
        ).forEach { (glyph, label, factor) ->
            val enabled = if (factor < 1f) zoom.value.zoomed else zoom.value.scale < FieldGeometry.MAX_ZOOM
            Surface(
                onClick = { zoom.value = zoom.value.zoomAbout(factor, w / 2, h / 2, w, h) },
                enabled = enabled,
                shape = skin.buttonShape,
                color = colors.surfaceRaised.copy(alpha = ZOOM_BUTTON_ALPHA),
                contentColor = if (enabled) colors.ink else colors.line,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier.size(skin.touchTarget).semantics { contentDescription = label },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // The label says it; the glyph is decoration for screen readers.
                    Text(glyph, style = Gridiron.type.title, modifier = Modifier.clearAndSetSemantics {})
                }
            }
        }
    }
}

// ---- Field dressing (DESIGN §7.2) ----

private fun DrawScope.drawTurf(t: FieldTransform) {
    // The visible field in yards: the whole canvas, not just the fitted window.
    val top = t.yYards(0f)
    val bottom = t.yYards(size.height)
    val left = t.xYards(0f)
    val right = t.xYards(size.width)

    for (k in floor(bottom / STRIPE_YARDS).toInt()..ceil(top / STRIPE_YARDS).toInt()) {
        val y0 = t.y((k * STRIPE_YARDS).toFloat())
        val y1 = t.y(((k + 1) * STRIPE_YARDS).toFloat())
        drawRect(if (k % 2 == 0) DiagramColors.turfA else DiagramColors.turfB, Offset(0f, y1), Size(size.width, y0 - y1))
    }

    val xFrom = t.x(maxOf(left, -FieldGeometry.HALF_WIDTH))
    val xTo = t.x(minOf(right, FieldGeometry.HALF_WIDTH))
    val line = 1.dp.toPx()
    for (k in floor(bottom / STRIPE_YARDS).toInt()..ceil(top / STRIPE_YARDS).toInt()) {
        val yards = k * STRIPE_YARDS
        if (yards == 0) continue // the LOS is drawn on its own
        val alpha = if (yards % MAJOR_LINE_YARDS == 0) MAJOR_LINE_ALPHA else MINOR_LINE_ALPHA
        drawLine(DiagramColors.chalk.copy(alpha = alpha), Offset(xFrom, t.y(yards.toFloat())), Offset(xTo, t.y(yards.toFloat())), line)
    }

    val hashLen = 4.dp.toPx()
    for (yd in ceil(bottom).toInt()..floor(top).toInt()) {
        for (hx in listOf(-FieldGeometry.HASH_X, FieldGeometry.HASH_X)) {
            val cx = t.x(hx)
            drawLine(DiagramColors.chalk.copy(alpha = HASH_ALPHA), Offset(cx - hashLen / 2, t.y(yd.toFloat())), Offset(cx + hashLen / 2, t.y(yd.toFloat())), line)
        }
    }

    val sideline = 3.dp.toPx()
    for (sx in listOf(-FieldGeometry.HALF_WIDTH, FieldGeometry.HALF_WIDTH)) {
        if (sx in left..right) drawLine(DiagramColors.chalk, Offset(t.x(sx), 0f), Offset(t.x(sx), size.height), sideline)
    }

    drawLine(DiagramColors.chalk, Offset(xFrom, t.y(0f)), Offset(xTo, t.y(0f)), 2.dp.toPx())
}

// ---- Zones (DESIGN §7.5) ----

private fun DrawScope.drawZones(d: Diagram, t: FieldTransform, highlight: String?, marks: Map<String, MarkState>) {
    val fillAll = d.zones.size <= 4
    val edge = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    d.zones.forEach { z ->
        val topLeft = Offset(t.x(z.x), t.y(z.y + z.h))
        val size = Size(z.w * t.scale, z.h * t.scale)
        if (fillAll || z.id == highlight) drawRect(DiagramColors.zoneFill, topLeft, size)
        // A tapped or graded zone gets a solid 3dp edge in the mark's colour; missed is dashed.
        val markColor = when (marks[z.id]) {
            MarkState.SELECTED -> DiagramColors.select
            MarkState.CORRECT, MarkState.MISSED -> DiagramColors.gradeCorrect
            MarkState.WRONG -> DiagramColors.gradeRetry
            null -> null
        }
        if (markColor == null) {
            drawRect(DiagramColors.zoneEdge, topLeft, size, style = edge)
        } else {
            val inset = 1.5.dp.toPx()
            val dashed = if (marks[z.id] == MarkState.MISSED) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())) else null
            drawRect(
                markColor,
                topLeft + Offset(inset, inset),
                Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(3.dp.toPx(), pathEffect = dashed),
            )
        }
    }
}

// ---- Paths (DESIGN §7.4) ----

private fun DrawScope.drawRoute(r: DiagramRoute, from: DiagramPlayer, t: FieldTransform) {
    // Repeated waypoints (or a first point on the player's own spot) have no direction;
    // dropping them keeps the corner maths from dividing by zero.
    val pts = (listOf(Offset(t.x(from.x), t.y(from.y))) + r.points.map { Offset(t.x(it.x), t.y(it.y)) })
        .fold(mutableListOf<Offset>()) { acc, o -> if (acc.isEmpty() || (acc.last() - o).getDistance() > 0.5f) acc += o; acc }
    if (pts.size < 2) return
    val (color, width) = when (r.kind) {
        PathKind.RUN -> DiagramColors.routeRun to 4.dp
        PathKind.BLITZ -> DiagramColors.routeBlitz to 4.dp
        PathKind.ROTATE -> DiagramColors.routeRotate to 3.dp
        PathKind.DROP -> DiagramColors.routeDrop to 3.dp
        PathKind.PASS, PathKind.MOTION, PathKind.BLOCK -> DiagramColors.chalk to 3.dp
    }
    val effect = when (r.kind) {
        PathKind.ROTATE -> PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
        PathKind.DROP -> PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
        else -> null
    }
    val path = if (r.kind == PathKind.MOTION) zigZag(pts, 1.2.dp.toPx(), 8.dp.toPx()) else rounded(pts, FieldGeometry.CORNER_RADIUS * t.scale)
    drawPath(path, color, style = Stroke(width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))

    val tip = pts.last()
    val prev = pts[pts.size - 2]
    val angle = atan2(tip.y - prev.y, tip.x - prev.x)
    if (r.kind == PathKind.BLOCK) {
        val half = 6.dp.toPx()
        val nx = -sin(angle) * half
        val ny = cos(angle) * half
        drawLine(color, Offset(tip.x - nx, tip.y - ny), Offset(tip.x + nx, tip.y + ny), width.toPx(), StrokeCap.Round)
    } else {
        drawArrowhead(tip, angle, color, width.toPx(), filled = r.kind == PathKind.BLITZ)
    }
}

private fun DrawScope.drawArrowhead(tip: Offset, angle: Float, color: Color, strokeWidth: Float, filled: Boolean) {
    val len = 9.dp.toPx()
    val spread = 0.45f
    val a = Offset(tip.x - len * cos(angle - spread), tip.y - len * sin(angle - spread))
    val b = Offset(tip.x - len * cos(angle + spread), tip.y - len * sin(angle + spread))
    if (filled) {
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(a.x, a.y); lineTo(b.x, b.y); close() }, color)
    } else {
        drawPath(
            Path().apply { moveTo(a.x, a.y); lineTo(tip.x, tip.y); lineTo(b.x, b.y) },
            color,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** A polyline with each corner rounded, so a curl reads as a hook and an out as an out. */
private fun rounded(pts: List<Offset>, radius: Float): Path = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size - 1) {
        val p = pts[i]
        val a = pts[i - 1]
        val b = pts[i + 1]
        val r = min(radius, min((a - p).getDistance(), (b - p).getDistance()) / 2)
        val inPt = p + (a - p) / (a - p).getDistance() * r
        val outPt = p + (b - p) / (b - p).getDistance() * r
        lineTo(inPt.x, inPt.y)
        quadraticTo(p.x, p.y, outPt.x, outPt.y)
    }
    lineTo(pts.last().x, pts.last().y)
}

/** Pre-snap motion: a zig-zag along the path, ending straight so the arrow reads cleanly. */
private fun zigZag(pts: List<Offset>, amplitude: Float, period: Float): Path = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val a = pts[i - 1]
        val b = pts[i]
        val len = hypot(b.x - a.x, b.y - a.y)
        if (len == 0f) continue
        val ux = (b.x - a.x) / len
        val uy = (b.y - a.y) / len
        val steps = (len / (period / 2)).toInt()
        for (s in 1 until steps) {
            val d = s * period / 2
            val side = if (s % 2 == 0) -amplitude else amplitude
            lineTo(a.x + ux * d - uy * side, a.y + uy * d + ux * side)
        }
        lineTo(b.x, b.y)
    }
}

// ---- Players (DESIGN §7.3) ----

private fun DrawScope.drawPlayer(
    p: DiagramPlayer,
    t: FieldTransform,
    half: Float,
    helmets: Boolean,
    mark: MarkState?,
    reducedMotion: Boolean,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val c = Offset(t.x(p.x), t.y(p.y))
    val grow = if (mark == MarkState.SELECTED && !reducedMotion) SELECTED_SCALE else 1f
    val fill = if (p.side == Side.OFFENSE) DiagramColors.offenseFill else DiagramColors.defenseFill
    val ring = 2.dp.toPx()

    scale(grow, pivot = c) {
        when {
            helmets -> drawHelmet(c, half, fill, faceUp = p.side == Side.OFFENSE)
            p.side == Side.OFFENSE -> drawCircle(fill, half, c)
            else -> drawRoundRect(fill, Offset(c.x - half, c.y - half), Size(half * 2, half * 2), CornerRadius(4.dp.toPx()))
        }
        if (p.side == Side.DEFENSE && !helmets) {
            drawRoundRect(DiagramColors.chalk, Offset(c.x - half, c.y - half), Size(half * 2, half * 2), CornerRadius(4.dp.toPx()), style = Stroke(ring))
        } else {
            drawCircle(DiagramColors.chalk, half, c, style = Stroke(ring))
        }
        val text = measurer.measure(p.label, labelStyle)
        drawText(text, topLeft = Offset(c.x - text.size.width / 2f, c.y - text.size.height / 2f))
    }

    val outer = half * grow + 4.dp.toPx()
    when (mark) {
        MarkState.SELECTED -> drawCircle(DiagramColors.select, outer, c, style = Stroke(3.dp.toPx()))
        MarkState.CORRECT -> { drawCircle(DiagramColors.gradeCorrect, outer, c, style = Stroke(3.dp.toPx())); drawBadge(c, half, DiagramColors.gradeCorrect, check = true) }
        MarkState.WRONG -> { drawCircle(DiagramColors.gradeRetry, outer, c, style = Stroke(3.dp.toPx())); drawBadge(c, half, DiagramColors.gradeRetry, check = false) }
        // A right answer the learner didn't tap. DESIGN §7.3 also pulses it once; that
        // arrives with the animation pass. The dashed ring carries the meaning meanwhile.
        MarkState.MISSED -> drawCircle(
            DiagramColors.gradeCorrect, outer, c,
            style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )
        null -> Unit
    }
}

/** Kid skin: a helmet shell with a facemask on the side the player faces. */
private fun DrawScope.drawHelmet(c: Offset, half: Float, fill: Color, faceUp: Boolean) {
    drawCircle(fill, half, c)
    val dir = if (faceUp) -1f else 1f
    val bar = 2.dp.toPx()
    val mask = DiagramColors.chalk.copy(alpha = 0.85f)
    for (k in 0..1) {
        val y = c.y + dir * half * (0.62f + 0.22f * k)
        val w = half * (0.75f - 0.2f * k)
        drawLine(mask, Offset(c.x - w, y), Offset(c.x + w, y), bar, StrokeCap.Round)
    }
}

/** A small round badge at the top-right of a mark: a check or a dash, so colour is never the only signal. */
private fun DrawScope.drawBadge(c: Offset, half: Float, color: Color, check: Boolean) {
    val r = 7.dp.toPx()
    val b = Offset(c.x + half * 0.85f, c.y - half * 0.85f)
    drawCircle(color, r, b)
    val w = 2.dp.toPx()
    if (check) {
        drawPath(
            Path().apply { moveTo(b.x - r * 0.45f, b.y); lineTo(b.x - r * 0.1f, b.y + r * 0.35f); lineTo(b.x + r * 0.5f, b.y - r * 0.35f) },
            DiagramColors.badgeInk,
            style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    } else {
        drawLine(DiagramColors.badgeInk, Offset(b.x - r * 0.45f, b.y), Offset(b.x + r * 0.45f, b.y), w, StrokeCap.Round)
    }
}

/** A label on a 70%-opaque surface pill, centred on [at] (or starting at it). */
private fun DrawScope.drawPill(measurer: TextMeasurer, text: String, style: TextStyle, bg: Color, at: Offset, anchorLeft: Boolean = false) {
    val layout = measurer.measure(text, style)
    val padX = 6.dp.toPx()
    val padY = 2.dp.toPx()
    val w = layout.size.width + padX * 2
    val h = layout.size.height + padY * 2
    val left = if (anchorLeft) at.x else at.x - w / 2
    val top = at.y - h / 2
    drawRoundRect(bg, Offset(left, top), Size(w, h), CornerRadius(h / 2))
    drawText(layout, topLeft = Offset(left + padX, top + padY))
}
