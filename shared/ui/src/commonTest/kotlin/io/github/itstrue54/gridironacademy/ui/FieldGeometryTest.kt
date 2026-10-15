package io.github.itstrue54.gridironacademy.ui

import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.ui.field.FieldGeometry
import io.github.itstrue54.gridironacademy.ui.field.FieldZoom
import kotlin.test.*

class FieldGeometryTest {
    private fun p(id: String, x: Float, y: Float, side: Side = Side.OFFENSE) = DiagramPlayer(id, id.uppercase(), side, x, y)

    @Test
    fun windowIsTightBoundsPlusMarginAndAlwaysShowsTheLos() {
        val w = FieldGeometry.window(Diagram(players = listOf(p("a", -10f, 5f), p("b", 12f, 9f))))
        assertEquals(-13f, w.xMin)
        assertEquals(15f, w.xMax)
        assertEquals(-3f, w.yMin) // the LOS (y = 0) minus the margin, though nobody stands behind it
        assertEquals(12f, w.yMax)
    }

    @Test
    fun windowNeverCropsPastTheSidelinesAndShiftsInstead() {
        val w = FieldGeometry.window(Diagram(players = listOf(p("wide", 25f, 0f), p("qb", 10f, -5f))))
        assertEquals(FieldGeometry.HALF_WIDTH, w.xMax)
        assertEquals(10f - FieldGeometry.MARGIN - (25f + FieldGeometry.MARGIN - FieldGeometry.HALF_WIDTH), w.xMin, 0.001f)
    }

    @Test
    fun onePlayerGetsTheMinimumWindowNotAGiantCloseUp() {
        val w = FieldGeometry.window(Diagram(players = listOf(p("qb", 0f, -5f))))
        assertEquals(FieldGeometry.MIN_X_SPAN, w.xSpan, 0.001f)
        assertTrue(w.ySpan >= FieldGeometry.MIN_Y_SPAN)
    }

    @Test
    fun emptyDiagramShowsTheWholeWidthFromBehindToDepth() {
        val w = FieldGeometry.window(Diagram(depth = 20f, behind = 8f))
        assertEquals(-FieldGeometry.HALF_WIDTH, w.xMin)
        assertEquals(-8f, w.yMin)
        assertEquals(20f, w.yMax)
    }

    @Test
    fun zonesSpanDownfieldFromTheirCorner() {
        // Authored like kid-1's end-zone step: y is the near edge, h runs downfield.
        val w = FieldGeometry.window(Diagram(zones = listOf(DiagramZone("ez", "End zone", -26.5f, 10f, 53f, 4.5f))))
        assertEquals(14.5f + FieldGeometry.MARGIN, w.yMax)
    }

    @Test
    fun transformCentresAndPutsDownfieldAtTheTop() {
        val w = FieldGeometry.window(Diagram(players = listOf(p("a", -10f, 0f), p("b", 10f, 10f))))
        val t = FieldGeometry.transform(w, 520f, 1000f)
        assertEquals(20f, t.scale, 0.001f) // 520 px / 26 yd; the height has room to spare
        assertTrue(t.y(10f) < t.y(0f), "downfield should be higher on screen")
        assertEquals((1000f - w.ySpan * 20f) / 2, t.offsetY, 0.001f)
        assertEquals(-10f, t.xYards(t.x(-10f)), 0.001f)
        assertEquals(7f, t.yYards(t.y(7f)), 0.001f)
    }

    @Test
    fun cardAspectIsClampedToThreeFourToFourThree() {
        val wide = FieldGeometry.window(Diagram(players = listOf(p("l", -26f, 0f), p("r", 26f, 0f))))
        assertEquals(4f / 3f, FieldGeometry.cardAspect(wide))
        val deep = FieldGeometry.window(Diagram(players = listOf(p("qb", 0f, -5f), p("fs", 0f, 30f))))
        assertEquals(3f / 4f, FieldGeometry.cardAspect(deep))
    }

