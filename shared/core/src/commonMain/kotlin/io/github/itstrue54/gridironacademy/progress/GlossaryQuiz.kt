package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.engine.Rng
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Lesson
import io.github.itstrue54.gridironacademy.model.Level
import io.github.itstrue54.gridironacademy.model.Step

/** "Where you learned it" and "Quiz me" for a glossary term (DESIGN 9.10). */
object GlossaryQuiz {
    /** A Quiz me is short: this many questions at most. */
    const val QUIZ_SIZE = 5

    /** The first lesson on [ladder] that teaches [concept], with its level. */
    fun firstLesson(content: Content, concept: String, ladder: List<Level>): Pair<Level, Lesson>? =
        ladder.asSequence().flatMap { lv -> content.unitsOf(lv).asSequence().flatMap { it.lessons }.map { lv to it } }
            .firstOrNull { (_, l) -> concept in content.conceptsOf(l) }

    /**
     * Up to [QUIZ_SIZE] graded steps on [concept] from the levels the learner can open,
     * so a quiz never asks about something not yet reachable. Seeded: one quiz per seed.
     */
    fun quiz(content: Content, concept: String, open: List<Level>, seed: Long): List<Step> {
        val pool = open.flatMap { lv -> content.unitsOf(lv).flatMap { u -> u.lessons.flatMap { it.steps } } }
            .filter { it !is Learn && it.concept == concept }
            .sortedBy { it.id }
        return Rng(seed).sample(pool, QUIZ_SIZE)
    }
}
