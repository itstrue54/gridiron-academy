package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.engine.DiagramDescriber
import io.github.itstrue54.gridironacademy.model.*
import kotlinx.serialization.encodeToString
import kotlin.test.*

/** Goal lines and end zones on a diagram, and turning the line of scrimmage off (DESIGN 7.2). */
class EndZoneTest {
    private val json = ContentLoader.json

    @Test fun `a diagram without them has no goal lines and keeps its line of scrimmage`() {
        val d = json.decodeFromString<Diagram>("""{"players": [], "depth": 10}""")
        assertNull(d.goalAhead)
        assertNull(d.goalBehind)
        assertTrue(d.lineOfScrimmage)
    }

    @Test fun `defaults are never written, so untouched content keeps its fingerprint`() {
        val text = json.encodeToString(Diagram(depth = 10f))
        assertFalse("goal" in text, text)
        assertFalse("lineOfScrimmage" in text, text)
    }

    @Test fun `goal lines and a hidden line of scrimmage survive a round trip`() {
        val d = json.decodeFromString<Diagram>("""{"goalAhead": 50, "goalBehind": -50, "lineOfScrimmage": false}""")
        assertEquals(50f, d.goalAhead)
        assertEquals(-50f, d.goalBehind)
        assertFalse(d.lineOfScrimmage)
        assertEquals(d, json.decodeFromString<Diagram>(json.encodeToString(d)))
    }

    private fun errorsFor(vararg ds: Diagram): List<String> {
        val unit = CourseUnit("u", "u", listOf(Lesson("l", "l", steps = ds.mapIndexed { i, d -> Learn("s$i", "t", "b", d) })))
        return ContentValidator.validateUnit(unit, kid = false, glossaryIds = emptySet())
    }

    @Test fun `validator wants goal lines on the right side of the line and a real field between them`() {
        val errs = errorsFor(
            Diagram(goalAhead = -3f),
            Diagram(goalBehind = 4f),
            Diagram(goalAhead = 40f, goalBehind = -40f),
        )
        assertTrue(errs.any { "s0" in it && "goalAhead" in it }, errs.joinToString("\n"))
        assertTrue(errs.any { "s1" in it && "goalBehind" in it }, errs.joinToString("\n"))
        assertTrue(errs.any { "s2" in it && "100 yards" in it }, errs.joinToString("\n"))
        val fine = errorsFor(Diagram(goalAhead = 2f), Diagram(goalBehind = -2f), Diagram(goalAhead = 50f, goalBehind = -50f))
        assertTrue(fine.none { "goal" in it || "end line" in it }, fine.joinToString("\n"))
    }

    @Test fun `validator rejects a player past an end line`() {
        val deep = DiagramPlayer("s", "S", Side.DEFENSE, 0f, 21f)
        val back = DiagramPlayer("rb", "RB", Side.OFFENSE, 0f, -13f)
        val errs = errorsFor(Diagram(players = listOf(deep), goalAhead = 10f), Diagram(players = listOf(back), goalBehind = -2f))
        assertTrue(errs.any { "s0" in it && "past the end line" in it }, errs.joinToString("\n"))
        assertTrue(errs.any { "s1" in it && "past the end line" in it }, errs.joinToString("\n"))
    }

    @Test fun `the screen reader hears each end zone without saying whose it is`() {
        val d = Diagram(goalAhead = 50f, goalBehind = -50f, lineOfScrimmage = false)
        assertEquals(
            "End zone, from 50 yards up the field to 60 yards up the field. " +
                "End zone, from 50 yards behind the line to 60 yards behind the line.",
            DiagramDescriber.describe(d),
        )
    }

    @Test fun `the end zone is read before the shaded areas`() {
        val d = Diagram(zones = listOf(DiagramZone("z", "Z", -5f, 0f, 10f, 5f)), goalAhead = 10f)
        val said = DiagramDescriber.describe(d)
        assertTrue(said.indexOf("End zone") < said.indexOf("Shaded area"), said)
    }
}
