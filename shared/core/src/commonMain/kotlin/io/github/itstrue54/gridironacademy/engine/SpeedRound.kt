package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.*
import kotlin.math.roundToInt

/**
 * Timed rapid-fire game. Time is injected (millis since the round began), never read
 * from the system clock, so rounds are deterministic and testable.
 * Only single-tap questions (multiple choice, true/false) qualify.
 */
class SpeedRound(
    private val questions: List<Step>,
    private val limitMillis: Long = 60_000,
) {
    init {
        require(questions.all { it is MultipleChoice || it is TrueFalse }) {
            "speed round takes multiple choice and true/false only"
        }
    }

    var index = 0
        private set
    var correct = 0
        private set
    var streak = 0
        private set
    var bestStreak = 0
        private set
    var points = 0
        private set
    private var over = false

    fun isOver(nowMillis: Long): Boolean = over || nowMillis >= limitMillis || index >= questions.size

    val current: Step? get() = questions.getOrNull(index)

    /** Returns the grade, or null if the round already ended. Streak of 3+ doubles points. */
    fun answer(answer: Answer, nowMillis: Long): Grade? {
        if (isOver(nowMillis)) { over = true; return null }
        val grade = Grader.grade(questions[index], answer)
        if (grade.correct) {
            correct++; streak++
            bestStreak = maxOf(bestStreak, streak)
            points += if (streak >= 3) 20 else 10
        } else {
            streak = 0
        }
        index++
        return grade
    }

    val accuracyPct: Int get() = if (index == 0) 0 else (100f * correct / index).roundToInt()

    /**
     * The gate score: right answers out of every question in the round, so questions the
     * clock cut off count as missed. Accuracy alone would let one right answer and a
     * wait pass the Big Game.
     */
    val scorePct: Int get() = if (questions.isEmpty()) 100 else (100f * correct / questions.size).roundToInt()

    val questionCount: Int get() = questions.size
}
