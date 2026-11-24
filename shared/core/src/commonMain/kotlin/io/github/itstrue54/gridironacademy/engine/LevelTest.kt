package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.*

/** Builds the pools a level test and level game draw from, deterministically. */
object LevelTest {
    private fun gradedSteps(units: List<CourseUnit>): List<Step> =
        units.flatMap { u -> u.lessons.flatMap { l -> l.steps } }.filter(Grader::isGraded)

    fun test(units: List<CourseUnit>, gate: LevelGate, seed: Long): List<Step> =
        Rng(seed).sample(gradedSteps(units), gate.testQuestions)

    /** True if [step] can be played in [game]. */
    fun fits(game: GameType, step: Step): Boolean = when (game) {
        GameType.SPEED_ROUND -> step is MultipleChoice || step is TrueFalse
        GameType.READ_DEFENSE -> step is ReadDefense
        GameType.GAME_DAY -> step is Situation
        GameType.DIAGRAM_DASH -> step is DiagramTap
    }

    /**
     * The gate game's questions. The gate's own [LevelGate.gameItems] come first (all of
     * them, or a seeded sample if there are more than the game asks), then the level's
     * lesson steps of the right kind fill the rest. An [LevelGate.ordered] gate plays
     * game items then lesson items in authored order, cut to the game's length.
     */
    fun gamePool(units: List<CourseUnit>, gate: LevelGate, seed: Long): List<Step> {
        val lesson = gradedSteps(units).filter { fits(gate.game, it) }
        val q = gate.gameQuestions
        if (gate.ordered) return (gate.gameItems + lesson).take(q)
        val rng = Rng(seed xor 0x5DEECE66DL)
        val own = if (gate.gameItems.size > q) rng.sample(gate.gameItems, q) else gate.gameItems
        val picked = own + rng.sample(lesson, q - own.size)
        // Game items are selected first, but met in a mixed order. Without any, the draw is
        // already a shuffle, so it stays exactly as it was.
        return if (own.isEmpty()) picked else rng.shuffled(picked)
    }

    /** True if the level has enough content of the right kind to run its gate. */
    fun gateIsPlayable(units: List<CourseUnit>, gate: LevelGate): Boolean =
        gradedSteps(units).size >= gate.testQuestions &&
            gamePool(units, gate, 1).size >= gate.gameQuestions
}
