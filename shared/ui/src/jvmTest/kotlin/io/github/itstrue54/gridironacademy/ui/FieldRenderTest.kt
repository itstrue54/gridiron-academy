package io.github.itstrue54.gridironacademy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.model.*
import kotlinx.serialization.decodeFromString
import io.github.itstrue54.gridironacademy.ui.field.FieldDiagram
import io.github.itstrue54.gridironacademy.ui.field.FieldGeometry
import io.github.itstrue54.gridironacademy.ui.field.FieldTransform
import io.github.itstrue54.gridironacademy.ui.field.FieldZoom
import io.github.itstrue54.gridironacademy.ui.field.FieldZoomState
import io.github.itstrue54.gridironacademy.ui.theme.DiagramColors
import io.github.itstrue54.gridironacademy.ui.field.MarkState
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.GridironTheme
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.jupiter.api.Timeout
import kotlin.math.abs
import java.io.File
import kotlin.test.*

/**
 * Renders diagrams headlessly to build/field-renders/ so a person (or a screenshot test
 * later, SPEC 13.4) can look at them, and checks pixels where players and paths must be.
 */
class FieldRenderTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val out = File("build/field-renders").apply { mkdirs() }

    private fun steps(): List<Step> = content.curriculum.levels.flatMap { content.unitsOf(it) }.flatMap { it.lessons }.flatMap { it.steps } +
        ContentLoader.json.decodeFromString<CourseUnit>(ClasspathContentSource().read("preview/l7-disguise.json")!!).lessons.flatMap { it.steps }

    /** Reads the rendered colour at a field position in yards, nudged by pixels. */
    private class Rendered(val bitmap: Bitmap, val t: FieldTransform) {
        fun at(x: Float, y: Float, dxPx: Int = 0, dyPx: Int = 0): Color =
            Color(bitmap.getColor(PAD_PX + t.x(x).toInt() + dxPx, PAD_PX + t.y(y).toInt() + dyPx))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun render(
        name: String,
        diagram: Diagram,
        skin: Skin,
        dark: Boolean,
        marks: Map<String, MarkState> = emptyMap(),
        zoom: FieldZoom = FieldZoom(),
        motion: Float? = null,
        film: Boolean = false,
    ): Rendered {
        val zoomState = FieldZoomState().apply { value = zoom }
        val image = ImageComposeScene(width = WIDTH_PX, height = 1000, density = Density(DENSITY)) {
            GridironTheme(skin = skin, dark = dark) {
                Box(Modifier.fillMaxSize().background(Gridiron.colors.surface).padding(PAD_DP.dp)) {
                    if (film) {
                        FieldDiagram(diagram, description = name, zoom = zoomState, motion = motion, motionKinds = FieldGeometry.FILM_KINDS, motionStaggerMs = 0)
                    } else {
                        FieldDiagram(diagram, description = name, marks = marks, zoom = zoomState, motion = motion)
                    }
                }
            }
        }.use { it.render() }
        File(out, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        val window = FieldGeometry.window(diagram)
        val w = (WIDTH_PX - 2 * PAD_PX).toFloat()
        return Rendered(Bitmap.makeFromImage(image), zoom.apply(FieldGeometry.transform(window, w, w / FieldGeometry.cardAspect(window))))
    }

    private fun assertNear(expected: Color, actual: Color, what: String) {
        val d = maxOf(abs(expected.red - actual.red), abs(expected.green - actual.green), abs(expected.blue - actual.blue))
        assertTrue(d < 0.04f, "$what: expected $expected, got $actual")
    }

    @Test
    fun disguisePreSnapAndRevealInBothSkinsAndThemes() {
        val rd = steps().filterIsInstance<ReadDefense>().first()
        render("disguise-presnap-adult-dark", rd.preSnap, Skin.Adult, dark = true)
        render("disguise-reveal-adult-light", rd.reveal, Skin.Adult, dark = false)
        render("disguise-reveal-kid-light", rd.reveal, Skin.Kid, dark = false)
    }

    /** Film mode (DESIGN 7.8): every teaching card with a ball, at the snap, midway and the end. */
    @Test
    fun teachingPlaysRunWithTheBall() {
        val films = steps().filterIsInstance<Learn>().filter { (it.diagram?.ball?.size ?: 0) > 0 }
        assertTrue(films.size >= 17, "found ${films.size}")
        for (step in films) for (m in listOf(0f, 0.5f, 1f)) {
            render("film-${step.id}-${(m * 100).toInt()}", step.diagram!!, Skin.Kid, dark = false, motion = m, film = true)
        }
    }

    @Test
    fun tapStepsWithZonesAndGradingMarks() {
        val taps = steps().filterIsInstance<DiagramTap>()
        val zoneStep = taps.first { it.diagram.zones.isNotEmpty() }
        render("zones-kid", zoneStep.diagram, Skin.Kid, dark = false)
        val playerStep = taps.first { it.diagram.players.size > 3 }
        val ids = playerStep.diagram.players.map { it.id }
        render(
            "graded-adult", playerStep.diagram, Skin.Adult, dark = true,
            marks = mapOf(ids[0] to MarkState.CORRECT, ids[1] to MarkState.WRONG, ids[2] to MarkState.MISSED, ids[3] to MarkState.SELECTED),
        )
    }

    @Test
    fun everyPathKind() {
        val qb = DiagramPlayer("qb", "QB", Side.OFFENSE, 0f, -5f)
        val players = listOf(
            qb,
            DiagramPlayer("x", "X", Side.OFFENSE, -18f, 0f),
            DiagramPlayer("z", "Z", Side.OFFENSE, 18f, -1f),
            DiagramPlayer("rb", "RB", Side.OFFENSE, 2f, -7f),
            DiagramPlayer("lt", "LT", Side.OFFENSE, -4f, 0f),
            DiagramPlayer("fs", "FS", Side.DEFENSE, 0f, 14f),
            DiagramPlayer("w", "W", Side.DEFENSE, 4f, 5f),
            DiagramPlayer("c", "C", Side.DEFENSE, -18f, 7f),
        )
        val routes = listOf(
            DiagramRoute("x", listOf(Pt(-18f, 10f), Pt(-12f, 10f)), PathKind.PASS, "Dig"),
            DiagramRoute("z", listOf(Pt(8f, -1f)), PathKind.MOTION, "Jet"),
            DiagramRoute("rb", listOf(Pt(2f, -2f), Pt(6f, 3f)), PathKind.RUN),
            DiagramRoute("lt", listOf(Pt(-5f, 2f)), PathKind.BLOCK),
            DiagramRoute("w", listOf(Pt(1f, 0.5f), Pt(0f, -3f)), PathKind.BLITZ),
            DiagramRoute("fs", listOf(Pt(10f, 12f)), PathKind.ROTATE),
            DiagramRoute("c", listOf(Pt(-16f, 14f)), PathKind.DROP),
        )
        val zones = listOf(DiagramZone("deep", "Deep third", -8f, 12f, 16f, 8f))
        val r = render("path-kinds-adult", Diagram(players, routes, zones, caption = "Every path kind"), Skin.Adult, dark = false)

        // Below each label, inside the mark: the side's fill. Offense LT, defense W.
        assertNear(DiagramColors.offenseFill, r.at(-4f, 0f, dyPx = 14), "LT fill")
        assertNear(DiagramColors.defenseFill, r.at(4f, 5f, dyPx = 14), "W fill")
        // The run path, halfway up its first leg; open turf away from everything.
        assertNear(DiagramColors.routeRun, r.at(2f, -4.5f), "RB run path")
        val turf = r.at(20f, -3f)
        assertTrue(turf == DiagramColors.turfA || turf == DiagramColors.turfB, "open turf, got $turf")
    }

    @Test
    fun zoomingInSeparatesACrowdedLine() {
        // Five linemen 1.6 yards apart with receivers at the sidelines: crowded on a phone.
        val ol = listOf(-3.2f, -1.6f, 0f, 1.6f, 3.2f).mapIndexed { i, x -> DiagramPlayer("ol$i", "${70 + i}", Side.OFFENSE, x, 0f) }
        val wide = listOf(DiagramPlayer("x", "X", Side.OFFENSE, -24f, 0f), DiagramPlayer("z", "Z", Side.OFFENSE, 24f, 0f))
        val d = Diagram(ol + wide)

        val flat = render("crowded-line-1x", d, Skin.Adult, dark = false)
        // At 1x the marks overlap, so halfway between two linemen is a mark, not turf.
        val between = flat.at(0.8f, 0f, dyPx = 8)
        assertTrue(between != DiagramColors.turfA && between != DiagramColors.turfB, "at 1x linemen overlap, got turf")

        val w = (WIDTH_PX - 2 * PAD_PX).toFloat()
        val base = FieldGeometry.transform(FieldGeometry.window(d), w, w / FieldGeometry.cardAspect(FieldGeometry.window(d)))
        val zoom = FieldZoom().zoomAbout(3f, base.x(0f), base.y(0f), w, w / FieldGeometry.cardAspect(FieldGeometry.window(d)))
        val near = render("crowded-line-3x", d, Skin.Adult, dark = false, zoom = zoom)
        val gap = near.at(0.8f, 0f, dyPx = 8)
        assertTrue(gap == DiagramColors.turfA || gap == DiagramColors.turfB, "at 3x linemen have turf between them, got $gap")
        assertNear(DiagramColors.offenseFill, near.at(0f, 0f, dyPx = 14), "centre's fill at 3x")

        // X and Z are off the sides at 3x: each gets an offense-coloured marker on its edge,
        // level with the line, instead of vanishing (DESIGN 7.10).
        val lineY = near.t.y(0f).toInt() + PAD_PX
        val inPx = (14 * DENSITY).toInt() // inside the pill, past the arrow
        assertNear(DiagramColors.offenseFill, Color(near.bitmap.getColor(PAD_PX + inPx, lineY)), "left edge marker for X")
        assertNear(DiagramColors.offenseFill, Color(near.bitmap.getColor(WIDTH_PX - PAD_PX - inPx, lineY)), "right edge marker for Z")
        // At 1x everyone is in view: no marker over the turf at the edge.
        val edge1x = Color(flat.bitmap.getColor(PAD_PX + inPx, flat.t.y(0f).toInt() + PAD_PX - (20 * DENSITY).toInt()))
        assertTrue(edge1x == DiagramColors.turfA || edge1x == DiagramColors.turfB, "no marker at 1x, got $edge1x")
    }

    @Test
    fun repeatedWaypointsStillDrawTheRoute() {
        val x = DiagramPlayer("x", "X", Side.OFFENSE, 0f, 0f)
        val route = DiagramRoute("x", listOf(Pt(0f, 0f), Pt(0f, 6f), Pt(0f, 6f), Pt(4f, 6f)), PathKind.PASS)
        val r = render("repeated-waypoints", Diagram(listOf(x), listOf(route)), Skin.Adult, dark = false)
        assertNear(DiagramColors.chalk, r.at(0f, 3.5f), "stem of the route")
        assertNear(DiagramColors.chalk, r.at(2f, 6f), "break of the route")
    }

    @Test
    @Timeout(60)
    @OptIn(ExperimentalComposeUiApi::class)
    fun aZeroHeightFieldDrawsNothingAndReturns() {
        ImageComposeScene(width = 400, height = 400) {
            GridironTheme(skin = Skin.Adult, dark = false) {
                Column(Modifier.height(0.dp)) {
                    FieldDiagram(steps().filterIsInstance<ReadDefense>().first().preSnap, description = "none", fitToCard = false)
                }
            }
        }.use { it.render() }
    }

    private companion object {
        const val WIDTH_PX = 720
        const val DENSITY = 2f
        const val PAD_DP = 16
        const val PAD_PX = (PAD_DP * DENSITY).toInt()
    }

    @Test
    fun theRevealMovesDefendersAndLeavesGhosts() {
        val rd = steps().filterIsInstance<ReadDefense>().first()
        val fs = rd.reveal.players.first { it.id == "fs" }
        val end = rd.reveal.routes.first { it.playerId == "fs" }.points.last()
        render("reveal-half-adult", rd.reveal, Skin.Adult, dark = false, motion = 0.5f)
        val done = render("reveal-end-adult", rd.reveal, Skin.Adult, dark = false, motion = 1f)
        assertNear(DiagramColors.defenseFill, done.at(end.x, end.y, dyPx = 14), "free safety arrived at the end of its rotation")
        // The ghost at the start is a faded mark: neither the full fill nor bare turf.
        val ghost = done.at(fs.x, fs.y, dyPx = 14)
        assertTrue(ghost != DiagramColors.defenseFill && ghost != DiagramColors.turfA && ghost != DiagramColors.turfB, "ghost at the pre-snap spot, got $ghost")
    }
}
