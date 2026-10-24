package io.github.itstrue54.gridironacademy.ui

import io.github.itstrue54.gridironacademy.model.BallSpot
import io.github.itstrue54.gridironacademy.model.Pt
import io.github.itstrue54.gridironacademy.ui.field.FieldGeometry
import kotlin.test.*

class BallFlightTest {
    // The QB stands at (0, -5); the receiver runs from (10, 0) straight up 20 yards.
    private val spot = { id: String, t: Float ->
        when (id) {
            "qb" -> Pt(0f, -5f)
            "x" -> Pt(10f, 20f * t)
            else -> null
        }
    }
    private val pass = listOf(BallSpot("qb"), BallSpot("x", 0.8f))

    @Test fun `no ball, nothing drawn`() = assertNull(FieldGeometry.ballAt(emptyList(), 0.5f, spot))

    @Test fun `the passer holds it until the throw`() = assertEquals(Pt(0f, -5f), FieldGeometry.ballAt(pass, 0.1f, spot))

    @Test fun `it arrives with the receiver and then rides along`() {
        assertEquals(Pt(10f, 16f), FieldGeometry.ballAt(pass, 0.8f, spot))
        assertEquals(Pt(10f, 18f), FieldGeometry.ballAt(pass, 0.9f, spot))
    }

    @Test fun `a fumble lies on the ground until someone picks it up`() {
        val fumble = listOf(BallSpot("x"), BallSpot(null, 0.5f, 3f, 12f), BallSpot("qb", 0.9f))
        assertEquals(Pt(3f, 12f), FieldGeometry.ballAt(fumble, 0.6f, spot))
        assertEquals(Pt(0f, -5f), FieldGeometry.ballAt(fumble, 1f, spot))
    }

    @Test fun `in the air it is between the two`() {
        val mid = assertNotNull(FieldGeometry.ballAt(pass, 0.7f, spot))
        assertTrue(mid.x in 0f..10f && mid.y in -5f..16f, "$mid")
        assertNotEquals(Pt(0f, -5f), mid)
    }
}
