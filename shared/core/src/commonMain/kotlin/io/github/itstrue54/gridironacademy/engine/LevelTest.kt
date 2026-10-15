package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.*

/** Builds the pools a level test and level game draw from, deterministically. */
object LevelTest {
    private fun gradedSteps(units: List<CourseUnit>): List<Step> =
        units.flatMap { u -> u.lessons.flatMap { l -> l.steps } }.filter(Grader::isGraded)

    fun test(units: List<CourseUnit>, gate: LevelGate, seed: Long): List<Step> =
        Rng(seed).sample(gradedSteps(units), gate.testQuestions)

    fun gamePool(units: List<CourseUnit>, gate: LevelGate, seed: Long): List<Step> {
        val all = gradedSteps(units)
        val pool = when (gate.game) {
            GameType.SPEED_ROUND -> all.filter { it is MultipleChoice || it is TrueFalse }
            GameType.READ_DEFENSE -> all.filterIsInstance<ReadDefense>()
            GameType.GAME_DAY -> all.filterIsInstance<Situation>()
            GameType.DIAGRAM_DASH -> all.filterIsInstance<DiagramTap>()
        }
        return Rng(seed xor 0x5DEECE66DL).sample(pool, gate.gameQuestions)
    }

    /** True if the level has enough content of the right kind to run its gate. */
    fun gateIsPlayable(units: List<CourseUnit>, gate: LevelGate): Boolean =
        gradedSteps(units).size >= gate.testQuestions &&
            gamePool(units, gate, 1).size >= gate.gameQuestions
}
