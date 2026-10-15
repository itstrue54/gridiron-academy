package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.model.*

/** What the learner may open next. Pure functions over [Progress]. */
object Gating {
    /** A level is open when all of `requires` and any one of `requiresAny` are passed. */
    fun levelUnlocked(level: Level, p: Progress): Boolean =
        level.requires.all { it in p.levelsPassed } &&
            (level.requiresAny.isEmpty() || level.requiresAny.any { it in p.levelsPassed })

    /** Lessons go in order: lesson N opens when lesson N-1 is passed. */
    fun lessonUnlocked(level: Level, units: List<CourseUnit>, lessonId: String, p: Progress): Boolean {
        if (!levelUnlocked(level, p)) return false
        val ordered = units.flatMap { it.lessons }
        val i = ordered.indexOfFirst { it.id == lessonId }
        if (i < 0) return false
        return i == 0 || p.lessonPassed(ordered[i - 1].id)
    }

    /** The first open lesson not yet passed, or null when every lesson is passed or the level is locked. */
    fun nextLesson(level: Level, units: List<CourseUnit>, p: Progress): Lesson? =
        units.flatMap { it.lessons }.firstOrNull { !p.lessonPassed(it.id) && lessonUnlocked(level, units, it.id, p) }

    fun allLessonsPassed(units: List<CourseUnit>, p: Progress): Boolean =
        units.flatMap { it.lessons }.all { p.lessonPassed(it.id) }

    fun testUnlocked(level: Level, units: List<CourseUnit>, p: Progress): Boolean =
        levelUnlocked(level, p) && allLessonsPassed(units, p)

    fun gameUnlocked(level: Level, p: Progress): Boolean =
        (p.levelTests[level.id] ?: -1) >= level.gate.testPassPct

    fun recordTest(level: Level, p: Progress, pct: Int): Progress {
        val best = maxOf(p.levelTests[level.id] ?: 0, pct)
        return finish(level, p.copy(levelTests = p.levelTests + (level.id to best), xp = p.xp + pct / 5))
    }

    fun recordGame(level: Level, p: Progress, pct: Int): Progress {
        val best = maxOf(p.levelGames[level.id] ?: 0, pct)
        return finish(level, p.copy(levelGames = p.levelGames + (level.id to best), xp = p.xp + pct / 5))
    }

    private fun finish(level: Level, p: Progress): Progress {
        val testOk = (p.levelTests[level.id] ?: -1) >= level.gate.testPassPct
        val gameOk = (p.levelGames[level.id] ?: -1) >= level.gate.gamePassPct
        return if (testOk && gameOk) p.copy(levelsPassed = p.levelsPassed + level.id) else p
    }

    /**
     * Test-out: an adult who aces a level's test without doing its lessons skips them.
     * Lessons are marked passed at the test score, and the test needs no unlock.
     */
    fun testOut(level: Level, units: List<CourseUnit>, p: Progress, pct: Int): Progress {
        if (!levelUnlocked(level, p) || pct < level.gate.testPassPct) return p
        val skipped = units.flatMap { it.lessons }.fold(p) { acc, l ->
            if (acc.lessonPassed(l.id)) acc else acc.withLesson(l.id, pct)
        }
        return recordTest(level, skipped, pct)
    }
}
