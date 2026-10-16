package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.*

/** Dashboard limits (SPEC 7.3). */
object DashboardRules {
    /** "3 to work on". */
    const val WEAKEST_COUNT = 3
    /** One miss isn't a weakness: a concept needs this many first tries to be listed. */
    const val MIN_ATTEMPTS_FOR_WEAKEST = 2
    const val ACTIVITY_DAYS = 7
    const val MOST_MISSED_ITEMS = 3
    /** A step needs this many first tries before its miss rate means anything. */
    const val MIN_TRIES_FOR_MISS_RATE = 2
}

/** One learner's line on an assignment. */
data class AssignmentLine(
    val assignment: Assignment,
    val learnerId: String,
    val status: AssignmentStatus,
    val fraction: Float,
    val planNow: AssignmentPlan,
)

data class DayActivity(val epochDay: Long, val answers: Int, val minutes: Int)

/** A step the team gets wrong, with the wrong answer they give most (SPEC 7.3). */
data class MissedItem(val step: Step, val tries: Int, val missRate: Float, val commonWrongAnswer: String?)

data class HeatRow(val learner: LinkedLearner, val cells: List<ConceptMastery>, val readinessPct: Int)

/**
 * Everything the Coach screens show, computed from one [CoachSnapshot] with the shared
 * engine (SPEC 7.3: "learner app and coach app always agree"). Pure: no clock, no I/O.
 */
class CoachDashboard(val snapshot: CoachSnapshot, private val content: Content, private val today: Long) {

    private val masteryCache = mutableMapOf<String, Map<String, ConceptMastery>>()

    fun attempts(learnerId: String): List<AttemptEvent> = snapshot.attempts[learnerId].orEmpty()

    fun mastery(learnerId: String): Map<String, ConceptMastery> = masteryCache.getOrPut(learnerId) {
        Mastery.forLearner(attempts(learnerId), content::conceptOfStep, today)
    }

    fun mastery(learnerId: String, concept: String): ConceptMastery =
        mastery(learnerId)[concept] ?: ConceptMastery(concept, MasteryState.NEW, 0, 0, null)

    /** The level a learner is on, as content: their track's level, or the shared one. */
    fun levelOf(learner: LinkedLearner): Level? =
        content.curriculum.levels.firstOrNull { it.number == learner.level && it.track == learner.track }
            ?: content.curriculum.levels.firstOrNull { it.number == learner.level && it.track == Track.SHARED }

    /** The concepts of the learner's current level, for the mastery strip. */
    fun levelConcepts(learner: LinkedLearner): List<String> =
        levelOf(learner)?.let { lv -> content.unitsOf(lv).flatMap { u -> u.lessons.flatMap { content.conceptsOf(it) } }.distinct() }
            .orEmpty()

    /** "3 to work on": touched but not secure, lowest score first; ties by concept id. */
    fun weakest(learnerId: String): List<ConceptMastery> =
        mastery(learnerId).values
            .filter { it.attempts >= DashboardRules.MIN_ATTEMPTS_FOR_WEAKEST && !it.secure }
            .sortedWith(compareBy<ConceptMastery>({ it.state == MasteryState.RUSTY }, { it.score }, { it.concept }))
            .take(DashboardRules.WEAKEST_COUNT)

    /** A pack that teaches [concept], for one-tap "Assign practice". */
    fun packFor(concept: String): SchemePack? = content.packs.firstOrNull { concept in content.conceptsOf(it) }

    fun activity(learnerId: String): List<DayActivity> {
        val byDay = attempts(learnerId).groupBy { it.epochDay }
        return (0 until DashboardRules.ACTIVITY_DAYS).map { back ->
            val day = today - back
            val xs = byDay[day].orEmpty()
            DayActivity(day, xs.size, (xs.sumOf { it.msSpent.toLong() } / 60_000).toInt())
        }
    }

    fun minutesThisWeek(learnerId: String): Int = activity(learnerId).sumOf { it.minutes }

    fun packsOf(a: Assignment): List<SchemePack> = a.packIds.mapNotNull { content.pack(it) }

