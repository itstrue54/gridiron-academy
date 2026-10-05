package io.github.itstrue54.gridironacademy.progress

/**
 * Spaced review of missed items. A miss goes to box 1 (due tomorrow); each correct
 * review moves it up a box and out in time; a miss sends it back to box 1.
 * Clearing box 5 retires the card.
 */
object Review {
    private val intervalDays = longArrayOf(1, 2, 4, 8, 16)

    fun miss(p: Progress, stepId: String, concept: String?, today: Long): Progress =
        p.copy(review = p.review + (stepId to ReviewCard(stepId, concept, 1, today + intervalDays[0])))

    fun answered(p: Progress, stepId: String, correct: Boolean, today: Long): Progress {
        val card = p.review[stepId] ?: return p
        if (!correct) return miss(p, stepId, card.concept, today)
        if (card.box >= 5) return p.copy(review = p.review - stepId)
        val box = card.box + 1
        return p.copy(review = p.review + (stepId to card.copy(box = box, dueDay = today + intervalDays[box - 1])))
    }

    fun due(p: Progress, today: Long): List<ReviewCard> =
        p.review.values.filter { it.dueDay <= today }.sortedBy { it.dueDay }
}
