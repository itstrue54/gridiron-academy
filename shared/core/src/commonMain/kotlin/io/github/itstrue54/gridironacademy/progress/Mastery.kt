package io.github.itstrue54.gridironacademy.progress

import kotlin.math.pow
import kotlin.math.roundToInt

/** Every mastery threshold (SPEC 4.6, 11.5 rule 6). */
object MasteryRules {
    /** Only the concept's most recent first-try attempts count. */
    const val WINDOW = 10
    /** Weight of an attempt [age] attempts back: DECAY^age. */
    const val DECAY = 0.8
    const val MASTERED_SCORE = 80
    const val MASTERED_MIN_CORRECT = 3
    const val MASTERED_MIN_DAYS = 2
    /** Below mastery, a score at or above this is Practiced; under it, Learning. */
    const val PRACTICED_SCORE = 60
    /** A mastered concept untouched this long needs a refresher. */
    const val RUSTY_AFTER_DAYS = 30
}

/** The five states, shown everywhere with the same glyph, word and colour (DESIGN 8.1). */
enum class MasteryState { NEW, LEARNING, PRACTICED, MASTERED, RUSTY }

data class ConceptMastery(
    val concept: String,
    val state: MasteryState,
    /** 0-100. */
    val score: Int,
    val attempts: Int,
    val lastDay: Long?,
) {
    /** Mastered and not rusty: the only state an assignment can skip. */
    val secure: Boolean get() = state == MasteryState.MASTERED
}

/**
 * Mastery per concept, derived from attempts (SPEC 4.6). Pure and deterministic: the
 * learner app, the coach app and the server compute the same answer from the same events.
 */
object Mastery {

    /** [attempts] are this concept's attempts, in any order; [today] is an epoch day. */
    fun of(concept: String, attempts: List<AttemptEvent>, today: Long): ConceptMastery {
        val graded = attempts.filter { it.firstTry }.sortedWith(AttemptOrder)
        if (graded.isEmpty()) return ConceptMastery(concept, MasteryState.NEW, 0, 0, null)

        val window = ArrayDeque<AttemptEvent>()
        var wasMastered = false
        var reviewMissSinceMastery = false
        for (a in graded) {
            window.addLast(a)
            if (window.size > MasteryRules.WINDOW) window.removeFirst()
            if (mastered(window)) {
                wasMastered = true
                reviewMissSinceMastery = false
            } else if (wasMastered && a.context == AttemptContext.REVIEW && !a.correct) {
                reviewMissSinceMastery = true
            }
        }
        val last = graded.last().epochDay
        val score = score(window)
        val state = when {
            mastered(window) && today - last >= MasteryRules.RUSTY_AFTER_DAYS -> MasteryState.RUSTY
            mastered(window) -> MasteryState.MASTERED
            wasMastered && reviewMissSinceMastery -> MasteryState.RUSTY
            score >= MasteryRules.PRACTICED_SCORE -> MasteryState.PRACTICED
            else -> MasteryState.LEARNING
        }
        return ConceptMastery(concept, state, score, graded.size, last)
    }

    /**
     * Every concept a learner has touched, plus [concepts] (shown as New when untouched).
     * [conceptOfStep] maps a step id to its concept; steps without one don't count.
     */
    fun forLearner(
        events: List<AttemptEvent>,
        conceptOfStep: (String) -> String?,
        today: Long,
        concepts: Collection<String> = emptyList(),
    ): Map<String, ConceptMastery> {
        val byConcept = events.groupBy { conceptOfStep(it.stepId) }.filterKeys { it != null }
        val all = (byConcept.keys.filterNotNull() + concepts).distinct()
        return all.associateWith { c -> of(c, byConcept[c].orEmpty(), today) }
    }

    /** Exponentially weighted first-try credit, newest attempt weighted 1. */
    private fun score(window: List<AttemptEvent>): Int {
        var num = 0.0
        var den = 0.0
        window.asReversed().forEachIndexed { age, a ->
            val w = MasteryRules.DECAY.pow(age)
            num += w * a.pct
            den += w
        }
        return (100 * num / den).roundToInt()
    }

    private fun mastered(window: List<AttemptEvent>): Boolean {
        val correct = window.filter { it.correct }
        return score(window) >= MasteryRules.MASTERED_SCORE &&
            correct.size >= MasteryRules.MASTERED_MIN_CORRECT &&
            correct.map { it.epochDay }.distinct().size >= MasteryRules.MASTERED_MIN_DAYS &&
            window.last().correct
    }
}
