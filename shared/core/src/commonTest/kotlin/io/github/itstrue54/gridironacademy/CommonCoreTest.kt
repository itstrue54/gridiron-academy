package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.engine.*
import io.github.itstrue54.gridironacademy.progress.*
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

    @Test
    fun afterLessonKeepsBestScoreQueuesMissesAndCountsTheDay() {
        val p = Review.afterLesson(Progress(), "l1", 75, mapOf("s3" to "downs", "s5" to null), today = 100)
        assertEquals(75, p.lessons.getValue("l1").bestPct)
        assertEquals(setOf("s3", "s5"), p.review.keys)
        assertEquals(101, p.review.getValue("s3").dueDay)
        assertEquals(1, p.streakDays)
        val again = Review.afterLesson(p, "l1", 60, emptyMap(), today = 101)
        assertEquals(75, again.lessons.getValue("l1").bestPct)
        assertEquals(2, again.streakDays)
    }

    @Test
    fun ageBandsPickTrackAndRules() {
        assertEquals(Track.KID, AgeBand.AGE_10_12.track)
        assertEquals(Track.ADULT, AgeBand.AGE_13_17.track)
        assertTrue(AgeBand.AGE_6_7.preReader)
        assertFalse(AgeBand.AGE_8_9.preReader)
        assertFalse(AgeBand.AGE_8_9.offeredPlacement)
        assertTrue(AgeBand.AGE_10_12.child)
        assertFalse(AgeBand.AGE_13_17.child)
    }

    @Test
    fun saveRoundTripsThroughJson() {
        val save = LearnerSave(profile = Profile("Leo", AgeBand.AGE_8_9), progress = Progress(xp = 40, streakDays = 3))
        val text = ContentLoader.json.encodeToString(LearnerSave.serializer(), save)
        assertTrue("\"8-9\"" in text, "age band is stored by its label: $text")
        assertEquals(save, ContentLoader.json.decodeFromString(LearnerSave.serializer(), text))
    }
}
