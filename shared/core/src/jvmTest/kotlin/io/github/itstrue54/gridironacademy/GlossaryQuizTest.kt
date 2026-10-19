package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.GlossaryQuiz
import kotlin.test.*

class GlossaryQuizTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val adultLadder = content.curriculum.levels.filter { it.track != Track.KID }.sortedBy { it.number }

    @Test fun `every glossary term is taught by some lesson`() {
        val kidLadder = content.curriculum.levels.filter { it.track != Track.ADULT }.sortedBy { it.number }
        val untaught = content.glossary.entries.map { it.id }.filter {
            GlossaryQuiz.firstLesson(content, it, adultLadder) == null && GlossaryQuiz.firstLesson(content, it, kidLadder) == null
        }
        // A term nobody teaches can't link to a lesson or be quizzed.
        assertEquals(emptyList(), untaught, "terms no lesson teaches")
    }

    @Test fun `first lesson is the earliest on the ladder`() {
        val (lv, lesson) = GlossaryQuiz.firstLesson(content, "personnel", adultLadder)!!
        assertEquals(4, lv.number)
        assertTrue("personnel" in content.conceptsOf(lesson))
    }

    @Test fun `quiz asks only graded steps on the term, only from open levels`() {
        val open = adultLadder.take(4)
        val q = GlossaryQuiz.quiz(content, "personnel", open, seed = 3)
        assertTrue(q.isNotEmpty() && q.size <= GlossaryQuiz.QUIZ_SIZE)
        assertTrue(q.all { it !is Learn && it.concept == "personnel" })
        assertEquals(q, GlossaryQuiz.quiz(content, "personnel", open, seed = 3))
        assertEquals(emptyList(), GlossaryQuiz.quiz(content, "personnel", adultLadder.take(1), seed = 3), "not reachable yet")
    }
}
