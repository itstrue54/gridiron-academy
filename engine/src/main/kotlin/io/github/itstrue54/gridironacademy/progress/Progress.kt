package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.engine.LessonSession
import kotlinx.serialization.Serializable

@Serializable
data class LessonResult(val bestPct: Int, val stars: Int)

/** Leitner card for a missed item. [box] 1..5; [dueDay] is an epoch day. */
@Serializable
data class ReviewCard(val stepId: String, val concept: String?, val box: Int, val dueDay: Long)

@Serializable
data class Progress(
    val lessons: Map<String, LessonResult> = emptyMap(),
    val levelTests: Map<String, Int> = emptyMap(),
    val levelGames: Map<String, Int> = emptyMap(),
    val levelsPassed: Set<String> = emptySet(),
    val review: Map<String, ReviewCard> = emptyMap(),
    val streakDays: Int = 0,
    val lastActiveDay: Long = -1,
    val xp: Int = 0,
) {
    fun withLesson(lessonId: String, pct: Int): Progress {
        val prev = lessons[lessonId]
        val best = maxOf(prev?.bestPct ?: 0, pct)
        return copy(
            lessons = lessons + (lessonId to LessonResult(best, LessonSession.stars(best))),
            xp = xp + pct / 10,
        )
    }

    fun lessonPassed(lessonId: String): Boolean =
        (lessons[lessonId]?.bestPct ?: -1) >= LessonSession.LESSON_PASS_PCT

    /** Count a day of practice toward the streak. Same-day repeats do nothing. */
    fun touch(today: Long): Progress = when {
        today == lastActiveDay -> this
        today == lastActiveDay + 1 -> copy(streakDays = streakDays + 1, lastActiveDay = today)
        else -> copy(streakDays = 1, lastActiveDay = today)
    }
}
