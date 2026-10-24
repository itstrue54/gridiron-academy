package io.github.itstrue54.gridironacademy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A football diagram in field coordinates, so it can express a formation, a route
 * concept, a coverage shell and a post-snap rotation with one model.
 *
 * x runs across the field in yards, 0 = middle, negative = left, positive = right
 * (field is 53.3 wide, so about -26.7..26.7; the hashes sit at +-3.1).
 * y runs downfield from the line of scrimmage in yards: positive = toward the
 * defense's goal line, negative = the offensive backfield.
 */
@Serializable
data class Diagram(
    val players: List<DiagramPlayer> = emptyList(),
    val routes: List<DiagramRoute> = emptyList(),
    val zones: List<DiagramZone> = emptyList(),
    /** Yards of field shown downfield of the LOS and behind it. */
    val depth: Float = 15f,
    val behind: Float = 6f,
    val caption: String = "",
    /**
     * Where the football goes when the play runs (film mode, DESIGN 7.8), in time order.
     * Empty: no ball is drawn. The first spot is who holds it at the snap.
     */
    val ball: List<BallSpot> = emptyList(),
)

/**
 * The ball reaches [playerId] at [at], a fraction 0..1 of the play. Between two spots it
 * is in the air (a pass, a pitch) or changing hands (a snap, a handoff); a longer trip
 * leaves the hand earlier. Afterwards it moves with that player.
 */
@Serializable
data class BallSpot(val playerId: String, val at: Float = 0f)

@Serializable
enum class Side { OFFENSE, DEFENSE }

@Serializable
data class DiagramPlayer(
    val id: String,
    /** Short label drawn on the dot: "QB", "X", "S", "M". */
    val label: String,
    val side: Side,
    val x: Float,
    val y: Float,
)

@Serializable
enum class PathKind {
    @SerialName("pass") PASS,
    @SerialName("run") RUN,
    @SerialName("motion") MOTION,
    @SerialName("block") BLOCK,
    @SerialName("blitz") BLITZ,
    @SerialName("rotate") ROTATE,
    @SerialName("drop") DROP,
}

@Serializable
data class Pt(val x: Float, val y: Float)

/** A path a player takes. [points] are waypoints after the player's start. */
@Serializable
data class DiagramRoute(
    val playerId: String,
    val points: List<Pt>,
    val kind: PathKind = PathKind.PASS,
    /** Optional name drawn near the end of the path, e.g. "Curl". */
    val label: String = "",
)

/**
 * A shaded rectangle: a coverage zone, a gap, the box. It spans [x] to [x]+[w] across
 * and [y] to [y]+[h] downfield, so [x],[y] is the corner nearest the left sideline and
 * the offense's own goal (the bottom-left as drawn, offense moving up the screen).
 */
@Serializable
data class DiagramZone(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
)
