package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class ReviewTest {
    private val atBoxTwo = Progress(review = mapOf("s" to ReviewCard("s", "down", 2, 110)))

    @Test fun correctOnNotDueCardDoesNotMove() {
        assertEquals(atBoxTwo, Review.answered(atBoxTwo, "s", true, 109))
    }

    @Test fun missOnNotDueCardGoesToBoxOne() {
        val p = Review.answered(atBoxTwo, "s", false, 109)
        assertEquals(ReviewCard("s", "down", 1, 110), p.review["s"])
    }

    @Test fun correctOnDueCardMovesUp() {
        val p = Review.answered(atBoxTwo, "s", true, 110)
        assertEquals(ReviewCard("s", "down", 3, 114), p.review["s"])
    }
}
