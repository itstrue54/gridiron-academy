package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.progress.Assignment
import io.github.itstrue54.gridironacademy.progress.AssignmentEvent
import io.github.itstrue54.gridironacademy.progress.AttemptEvent
import kotlinx.coroutines.flow.StateFlow

/** The signed-in adult (SPEC 7.1). One account can be a parent and a coach at once. */
data class CoachAccount(
    val id: String,
    /** Shown to learners as "From Coach Ray". */
    val displayName: String,
    val isParent: Boolean,
    val isCoach: Boolean,
)

/** Position groups for the team view's filter (SPEC 7.3). */
enum class PositionGroup { QB, OL, WR, RB, TE, DL, LB, DB, ST }

data class Team(
    val id: String,
    val name: String,
    /** "High school", "Youth tackle", "Flag 5v5"... as the coach typed it. */
    val variant: String,
    val ageGroup: String,
    val season: String,
)

/** A learner this adult can see: their own child, a team member, or both. */
data class LinkedLearner(
    val id: String,
    val nickname: String,
    val ageBand: AgeBand,
    val track: Track,
    /** The level they're working on now. */
    val level: Int,
    val streakDays: Int,
    val lastActiveDay: Long?,
    /** True when this adult is their parent or guardian. */
    val myChild: Boolean,
    val teamIds: List<String> = emptyList(),
    val positions: List<PositionGroup> = emptyList(),
)

/** Everything a dashboard is computed from: the same events the learner app folds. */
data class CoachSnapshot(
    val account: CoachAccount,
    val teams: List<Team>,
    val learners: List<LinkedLearner>,
    val attempts: Map<String, List<AttemptEvent>>,
    val assignmentEvents: List<AssignmentEvent>,
    val assignments: List<Assignment>,
    /** True while the data is the built-in demo, not a real account. */
    val demo: Boolean,
) {
    fun learner(id: String): LinkedLearner? = learners.firstOrNull { it.id == id }
    fun team(id: String): Team? = teams.firstOrNull { it.id == id }
    fun membersOf(teamId: String): List<LinkedLearner> = learners.filter { teamId in it.teamIds }
    fun assignmentsFor(learnerId: String): List<Assignment> =
        assignments.filter { a -> a.targets.any { it.learnerId == learnerId } }
}

sealed interface SendResult {
    data class Sent(val assignment: Assignment) : SendResult
    data class Rejected(val reason: String) : SendResult
}

/**
 * The Coach app's data (SPEC 8.4). Screens read [snapshot] and compute every number with
 * the shared engine, so the learner app and the coach app always agree. The backend
 * (SPEC 8.2, still open) plugs in here; until then [DemoCoachRepository] stands in.
 */
interface CoachRepository {
    val snapshot: StateFlow<CoachSnapshot>

    /** Assign a new id, stamp the send day, and deliver to the targets. */
    suspend fun send(draft: Assignment): SendResult

    /** Fetch again (pull to refresh, after sign-in). The demo has nothing to fetch. */
    suspend fun refresh() {}
}
