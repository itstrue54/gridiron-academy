package io.github.itstrue54.gridironacademy.progress

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where an attempt happened. Review misses can make a mastered concept rusty (SPEC 4.6). */
@Serializable
enum class AttemptContext {
    @SerialName("lesson") LESSON,
    @SerialName("review") REVIEW,
    @SerialName("check") CHECK,
    @SerialName("game") GAME,
    @SerialName("placement") PLACEMENT,
}

/**
 * One graded answer (SPEC 8.1). Identity is ([deviceId], [seq]): unique without
 * randomness. Time comes in as [epochDay] and [at] (epoch millis); the engine never
 * reads a clock.
 */
@Serializable
data class AttemptEvent(
    val learnerId: String,
    val deviceId: String,
    val seq: Long,
    val stepId: String,
    val context: AttemptContext,
    /** Credit 0..1; partial credit for match and order steps. */
    val pct: Float,
    /** False for a retry of a step already missed in the same session. */
    val firstTry: Boolean = true,
    /** The answer given, as shown on screen, so a coach can see common wrong answers. */
    val answer: String? = null,
    val msSpent: Int = 0,
    val epochDay: Long,
    val at: Long,
) {
    val correct: Boolean get() = pct >= 1f
}

/** What happened to an assignment on the learner's side (SPEC 8.1). */
@Serializable
enum class AssignmentEventType {
    @SerialName("opened") OPENED,
    @SerialName("check_result") CHECK_RESULT,
}

@Serializable
data class AssignmentEvent(
    val learnerId: String,
    val deviceId: String,
    val seq: Long,
    val assignmentId: String,
    val type: AssignmentEventType,
    /** The check score, 0-100, for [AssignmentEventType.CHECK_RESULT]. */
    val pct: Int = 0,
    val epochDay: Long,
    val at: Long,
)

/** The fold order (SPEC 8.1): deterministic and the same on every device. */
val AttemptOrder: Comparator<AttemptEvent> =
    compareBy<AttemptEvent>({ it.epochDay }, { it.at }, { it.deviceId }, { it.seq })

/**
 * What one row of the server's event log carries in `payload` (SPEC 8.1): a learner
 * device uploads these and every adult's app folds them, so both sides share this one
 * definition. The row's (learner, device, seq) key repeats the event's own identity.
 */
@Serializable
sealed interface EventPayload {
    @Serializable
    @SerialName("attempt")
    data class Attempt(val event: AttemptEvent) : EventPayload

    @Serializable
    @SerialName("assignment")
    data class Assignment(val event: AssignmentEvent) : EventPayload

    companion object {
        /** The wire format: `{"type": "attempt", "event": {...}}`. Unknown fields are ignored. */
        val json = kotlinx.serialization.json.Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }
}
