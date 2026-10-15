package io.github.itstrue54.gridironacademy.ui

import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.ui.field.FieldGeometry
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
}
