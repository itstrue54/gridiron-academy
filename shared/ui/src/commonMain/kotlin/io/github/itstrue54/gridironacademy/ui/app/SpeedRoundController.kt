package io.github.itstrue54.gridironacademy.ui.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.engine.SpeedRound
import io.github.itstrue54.gridironacademy.model.MultipleChoice
import io.github.itstrue54.gridironacademy.model.Step
import io.github.itstrue54.gridironacademy.model.TrueFalse

/**
 * A Speed round (DESIGN 9.6) made observable. The engine's [SpeedRound] keeps score;
 * time comes in through [tick] as milliseconds since the round began, so the round is
 * deterministic and a test can play it without a clock.
 */
@Stable
class SpeedRoundController(questions: List<Step>, val limitMillis: Long) {
    private val round = SpeedRound(questions, limitMillis)
    private val missed = mutableListOf<ReviewItem>()

    var elapsedMillis: Long by mutableStateOf(0L)
        private set
    var current: Step? by mutableStateOf(round.current)
        private set
    var points: Int by mutableStateOf(0)
        private set
    var streak: Int by mutableStateOf(0)
        private set
    var bestStreak: Int by mutableStateOf(0)
        private set
    var answered: Int by mutableStateOf(0)
        private set
    var over: Boolean by mutableStateOf(round.isOver(0))
        private set
    /** True for a moment after a right answer, false after a miss, null otherwise: drives the flash. */
    var lastRight: Boolean? by mutableStateOf(null)
        private set

    val questionCount: Int get() = round.questionCount
    val scorePct: Int get() = round.scorePct
    val millisLeft: Long get() = (limitMillis - elapsedMillis).coerceAtLeast(0)
    val review: List<ReviewItem> get() = missed.toList()

    fun tick(elapsed: Long) {
        if (over) return
        elapsedMillis = elapsed
        if (round.isOver(elapsed)) over = true
    }

    fun answer(a: Answer) {
        val step = current ?: return
        val grade = round.answer(a, elapsedMillis)
        if (grade == null) { over = true; return }
        if (!grade.correct) missed += reviewItem(step, grade.feedback)
        lastRight = grade.correct
        points = round.points
        streak = round.streak
        bestStreak = round.bestStreak
        answered = round.index
        current = round.current
        if (round.isOver(elapsedMillis)) over = true
    }

    private fun reviewItem(step: Step, why: String): ReviewItem = when (step) {
        is MultipleChoice -> ReviewItem(step.prompt, why, answerText = step.choices[step.answer])
        is TrueFalse -> ReviewItem(step.statement, why, answerTrue = step.answer)
        else -> ReviewItem("", why)
    }
}
