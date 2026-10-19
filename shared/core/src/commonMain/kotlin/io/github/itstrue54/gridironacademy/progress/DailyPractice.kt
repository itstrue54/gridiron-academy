package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.engine.Rng
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Level
import io.github.itstrue54.gridironacademy.model.Step

/** Today's practice limits (SPEC 6.4, 11.5 rule 6). */
object PracticeRules {
    /** The most questions in one day's practice. */
    const val MAX_ITEMS = 10
    /** Fewer than this and practice tops up with concepts still being learned. */
    const val MIN_ITEMS = 5
}

/** What today's practice holds, and whether it's the screen's main action. */
data class Practice(
    val steps: List<Step>,
    /** Cards are due or a concept needs a refresher: Home leads with practice (DESIGN 9.1). */
    val urgent: Boolean,
)

/**
 * Today's practice (SPEC 6.4): due review cards first, then concepts that need a
 * refresher, then concepts still being learned on the current level, 10 at most.
 * Deterministic from the seed and the day: the same profile sees the same set all day.
 */
object DailyPractice {

    fun pick(
        progress: Progress,
        attempts: List<AttemptEvent>,
        content: Content,
        current: Level?,
        today: Long,
        seed: Long,
    ): Practice {
        val rng = Rng(seed * PRIME + today)
        val stepsById = content.lessons.flatMap { it.steps }.associateBy { it.id }
        val chosen = LinkedHashMap<String, Step>()
        fun add(s: Step?) { if (s != null && s !is Learn && s.id !in chosen && chosen.size < PracticeRules.MAX_ITEMS) chosen[s.id] = s }

        // 1. Due review cards, oldest first.
        val due = Review.due(progress, today)
        due.forEach { add(stepsById[it.stepId]) }

        // Steps this learner has seen, by concept: practice asks what was taught.
        val seen = attempts.map { it.stepId }.toSet()
        val seenByConcept = seen.mapNotNull { stepsById[it] }.filter { it !is Learn && it.concept != null }
            .groupBy { it.concept!! }
        val mastery = Mastery.forLearner(attempts, content::conceptOfStep, today)

        // 2. Concepts that need a refresher, weakest first.
        val rusty = mastery.values.filter { it.state == MasteryState.RUSTY }
            .sortedWith(compareBy({ it.score }, { it.concept }))
        rusty.forEach { m -> seenByConcept[m.concept]?.let { add(rng.shuffled(it.sortedBy { s -> s.id }).first()) } }

        // 3. Top up with concepts still being learned on the current level.
        if (chosen.size < PracticeRules.MIN_ITEMS && current != null) {
            val levelConcepts = content.unitsOf(current).flatMap { u -> u.lessons.flatMap { content.conceptsOf(it) } }.toSet()
            mastery.values
                .filter { it.concept in levelConcepts && it.state == MasteryState.LEARNING }
                .sortedWith(compareBy({ it.score }, { it.concept }))
                .forEach { m -> seenByConcept[m.concept]?.let { add(rng.shuffled(it.sortedBy { s -> s.id }).first()) } }
        }
        return Practice(chosen.values.toList(), urgent = due.isNotEmpty() || rusty.isNotEmpty())
    }

    /** Practice is done for the day once any review answer was given today. */
    fun doneToday(attempts: List<AttemptEvent>, today: Long): Boolean =
        attempts.any { it.context == AttemptContext.REVIEW && it.epochDay == today }

    private const val PRIME = 1_000_003L
}
