package io.github.itstrue54.gridironacademy.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// One class per table in backend/supabase/migrations, field for column. Kept apart from
// the app's own models so the wire format can change without touching the engine.

@Serializable
data class AccountRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class LearnerRow(
    val id: String = "",
    val nickname: String,
    @SerialName("age_band") val ageBand: String,
    val track: String,
    @SerialName("managed_by") val managedBy: String? = null,
    @SerialName("own_account") val ownAccount: String? = null,
)

@Serializable
data class TeamRow(
    val id: String = "",
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val variant: String = "",
    @SerialName("age_group") val ageGroup: String = "",
    val season: String = "",
)

@Serializable
data class StaffRow(
    @SerialName("team_id") val teamId: String,
    @SerialName("account_id") val accountId: String,
    val role: String,
)

@Serializable
data class MemberRow(
    @SerialName("team_id") val teamId: String,
    @SerialName("learner_id") val learnerId: String,
    val status: String = "pending",
    val positions: List<String> = emptyList(),
    val groups: List<String> = emptyList(),
)

@Serializable
data class EventRow(
    @SerialName("learner_id") val learnerId: String,
    @SerialName("device_id") val deviceId: String,
    val seq: Long,
    val payload: JsonObject,
)

@Serializable
data class AssignmentRow(
    val id: String = "",
    val version: Int = 1,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("from_name") val fromName: String,
    @SerialName("pack_ids") val packIds: List<String>,
    @SerialName("due_day") val dueDay: Long? = null,
    @SerialName("pass_pct") val passPct: Int = 80,
    val note: String = "",
    @SerialName("sent_day") val sentDay: Long,
)

@Serializable
data class TargetRow(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("learner_id") val learnerId: String,
    @SerialName("lessons_at_send") val lessonsAtSend: Int = 0,
)

/** Everything one refresh reads, already filtered by row-level security. */
data class ServerRows(
    val me: AccountRow,
    val learners: List<LearnerRow>,
    val teams: List<TeamRow>,
    val staff: List<StaffRow>,
    val members: List<MemberRow>,
    val events: List<EventRow>,
    val assignments: List<AssignmentRow>,
    val targets: List<TargetRow>,
)
