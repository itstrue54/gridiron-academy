package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.*

sealed interface Answer {
    data class Choice(val index: Int) : Answer
    data class Bool(val value: Boolean) : Answer
    /** left text -> right text the learner paired it with. */
    data class Matches(val pairs: Map<String, String>) : Answer
    data class Ordering(val items: List<String>) : Answer
    data class Taps(val ids: Set<String>) : Answer
    /** The learner read through the card; nothing to grade. */
    data object Seen : Answer
}

/**
 * [credit] is 0..1. A step counts as "correct" only at full credit; partial credit
 * (an OK-but-not-best game decision, some matches right) feeds the score but still
 * queues the item for a retry.
 */
data class Grade(val credit: Float, val feedback: String) {
    val correct: Boolean get() = credit >= 1f
}

object Grader {
    const val OK_CREDIT = 0.5f

    fun isGraded(step: Step): Boolean = step !is Learn

    fun grade(step: Step, answer: Answer): Grade = when (step) {
        is Learn -> Grade(1f, "")
        is MultipleChoice -> choice(answer, step.answer, step.explanation)
        is ReadDefense -> choice(answer, step.answer, step.explanation)
        is TrueFalse -> {
            val a = answer as? Answer.Bool
            Grade(if (a != null && a.value == step.answer) 1f else 0f, step.explanation)
        }
        is Match -> {
            val a = answer as? Answer.Matches
            val right = a?.pairs?.count { (l, r) -> step.pairs.any { it.left == l && it.right == r } } ?: 0
            Grade(right.toFloat() / step.pairs.size, step.explanation)
        }
        is Order -> {
            val a = answer as? Answer.Ordering
            val hits = a?.items?.zip(step.items)?.count { (x, y) -> x == y } ?: 0
            // All-or-nothing on order is too harsh for long sequences, but a partly
            // right order must still be retried, hence credit < 1 until perfect.
            Grade(hits.toFloat() / step.items.size, step.explanation)
        }
        is DiagramTap -> {
            val a = answer as? Answer.Taps
            val credit = if (a == null) 0f else {
                val hits = a.ids.count { it in step.targets }
                val wrong = a.ids.size - hits
                ((hits - wrong).coerceAtLeast(0)).toFloat() / step.targets.size
            }
            Grade(credit, step.explanation)
        }
        is Situation -> {
            val a = answer as? Answer.Choice
            val opt = a?.index?.let { step.options.getOrNull(it) }
            val credit = when (opt?.quality) {
                Quality.BEST -> 1f
                Quality.OK -> OK_CREDIT
                else -> 0f
            }
            Grade(credit, opt?.why?.let { "$it ${step.explanation}" } ?: step.explanation)
        }
    }

    private fun choice(answer: Answer, correct: Int, explanation: String): Grade {
        val a = answer as? Answer.Choice
        return Grade(if (a?.index == correct) 1f else 0f, explanation)
    }
}