    @Test
    fun hitTestPicksTheNearestCentreWithinTheTarget() {
        val players = listOf(p("a", 0f, 0f), p("b", 1f, 0f))
        val t = FieldGeometry.transform(FieldGeometry.window(Diagram(players = players)), 400f, 400f)
        assertEquals("b", FieldGeometry.hitTest(players, t, t.x(0.8f), t.y(0f), 48f)?.id)
        assertNull(FieldGeometry.hitTest(players, t, t.x(8f), t.y(0f), 24f))
    }

    @Test
    fun zoneAtPicksTheSmallestZoneUnderTheTap() {
        val zones = listOf(DiagramZone("deep", "Deep", -20f, 10f, 40f, 15f), DiagramZone("hole", "Hole", -3f, 12f, 6f, 4f))
        val t = FieldGeometry.transform(FieldGeometry.window(Diagram(zones = zones)), 600f, 600f)
        assertEquals("hole", FieldGeometry.zoneAt(zones, t, t.x(0f), t.y(14f))?.id)
        assertEquals("deep", FieldGeometry.zoneAt(zones, t, t.x(15f), t.y(14f))?.id)
        assertNull(FieldGeometry.zoneAt(zones, t, t.x(0f), t.y(2f)))
    }

    @Test
    fun crowdedFlagsPlayersTooCloseToTapApart() {
        val players = listOf(p("lg", -1.2f, 0f), p("c", 0f, 0f))
        val t = FieldGeometry.transform(FieldGeometry.window(Diagram(players = players)), 360f, 360f)
        assertTrue(FieldGeometry.crowded(players, t, 40f))
        assertFalse(FieldGeometry.crowded(players, t, 10f))
    }

    @Test
    fun marksShrinkToTheTightestSpacingButNotBelowTheFloor() {
        val spread = listOf(p("a", -10f, 0f), p("b", 10f, 0f))
        val t1 = FieldGeometry.transform(FieldGeometry.window(Diagram(players = spread)), 520f, 520f)
        assertEquals(44f, FieldGeometry.markSize(spread, t1, preferredPx = 44f, minPx = 36f, gapPx = 4f))

        val line = listOf(p("lg", -1.6f, 0f), p("c", 0f, 0f), p("rg", 1.6f, 0f), p("x", -25f, 0f), p("z", 25f, 0f))
        val t2 = FieldGeometry.transform(FieldGeometry.window(Diagram(players = line)), 656f, 656f)
        val spacing = 1.6f * t2.scale
        assertEquals(spacing - 4f, FieldGeometry.markSize(line, t2, preferredPx = 44f, minPx = 10f, gapPx = 4f), 0.01f)
        assertEquals(36f, FieldGeometry.markSize(line, t2, preferredPx = 44f, minPx = 36f, gapPx = 4f))
    }

    @Test
    fun pinchKeepsThePointUnderTheFingersStill() {
        val z = FieldZoom().zoomAbout(2f, 300f, 200f, 600f, 400f)
        assertEquals(2f, z.scale)
        // Canvas point (300, 200) at 1x must still be drawn at (300, 200).
        assertEquals(300f, 300f * z.scale + z.panX, 0.001f)
        assertEquals(200f, 200f * z.scale + z.panY, 0.001f)
    }

    @Test
    fun zoomStaysBetweenOneAndMaxAndNeverShowsPastTheEdge() {
        assertEquals(FieldGeometry.MAX_ZOOM, FieldZoom().zoomAbout(10f, 0f, 0f, 600f, 400f).scale)
        assertEquals(FieldZoom(), FieldZoom(2f, -300f, -200f).zoomAbout(0.1f, 50f, 50f, 600f, 400f))
        val panned = FieldZoom(2f).panBy(500f, -5000f, 600f, 400f)
        assertEquals(0f, panned.panX) // can't drag the field's left edge into view
        assertEquals(-400f, panned.panY) // nor past its bottom: 400 * (1 - 2)
        assertEquals(FieldZoom(), FieldZoom().panBy(-80f, 40f, 600f, 400f)) // 1x never pans
    }

