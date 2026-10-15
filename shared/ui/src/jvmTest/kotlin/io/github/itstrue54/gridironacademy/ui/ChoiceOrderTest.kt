package io.github.itstrue54.gridironacademy.ui

import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.ui.lesson.choiceOrder
import kotlin.test.*

/** The right answer must not sit in a guessable spot (authors favour putting it first). */
class ChoiceOrderTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    @Test
    fun orderIsAFixedPermutation() {
        val choices = listOf("Cover 2", "Cover 3", "Cover 4", "Cover 1")
        val a = choiceOrder("step-1", choices)
        assertEquals(choices.indices.toSet(), a.toSet())
        assertEquals(a, choiceOrder("step-1", choices), "same step, same order, every time")
    }

    @Test
    fun plainNumbersAreSortedNotShuffled() {
        assertEquals(listOf(2, 0, 3, 1), choiceOrder("n", listOf("10", "100", "5", "50")))
    }

    /** Where the right answer is shown, per level, for every single-answer question. */
    private fun shownPositions(level: Level): List<Int> =
        content.unitsOf(level).flatMap { it.lessons }.flatMap { it.steps }.mapNotNull { s ->
            when (s) {
                is MultipleChoice -> choiceOrder(s.id, s.choices).indexOf(s.answer)
                is ReadDefense -> choiceOrder(s.id, s.choices).indexOf(s.answer)
                is Situation -> choiceOrder(s.id, s.options.map { it.text }).indexOf(s.options.indexOfFirst { it.quality == Quality.BEST })
                else -> null
            }
        }

    @Test
    fun noLevelPutsTheAnswerInOneSpotMostOfTheTime() {
        for (level in content.curriculum.levels) {
            val shown = shownPositions(level)
            val top = shown.groupingBy { it }.eachCount().maxOf { it.value }
            println("${level.id}: ${shown.groupingBy { it }.eachCount().toSortedMap()}")
            assertTrue(top.toFloat() / shown.size <= 0.5f, "${level.id}: answer shown in one position ${top} of ${shown.size} times")
        }
    }
}
