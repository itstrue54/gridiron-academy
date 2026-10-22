package io.github.itstrue54.gridironacademy.ui.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.engine.Grade
import io.github.itstrue54.gridironacademy.engine.SpeedRound
import io.github.itstrue54.gridironacademy.model.MultipleChoice
import io.github.itstrue54.gridironacademy.model.Step
import io.github.itstrue54.gridironacademy.model.TrueFalse

/**
 * A Speed round (DESIGN 9.6) made observable. The engine's [SpeedRound] keeps score;
 * time comes in through [tick] as milliseconds since the round began, so the round is
 * deterministic and a test can play it without a clock.
 *
 * The round waits on its rules screen until [start]. An untimed round (DESIGN 12) keeps the
 * points and the streak but has no clock: time still adds up, for the attempt log, but
 * never ends the round.
 */
@Stable
class SpeedRoundController(
    questions: List<Step>,
    val limitMillis: Long,
    /** Told about every answer: step, grade, answer, first try (always), time on the question. */
    private val onGraded: ((Step, Grade, Answer, Boolean, Long) -> Unit)? = null,
) {
    private var shownAtElapsed = 0L
    private val round = SpeedRound(questions, limitMillis)
    private val missed = mutableListOf<ReviewItem>()

    var elapsedMillis: Long by mutableStateOf(0L)
        private set
    /** False while the rules screen is up. */
    var started: Boolean by mutableStateOf(false)
        private set
    var timed: Boolean by mutableStateOf(true)
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

    fun start(timed: Boolean) {
        this.timed = timed
        started = true
    }

    /** What the engine's clock reads: frozen at zero when untimed. */
    private val gameMillis: Long get() = if (timed) elapsedMillis else 0L

    fun tick(elapsed: Long) {
        if (over) return
        elapsedMillis = elapsed
        if (round.isOver(gameMillis)) over = true
    }

    fun answer(a: Answer) {
        val step = current ?: return
        val grade = round.answer(a, gameMillis)
        if (grade == null) { over = true; return }
        onGraded?.invoke(step, grade, a, true, elapsedMillis - shownAtElapsed)
        shownAtElapsed = elapsedMillis
        if (!grade.correct) missed += reviewItem(step, grade.feedback)
        lastRight = grade.correct
        points = round.points
        streak = round.streak
        bestStreak = round.bestStreak
        answered = round.index
        current = round.current
        if (round.isOver(gameMillis)) over = true
    }

    private fun reviewItem(step: Step, why: String): ReviewItem = when (step) {
        is MultipleChoice -> ReviewItem(step.prompt, why, answerText = step.choices[step.answer])
        is TrueFalse -> ReviewItem(step.statement, why, answerTrue = step.answer)
        else -> ReviewItem("", why)
    }
}