    @Test
    fun zoomedTransformMapsYardsThroughTheZoom() {
        val w = FieldGeometry.window(Diagram(players = listOf(p("a", -10f, 0f), p("b", 10f, 10f))))
        val base = FieldGeometry.transform(w, 520f, 520f)
        val z = FieldZoom(2f, -260f, -100f)
        val t = z.apply(base)
        assertEquals(base.x(3f) * 2f - 260f, t.x(3f), 0.001f)
        assertEquals(base.y(4f) * 2f - 100f, t.y(4f), 0.001f)
        assertEquals(3f, t.xYards(t.x(3f)), 0.001f)
    }

    @Test
    fun zoomingInThenOutLandsExactlyOnOneX() {
        val z = FieldZoom().zoomAbout(FieldGeometry.ZOOM_STEP, 300f, 200f, 600f, 400f)
            .zoomAbout(1f / FieldGeometry.ZOOM_STEP, 300f, 200f, 600f, 400f)
        assertEquals(FieldZoom(), z)
        assertFalse(z.zoomed)
    }

    @Test
    fun resizingKeepsTheRelativeViewAndClamps() {
        val z = FieldZoom(2f, -300f, -100f).resized(600f, 400f, 400f, 600f)
        assertEquals(-200f, z.panX, 0.001f)
        assertEquals(-150f, z.panY, 0.001f)
        // A pan scaled with the canvas stays inside the field at the new size.
        val shrunk = FieldZoom(4f, -1500f, 0f).resized(600f, 400f, 100f, 400f)
        assertEquals(-250f, shrunk.panX, 0.001f)
        assertTrue(shrunk.panX >= 100f * (1 - 4f))
        // A first size (nothing to scale from) just clamps.
        assertEquals(0f, FieldZoom(2f, 50f, 0f).resized(0f, 0f, 600f, 400f).panX)
    }

    @Test
    fun aSharedWindowCoversPreSnapAndReveal() {
        val pre = Diagram(players = listOf(p("fs", 0f, 12f, Side.DEFENSE)))
        val reveal = Diagram(
            players = pre.players,
            routes = listOf(DiagramRoute("fs", listOf(Pt(14f, 16f)), PathKind.ROTATE)),
        )
        val both = FieldGeometry.window(pre, reveal)
        val r = FieldGeometry.window(reveal)
        assertEquals(r.xMax, both.xMax)
        assertEquals(minOf(FieldGeometry.window(pre).xMin, r.xMin), both.xMin)
    }

    @Test
    fun alongWalksTheRouteByDistance() {
        val pts = listOf(Pt(0f, 10f), Pt(10f, 10f)) // 10 up, then 10 across: 20 yards
        assertEquals(Pt(0f, 0f), FieldGeometry.along(0f, 0f, pts, 0f))
        assertEquals(Pt(0f, 5f), FieldGeometry.along(0f, 0f, pts, 0.25f))
        assertEquals(Pt(5f, 10f), FieldGeometry.along(0f, 0f, pts, 0.75f))
        assertEquals(Pt(10f, 10f), FieldGeometry.along(0f, 0f, pts, 1f))
        assertEquals(Pt(3f, 4f), FieldGeometry.along(3f, 4f, listOf(Pt(3f, 4f)), 0.5f), "a path with no length stays put")
    }

    @Test
    fun revealPathsStartInTurnAndFinishTogether() {
        val n = 4
        val d = 900
        assertEquals(0f, FieldGeometry.staggered(0f, 0, n, d))
        // At 60ms the first path has started and the second hasn't.
        val p60 = 60f / d
        assertTrue(FieldGeometry.staggered(p60, 0, n, d) > 0f)
        assertEquals(0f, FieldGeometry.staggered(p60, 1, n, d), 1e-4f)
        // Everyone arrives at the end.
        (0 until n).forEach { assertEquals(1f, FieldGeometry.staggered(1f, it, n, d)) }
    }
}
