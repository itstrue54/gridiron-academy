package io.github.itstrue54.gridironacademy.data.supabase

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.data.*
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.*

/**
 * Server rows -> the [CoachSnapshot] every screen already reads. Pure, so all of the
 * Supabase path's logic is unit-tested without a network. A learner's level, streak and
 * last day are folded from their events (SPEC 8.1): the server stores no derived state.
 */
object SnapshotAssembler {

    fun assemble(rows: ServerRows, content: Content, today: Long): CoachSnapshot {
        val me = rows.me.id
        val payloads = rows.events.mapNotNull { decode(it) }
        val attempts = payloads.filterIsInstance<EventPayload.Attempt>().map { it.event }.groupBy { it.learnerId }
        val assignmentEvents = payloads.filterIsInstance<EventPayload.Assignment>().map { it.event }

        val myTeams = rows.staff.filter { it.accountId == me }.map { it.teamId }.toSet()
        val approved = rows.members.filter { it.status == "approved" }
        val stepLevel = content.lessons.flatMap { l ->
            val n = content.levelOfLesson(l.id)?.number ?: 1
            l.steps.map { it.id to n }
        }.toMap()

        val learners = rows.learners.mapNotNull { r ->
            val band = AgeBand.entries.firstOrNull { it.label == r.ageBand } ?: return@mapNotNull null
            val mine = approved.filter { it.learnerId == r.id }
            val xs = attempts[r.id].orEmpty()
            LinkedLearner(
                id = r.id,
                nickname = r.nickname,
                ageBand = band,
                track = if (r.track == "kid") Track.KID else Track.ADULT,
                level = xs.maxOfOrNull { stepLevel[it.stepId] ?: 1 } ?: 1,
                streakDays = streak(xs.map { it.epochDay }.toSet(), today),
                lastActiveDay = xs.maxOfOrNull { it.epochDay },
                myChild = r.managedBy == me,
                teamIds = mine.map { it.teamId },
                positions = mine.flatMap { it.positions }.mapNotNull { p -> PositionGroup.entries.firstOrNull { it.name == p } }.distinct(),
            )
        }.sortedWith(compareBy({ !it.myChild }, { it.nickname.lowercase() }))

        val teams = rows.teams.filter { it.id in myTeams }.map {
            Team(it.id, it.name, it.variant, it.ageGroup, it.season)
        }.sortedBy { it.name.lowercase() }

        val targets = rows.targets.groupBy { it.assignmentId }
        val assignments = rows.assignments.map { a ->
            Assignment(
                id = a.id, version = a.version, teamId = a.teamId, fromName = a.fromName, packIds = a.packIds,
                targets = targets[a.id].orEmpty().map { AssignmentTarget(it.learnerId, it.lessonsAtSend) },
                dueDay = a.dueDay, passPct = a.passPct, note = a.note, sentDay = a.sentDay,
            )
        }.sortedWith(compareBy({ it.sentDay }, { it.id }))

        return CoachSnapshot(
            account = CoachAccount(
                id = me,
                displayName = rows.me.displayName,
                isParent = rows.learners.any { it.managedBy == me },
                isCoach = myTeams.isNotEmpty(),
            ),
            teams = teams,
            learners = learners,
            attempts = attempts,
            assignmentEvents = assignmentEvents,
            assignments = assignments,
            demo = false,
        )
    }

    /** Days in a row with practice, ending today or yesterday (a missed today isn't a break yet). */
    fun streak(days: Set<Long>, today: Long): Int {
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    /** A payload this app version can't read is skipped, never a crash. */
    private fun decode(row: EventRow): EventPayload? =
        runCatching { EventPayload.json.decodeFromJsonElement(EventPayload.serializer(), row.payload) }.getOrNull()
}
