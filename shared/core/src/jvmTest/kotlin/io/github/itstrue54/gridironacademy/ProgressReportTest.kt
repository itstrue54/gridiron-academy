package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class ProgressReportTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val ladder = content.curriculum.levels.filter { it.track != Track.KID }.sortedBy { it.number }

    @Test fun `a new learner has nothing passed or mastered`() {
        val r = ProgressReport.levels(content, ladder, Progress(), emptyList(), 100)
        assertEquals(ladder.size, r.size)
        assertTrue(r.all { it.drillsPassed == 0 && it.conceptsMastered == 0 && it.drills > 0 && it.concepts > 0 })
    }

    @Test fun `passed drills and mastered concepts are counted per level`() {
        val l1 = ladder.first()
        val lesson = content.unitsOf(l1).first().lessons.first()
        val progress = Progress().withLesson(lesson.id, 100)
        var seq = 0L
        // Every graded step of the lesson right on two days: its concepts are mastered.
        val attempts = listOf(98L, 99L, 100L).flatMap { d ->
            lesson.steps.filter { it !is Learn }.map { AttemptEvent("me", "d", seq++, it.id, AttemptContext.LESSON, 1f, msSpent = 30_000, epochDay = d, at = d * 1000 + seq) }
        }
        val r = ProgressReport.levels(content, ladder, progress, attempts, 100).first()
        assertEquals(1, r.drillsPassed)
        assertTrue(r.conceptsMastered >= 1)
        val expectedMinutes = (attempts.sumOf { it.msSpent.toLong() } / 60_000).toInt()
        assertEquals(expectedMinutes, ProgressReport.minutes(attempts, 100))
        assertEquals(0, ProgressReport.minutes(attempts, 120), "outside the week")
    }
}
