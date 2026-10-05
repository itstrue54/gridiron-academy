package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.Step

/**
 * Drives one lesson (or level test, or game round) over a list of steps.
 * Missed items come back once at the end so the learner finishes on a win;
 * only the first attempt counts toward the score.
 */
class LessonSession(steps: List<Step>) {
    private val queue = ArrayDeque(steps)
    private val retried = mutableSetOf<String>()
    private val firstTry = mutableMapOf<String, Float>()

    var current: Step? = queue.firstOrNull()
        private set
    var lastGrade: Grade? = null
        private set

    val finished: Boolean get() = current == null

    /** Graded steps answered on their first attempt. */
    val gradedCount: Int get() = firstTry.size

    val totalCredit: Float get() = firstTry.values.sum()

    /** 0..100 over graded steps seen on their first attempt; 100 if none are graded. */
    val scorePct: Int
        get() = if (firstTry.isEmpty()) 100 else Math.round(100f * totalCredit / firstTry.size)

    val missedIds: Set<String> get() = firstTry.filterValues { it < 1f }.keys

    fun submit(answer: Answer): Grade {
        val step = checkNotNull(current) { "session is finished" }
        val grade = Grader.grade(step, answer)
        lastGrade = grade
        if (Grader.isGraded(step)) {
            if (step.id !in firstTry) firstTry[step.id] = grade.credit
            if (!grade.correct && retried.add(step.id)) queue.addLast(step)
        }
        return grade
    }

    fun next(): Step? {
        queue.removeFirstOrNull()
        lastGrade = null
        current = queue.firstOrNull()
        return current
    }

    /** Remaining steps including the current one, retries included. */
    val remaining: Int get() = queue.size

    companion object {
        fun stars(scorePct: Int): Int = when {
            scorePct >= 95 -> 3
            scorePct >= 80 -> 2
            scorePct >= 60 -> 1
            else -> 0
        }

        const val LESSON_PASS_PCT = 60
    }
}
