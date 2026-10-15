package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.engine.*
import io.github.itstrue54.gridironacademy.model.*
import kotlin.test.*

/**
 * Platform-free checks in commonTest. Today they run on the JVM only; iOS runs them once
 * the M5 macOS CI builds the iOS targets.
 */
class CommonCoreTest {
    @Test
    fun scoresRoundHalfUpLikeMathRoundDid() {
        // 1 of 8 is 12.5%: java.lang.Math.round gave 13, and roundToInt must too.
        val qs = (1..8).map { TrueFalse("tf$it", "s$it", answer = true, explanation = "e") }
        val round = SpeedRound(qs)
        round.answer(Answer.Bool(true), 0)
        repeat(7) { round.answer(Answer.Bool(false), 0) }
        assertEquals(13, round.accuracyPct)
    }

    @Test
    fun rngSequenceIsFixedForASeed() {
        // Saved games and synced events replay from seeds, so this must never drift.
        // Golden values, computed independently from the xorshift64* definition.
        val r = Rng(42)
        assertEquals(listOf(800, 749, 923, 367, 339), List(5) { r.nextInt(1000) })
    }

    @Test
    fun anEmptyStepEarnsZeroInsteadOfNaN() {
        // Math.round(NaN) used to hide this as 0; roundToInt would throw instead.
        val empty = Match("m", "p", pairs = emptyList(), explanation = "e")
        assertEquals(0f, Grader.grade(empty, Answer.Matches(emptyMap())).credit)
    }
}
