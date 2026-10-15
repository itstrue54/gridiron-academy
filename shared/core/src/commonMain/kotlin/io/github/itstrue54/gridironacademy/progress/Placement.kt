package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.model.*

/**
 * Placement for a learner who already knows some football: one short quiz per level,
 * in order. They start at the first level whose quiz they miss.
 */
object Placement {
    const val PASS_PCT = 80

    /** [scoresByLevelNumber] in ascending level order; stops at the first miss. */
    fun startingLevel(levels: List<Level>, scoresByLevelNumber: Map<Int, Int>): Level? {
        val ordered = levels.sortedBy { it.number }
        for (lv in ordered) {
            val score = scoresByLevelNumber[lv.number] ?: return lv
            if (score < PASS_PCT) return lv
        }
        return null // aced everything
    }

    /** Mark all levels below the starting one as passed. */
    fun apply(levels: List<Level>, start: Level?, p: Progress): Progress {
        val below = levels.filter { start == null || it.number < start.number }.map { it.id }
        return p.copy(levelsPassed = p.levelsPassed + below)
    }
}
