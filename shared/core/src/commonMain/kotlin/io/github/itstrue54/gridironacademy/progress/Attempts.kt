package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.model.*

/** Turning a graded answer into the log's terms (SPEC 8.1). */
object Attempts {
    /**
     * The answer as the learner saw it, for "the usual wrong answer" on the coach's
     * dashboard. Null where the answer isn't one piece of text (taps, matches, orders).
     */
    fun answerText(step: Step, answer: Answer): String? = when {
        step is MultipleChoice && answer is Answer.Choice -> step.choices.getOrNull(answer.index)
        step is ReadDefense && answer is Answer.Choice -> step.choices.getOrNull(answer.index)
        step is Situation && answer is Answer.Choice -> step.options.getOrNull(answer.index)?.text
        step is TrueFalse && answer is Answer.Bool -> answer.value.toString()
        else -> null
    }
}
