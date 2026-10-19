package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class DailyPracticeTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val level = content.level("adult-1")
    private val graded = content.unitsOf(level).flatMap { u -> u.lessons.flatMap { l -> l.steps.filter { it !is Learn } } }
    private var seq = 0L
    private fun a(step: String, day: Long, pct: Float, ctx: AttemptContext = AttemptContext.LESSON) =
        AttemptEvent("me", "d", seq++, step, ctx, pct, epochDay = day, at = day * 1000 + seq)

    @Test fun `nothing to practice for a new learner`() {
        val p = DailyPractice.pick(Progress(), emptyList(), content, level, 100, 1)
        assertEquals(emptyList(), p.steps)
        assertFalse(p.urgent)
    }

    @Test fun `due cards come first and make practice the main action`() {
        val missed = graded[3]
        val progress = Review.miss(Progress(), missed.id, missed.concept, 99)
        val p = DailyPractice.pick(progress, listOf(a(missed.id, 99, 0f)), content, level, 100, 1)
        assertEquals(missed.id, p.steps.first().id)
        assertTrue(p.urgent)
        // Not due yet the day it was missed.
        assertFalse(DailyPractice.pick(progress, listOf(a(missed.id, 99, 0f)), content, level, 99, 1).urgent)
    }

    @Test fun `concepts still being learned top it up, never more than ten`() {
        // Every adult-1 step answered wrong once: everything is Learning.
        val attempts = graded.map { a(it.id, 90, 0f) }
        val p = DailyPractice.pick(Progress(), attempts, content, level, 100, 1)
        assertTrue(p.steps.size in PracticeRules.MIN_ITEMS..PracticeRules.MAX_ITEMS, "${p.steps.size}")
        assertFalse(p.urgent, "learning concepts alone don't take over Home")
        assertEquals(p.steps.size, p.steps.map { it.concept }.distinct().size, "one question per concept")
        assertTrue(p.steps.all { s -> attempts.any { it.stepId == s.id } }, "only what was taught")
    }

    @Test fun `the same day gives the same set, another day may not`() {
        val attempts = graded.map { a(it.id, 90, 0f) }
        val one = DailyPractice.pick(Progress(), attempts, content, level, 100, 7).steps.map { it.id }
        assertEquals(one, DailyPractice.pick(Progress(), attempts, content, level, 100, 7).steps.map { it.id })
        val days = (100L..110L).map { d -> DailyPractice.pick(Progress(), attempts, content, level, d, 7).steps.map { it.id } }.toSet()
        assertTrue(days.size > 1, "practice varies from day to day")
    }

    @Test fun `a review answer today marks practice done`() {
        assertFalse(DailyPractice.doneToday(listOf(a("x", 100, 1f)), 100))
        assertTrue(DailyPractice.doneToday(listOf(a("x", 100, 1f, AttemptContext.REVIEW)), 100))
        assertFalse(DailyPractice.doneToday(listOf(a("x", 99, 1f, AttemptContext.REVIEW)), 100))
    }
}
