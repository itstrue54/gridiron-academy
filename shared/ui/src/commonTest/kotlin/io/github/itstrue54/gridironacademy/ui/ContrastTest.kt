package io.github.itstrue54.gridironacademy.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.itstrue54.gridironacademy.ui.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlin.test.*

/** DESIGN §3 and §12: the contrast promises are checked, not eyeballed. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val la = a.luminance(); val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    private fun textPairs(c: GridironColors) = mapOf(
        "ink" to c.ink, "inkDim" to c.inkDim, "offense" to c.offense, "defense" to c.defense,
        "correct" to c.correct, "retry" to c.retry, "link" to c.link,
    )

    @Test
    fun everyTextColourClearsAaOnBothSurfacesInBothThemes() {
        for ((theme, c) in listOf("Daylight" to Daylight, "Floodlight" to Floodlight)) {
            for ((name, fg) in textPairs(c)) {
                for ((bgName, bg) in listOf("surface" to c.surface, "surfaceRaised" to c.surfaceRaised)) {
                    val r = ratio(fg, bg)
                    assertTrue(r >= 4.5f, "$theme $name on $bgName is ${r}:1, needs 4.5:1")
                }
            }
            assertTrue(ratio(c.onGold, c.gold) >= 4.5f, "$theme onGold on gold")
        }
    }

    @Test
    fun measuredValuesInDesignDocStillHold() {
        // Spot checks against the table in DESIGN §3 so the doc and the code can't drift.
        assertEquals(15.35f, ratio(Daylight.ink, Daylight.surface), 0.05f)
        assertEquals(4.71f, ratio(Daylight.defense, Daylight.surface), 0.05f)
        assertEquals(9.60f, ratio(Daylight.onGold, Daylight.gold), 0.05f)
        assertEquals(15.50f, ratio(Floodlight.ink, Floodlight.surface), 0.05f)
    }

    @Test
    fun diagramPathsAndRingsClearThreeToOneOnBothTurfStripes() {
        val graphics = mapOf(
            "chalk" to DiagramColors.chalk, "routeRun" to DiagramColors.routeRun,
            "routeBlitz" to DiagramColors.routeBlitz, "routeRotate" to DiagramColors.routeRotate,
            "routeDrop" to DiagramColors.routeDrop, "zoneEdge" to DiagramColors.zoneEdge,
            "select" to DiagramColors.select, "gradeCorrect" to DiagramColors.gradeCorrect,
            "gradeRetry" to DiagramColors.gradeRetry,
        )
        for ((turfName, turf) in listOf("turfA" to DiagramColors.turfA, "turfB" to DiagramColors.turfB)) {
            for ((name, fg) in graphics) {
                val r = ratio(fg, turf)
                assertTrue(r >= 3f, "$name on $turfName is ${r}:1, needs 3:1")
            }
        }
    }

    @Test
    fun playerLabelsAreReadableOnTheirFills() {
        assertTrue(ratio(DiagramColors.label, DiagramColors.offenseFill) >= 4.5f)
        assertTrue(ratio(DiagramColors.label, DiagramColors.defenseFill) >= 4.5f)
        assertTrue(ratio(DiagramColors.badgeInk, DiagramColors.gradeCorrect) >= 4.5f)
        assertTrue(ratio(DiagramColors.badgeInk, DiagramColors.gradeRetry) >= 4.5f)
    }
}
