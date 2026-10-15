package io.github.itstrue54.gridironacademy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One screen of a lesson. Steps that are graded carry an [explanation], shown after
 * every answer: a wrong answer is the best teaching moment, so it is never optional.
 * [concept] points at a glossary entry so missed items can be reviewed later.
 */
@Serializable
sealed interface Step {
    val id: String
    val concept: String?
}

/** Teaching card. Not graded. */
@Serializable
@SerialName("learn")
data class Learn(
    override val id: String,
    val title: String,
    val body: String,
    val diagram: Diagram? = null,
    override val concept: String? = null,
) : Step

@Serializable
@SerialName("multiple_choice")
data class MultipleChoice(
    override val id: String,
    val prompt: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
    val diagram: Diagram? = null,
    override val concept: String? = null,
) : Step

@Serializable
@SerialName("true_false")
data class TrueFalse(
    override val id: String,
    val statement: String,
    val answer: Boolean,
    val explanation: String,
    override val concept: String? = null,
) : Step

@Serializable
data class MatchPair(val left: String, val right: String)

/** Drag/tap matching. Every pair must be matched for full credit. */
@Serializable
@SerialName("match")
data class Match(
    override val id: String,
    val prompt: String,
    val pairs: List<MatchPair>,
    val explanation: String,
    override val concept: String? = null,
) : Step

/** Put [items] in order. [items] is stored in the correct order; the UI shuffles. */
@Serializable
@SerialName("order")
data class Order(
    override val id: String,
    val prompt: String,
    val items: List<String>,
    val explanation: String,
    override val concept: String? = null,
) : Step

/** Tap the right player(s) or zone(s) on a diagram. [targets] are player or zone ids. */
@Serializable
@SerialName("diagram_tap")
data class DiagramTap(
    override val id: String,
    val prompt: String,
    val diagram: Diagram,
    val targets: Set<String>,
    val explanation: String,
    override val concept: String? = null,
) : Step

/**
 * "Read the defense": a pre-snap picture, the player names the coverage (or front,
 * or pressure), then [reveal] shows what really happened after the snap. When the
 * look was a disguise, [reveal] is where the learner sees the rotation.
 */
@Serializable
@SerialName("read_defense")
data class ReadDefense(
    override val id: String,
    val prompt: String,
    val preSnap: Diagram,
    val choices: List<String>,
    val answer: Int,
    val reveal: Diagram,
    val explanation: String,
    val disguised: Boolean = false,
    override val concept: String? = null,
) : Step

@Serializable
enum class Quality {
    @SerialName("best") BEST,
    @SerialName("ok") OK,
    @SerialName("poor") POOR,
}

@Serializable
data class SituationOption(val text: String, val quality: Quality, val why: String)

/** Game-state decision: down, distance, clock, score. */
@Serializable
@SerialName("situation")
data class Situation(
    override val id: String,
    val state: GameState,
    val prompt: String,
    val options: List<SituationOption>,
    val explanation: String,
    override val concept: String? = null,
) : Step

@Serializable
data class GameState(
    val quarter: Int,
    /** Seconds left in the quarter. */
    val clock: Int,
    val down: Int,
    val toGo: Int,
    /** Yards from your own goal line: 25 = your own 25, 75 = opponent's 25. */
    val yardLine: Int,
    /** Your score minus theirs. */
    val scoreDiff: Int,
    val timeouts: Int = 3,
    val note: String = "",
)
