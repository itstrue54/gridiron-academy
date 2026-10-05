package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.*

/**
 * A "game" of situational decisions. Each [Situation] is a snap or a call; the
 * learner earns coach points by decision quality. Pure bookkeeping: it does not
 * simulate football, it scores whether the learner would have coached it well.
 */
class GameDay(private val situations: List<Situation>) {
    var index = 0
        private set
    var coachPoints = 0f
        private set

    val current: Situation? get() = situations.getOrNull(index)
    val finished: Boolean get() = index >= situations.size
    val maxPoints: Int get() = situations.size

    fun decide(optionIndex: Int): Grade {
        val s = checkNotNull(current) { "game is over" }
        val g = Grader.grade(s, Answer.Choice(optionIndex))
        coachPoints += g.credit
        index++
        return g
    }

    val scorePct: Int get() = if (situations.isEmpty()) 100 else Math.round(100f * coachPoints / situations.size)
}
