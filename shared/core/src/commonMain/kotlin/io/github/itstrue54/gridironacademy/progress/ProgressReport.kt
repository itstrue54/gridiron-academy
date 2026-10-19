package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.model.Level

/** One level's line in the grown-up zone's progress report (DESIGN 9.9). */
data class LevelReport(
    val level: Level,
    val drillsPassed: Int,
    val drills: Int,
    val conceptsMastered: Int,
    val concepts: Int,
    val passed: Boolean,
)

/** What a parent sees: per level, drills passed and concepts mastered; minutes of practice. */
object ProgressReport {
    const val WEEK_DAYS = 7

    fun levels(content: Content, ladder: List<Level>, progress: Progress, attempts: List<AttemptEvent>, today: Long): List<LevelReport> {
        val mastery = Mastery.forLearner(attempts, content::conceptOfStep, today)
        return ladder.map { lv ->
            val lessons = content.unitsOf(lv).flatMap { it.lessons }
            val concepts = lessons.flatMap { content.conceptsOf(it) }.distinct()
            LevelReport(
                level = lv,
                drillsPassed = lessons.count { progress.lessonPassed(it.id) },
                drills = lessons.size,
                conceptsMastered = concepts.count { mastery[it]?.secure == true },
                concepts = concepts.size,
                passed = lv.id in progress.levelsPassed,
            )
        }
    }

    /** Minutes spent answering over the last [days] days, today included. */
    fun minutes(attempts: List<AttemptEvent>, today: Long, days: Int = WEEK_DAYS): Int =
        (attempts.filter { it.epochDay > today - days && it.epochDay <= today }.sumOf { it.msSpent.toLong() } / MS_PER_MINUTE).toInt()

    private const val MS_PER_MINUTE = 60_000L
}