    fun line(a: Assignment, learnerId: String): AssignmentLine {
        val learner = snapshot.learner(learnerId)
        val plan = AssignmentPlanner.plan(packsOf(a), content, mastery(learnerId), learner?.level ?: 1)
        return AssignmentLine(
            a, learnerId,
            AssignmentProgress.status(a, learnerId, plan, snapshot.assignmentEvents, today),
            AssignmentProgress.fraction(a, learnerId, plan, snapshot.assignmentEvents),
            plan,
        )
    }

    fun lines(learnerId: String): List<AssignmentLine> =
        snapshot.assignmentsFor(learnerId).map { line(it, learnerId) }
            .sortedWith(compareBy({ it.status == AssignmentStatus.DONE }, { it.assignment.dueDay ?: Long.MAX_VALUE }))

    /** Team assignments still open, with how many members have passed the check. */
    fun teamProgress(teamId: String): List<Pair<Assignment, Int>> =
        snapshot.assignments.filter { it.teamId == teamId }.map { a ->
            a to a.targets.count { AssignmentProgress.done(a, it.learnerId, snapshot.assignmentEvents) }
        }

    /** The status pill on a roster row: the worst open assignment, or Done, or none. */
    fun overallStatus(learnerId: String): AssignmentStatus? {
        val ls = lines(learnerId)
        return when {
            ls.isEmpty() -> null
            ls.any { it.status == AssignmentStatus.BEHIND } -> AssignmentStatus.BEHIND
            ls.any { it.status == AssignmentStatus.ON_TRACK } -> AssignmentStatus.ON_TRACK
            else -> AssignmentStatus.DONE
        }
    }

    /** Percent of a pack's concepts the learner has mastered (SPEC 7.3 readiness). */
    fun readiness(learnerId: String, pack: SchemePack): Int {
        val concepts = content.conceptsOf(pack)
        if (concepts.isEmpty()) return 0
        return concepts.count { mastery(learnerId, it).secure } * 100 / concepts.size
    }

    /** Learners x the pack's concepts (DESIGN 14.3), optionally one position group. */
    fun heatMap(teamId: String, pack: SchemePack, group: PositionGroup? = null): List<HeatRow> {
        val concepts = content.conceptsOf(pack)
        return snapshot.membersOf(teamId)
            .filter { group == null || group in it.positions }
            .map { l -> HeatRow(l, concepts.map { mastery(l.id, it) }, readiness(l.id, pack)) }
    }

    /** The concept's steps the team misses most, with the usual wrong answer. */
    fun mostMissed(teamId: String, concept: String): List<MissedItem> {
        val members = snapshot.membersOf(teamId).map { it.id }.toSet()
        val steps = content.lessons.flatMap { it.steps }.filter { it.concept == concept && it !is Learn }
        val tries = members.flatMap { attempts(it) }.filter { it.firstTry }.groupBy { it.stepId }
        return steps.mapNotNull { s ->
            val xs = tries[s.id].orEmpty()
            if (xs.size < DashboardRules.MIN_TRIES_FOR_MISS_RATE) return@mapNotNull null
            val misses = xs.filter { !it.correct }
            val common = misses.mapNotNull { it.answer }.groupingBy { it }.eachCount()
                .entries.sortedWith(compareBy({ -it.value }, { it.key })).firstOrNull()?.key
            MissedItem(s, xs.size, misses.size.toFloat() / xs.size, common)
        }.filter { it.missRate > 0f }
            .sortedWith(compareBy({ -it.missRate }, { it.step.id }))
            .take(DashboardRules.MOST_MISSED_ITEMS)
    }

    /** The per-learner preview before Send (DESIGN 14.4). */
    fun preview(packs: List<SchemePack>, learnerIds: List<String>): List<Pair<LinkedLearner, AssignmentPlan>> =
        learnerIds.mapNotNull { snapshot.learner(it) }.map { l ->
            l to AssignmentPlanner.plan(packs, content, mastery(l.id), l.level)
        }

    /** Turn a draft into what [CoachRepository.send] takes, with each plan's size. */
    fun draft(packs: List<SchemePack>, learnerIds: List<String>, dueDay: Long?, passPct: Int, note: String, teamId: String?) =
        Assignment(
            id = "", teamId = teamId, fromName = snapshot.account.displayName, packIds = packs.map { it.id },
            targets = preview(packs, learnerIds).map { (l, plan) -> AssignmentTarget(l.id, plan.lessons) },
            dueDay = dueDay, passPct = passPct, note = note.trim(), sentDay = today,
        )
}
