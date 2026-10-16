package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.engine.Rng
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A believable account to build and demo the Coach app against before the backend
 * exists (SPEC 8.2 is still open): a parent who also coaches a high-school team.
 * Every name is fictional. Generated from [seed] and [today] only, so the same day
 * always shows the same dashboard and tests can pin numbers.
 */
class DemoCoachRepository(
    private val content: Content,
    private val today: Long,
    seed: Long = DEMO_SEED,
) : CoachRepository {

    private val state = MutableStateFlow(build(Rng(seed)))
    override val snapshot: StateFlow<CoachSnapshot> = state.asStateFlow()

    override suspend fun send(draft: Assignment): SendResult {
        val problems = NoteRules.problems(draft.note)
        if (problems.isNotEmpty()) return SendResult.Rejected("note: ${problems.joinToString()}")
        if (draft.targets.isEmpty() || draft.packIds.isEmpty()) return SendResult.Rejected("nothing to send")
        var sent: Assignment? = null
        state.update { s ->
            val a = draft.copy(id = "a${s.assignments.size + 1}", sentDay = today, fromName = s.account.displayName)
            sent = a
            s.copy(assignments = s.assignments + a)
        }
        return SendResult.Sent(sent!!)
    }

    private fun build(rng: Rng): CoachSnapshot {
        val account = CoachAccount("demo-ray", "Coach Ray", isParent = true, isCoach = true)
        val team = Team("gulls", "Harbor City Gulls", variant = "High school", ageGroup = "Varsity", season = "Fall")
        val learners = ROSTER.map { r ->
            LinkedLearner(
                id = r.id, nickname = r.nickname, ageBand = r.band, track = r.band.track, level = r.level,
                streakDays = r.streak, lastActiveDay = today - r.idleDays,
                myChild = r.myChild, teamIds = if (r.onTeam) listOf(team.id) else emptyList(), positions = r.positions,
            )
        }
        val attempts = ROSTER.associate { r -> r.id to attemptsFor(r, rng) }

        val coverage = content.pack("def-coverage-0-4")
        val assignments = mutableListOf<Assignment>()
        val assignmentEvents = mutableListOf<AssignmentEvent>()
        if (coverage != null) {
            val members = ROSTER.filter { it.onTeam }
            val a = Assignment(
                id = "a1", teamId = team.id, fromName = account.displayName, packIds = listOf(coverage.id),
                targets = members.map { r ->
                    val m = Mastery.forLearner(attempts.getValue(r.id), content::conceptOfStep, today - 6)
                    AssignmentTarget(r.id, AssignmentPlanner.plan(listOf(coverage), content, m, r.level).lessons)
                },
                dueDay = today + 3, note = "Know these before Thursday's practice.", sentDay = today - 6,
            )
            assignments += a
            members.filter { it.skill >= DONE_SKILL }.forEachIndexed { i, r ->
                assignmentEvents += AssignmentEvent(r.id, "demo", 10_000L + i, a.id, AssignmentEventType.CHECK_RESULT,
                    pct = 80 + (r.skill * 20).toInt().coerceAtMost(20), epochDay = today - 1, at = (today - 1) * DAY_MS)
            }
        }
        return CoachSnapshot(account, listOf(team), learners, attempts, assignmentEvents, assignments, demo = true)
    }

    /** Lessons below the learner's level all played; the current level part-way. */
    private fun attemptsFor(r: DemoLearner, rng: Rng): List<AttemptEvent> {
        val out = mutableListOf<AttemptEvent>()
        var seq = 0L
        val lessons = ladder(r.band.track, r.level).flatMap { (lv, ls) ->
            val share = if (lv.number < r.level) 1.0 else r.currentShare
            ls.take((ls.size * share).toInt())
        }
        val span = lessons.size.coerceAtLeast(1)
        lessons.forEachIndexed { n, lesson ->
            // Spread the history over the last weeks, oldest lessons first.
            val day = today - r.idleDays - (HISTORY_DAYS * (span - 1 - n) / span)
            lesson.steps.filter { it !is Learn }.forEach { step ->
                val right = rng.nextInt(100) < (r.skill * 100).toInt()
                out += AttemptEvent(r.id, "demo-${r.id}", seq++, step.id, AttemptContext.LESSON,
                    pct = if (right) 1f else partial(step, rng), answer = if (right) null else wrongAnswer(step, rng),
                    epochDay = day, at = day * DAY_MS + seq)
                // Strong players have been reviewing: a second right answer on a later day.
                if (right && r.skill >= REVIEW_SKILL && rng.nextInt(2) == 0) {
                    val later = (day + 1 + rng.nextInt(3)).coerceAtMost(today - r.idleDays)
                    out += AttemptEvent(r.id, "demo-${r.id}", seq++, step.id, AttemptContext.REVIEW, 1f,
                        epochDay = later, at = later * DAY_MS + seq)
                }
            }
        }
        return out
    }

    /** The levels a learner on [track] climbs, up to [level], with their lessons. */
    private fun ladder(track: Track, level: Int): List<Pair<Level, List<Lesson>>> =
        (1..level).mapNotNull { n ->
            content.curriculum.levels.firstOrNull { it.number == n && it.track == track }
                ?: content.curriculum.levels.firstOrNull { it.number == n && it.track == Track.SHARED }
        }.map { lv -> lv to content.unitsOf(lv).flatMap { it.lessons } }

    private fun partial(step: Step, rng: Rng): Float =
        if (step is Match || step is Order) (1 + rng.nextInt(3)) / 4f else 0f

    private fun wrongAnswer(step: Step, rng: Rng): String? = when (step) {
        is MultipleChoice -> step.choices.filterIndexed { i, _ -> i != step.answer }.let { it[rng.nextInt(it.size)] }
        is ReadDefense -> step.choices.filterIndexed { i, _ -> i != step.answer }.let { it[rng.nextInt(it.size)] }
        is TrueFalse -> (!step.answer).toString()
        is Situation -> step.options.filter { it.quality != Quality.BEST }.let { it[rng.nextInt(it.size)].text }
        else -> null
    }

    private data class DemoLearner(
        val id: String, val nickname: String, val band: AgeBand, val level: Int,
        /** Chance of a right first try. */
        val skill: Double,
        /** How much of the current level is done. */
        val currentShare: Double,
        val streak: Int, val idleDays: Long, val myChild: Boolean, val onTeam: Boolean,
        val positions: List<PositionGroup>,
    )

    private companion object {
        const val DEMO_SEED = 2026L
        const val DAY_MS = 86_400_000L
        const val HISTORY_DAYS = 28
        /** Players at or above this skill have passed the demo assignment's check. */
        const val DONE_SKILL = 0.85
        const val REVIEW_SKILL = 0.7

        val ROSTER = listOf(
            // The coach's own children: one too young for the team, one on it.
            DemoLearner("leo", "Leo", AgeBand.AGE_8_9, 2, 0.8, 0.5, 4, 0, myChild = true, onTeam = false, positions = emptyList()),
            DemoLearner("jordan", "Jordan", AgeBand.AGE_13_17, 5, 0.86, 0.4, 9, 0, myChild = true, onTeam = true, positions = listOf(PositionGroup.LB)),
            DemoLearner("ace", "Ace", AgeBand.AGE_13_17, 6, 0.92, 0.6, 21, 0, myChild = false, onTeam = true, positions = listOf(PositionGroup.QB)),
            DemoLearner("moose", "Moose", AgeBand.AGE_13_17, 4, 0.62, 0.5, 2, 1, myChild = false, onTeam = true, positions = listOf(PositionGroup.OL)),
            DemoLearner("jet", "Jet", AgeBand.AGE_13_17, 5, 0.78, 0.3, 6, 0, myChild = false, onTeam = true, positions = listOf(PositionGroup.WR)),
            DemoLearner("tank", "Tank", AgeBand.AGE_13_17, 4, 0.55, 0.2, 0, 6, myChild = false, onTeam = true, positions = listOf(PositionGroup.DL)),
            DemoLearner("hawk", "Hawk", AgeBand.AGE_13_17, 5, 0.88, 0.7, 12, 0, myChild = false, onTeam = true, positions = listOf(PositionGroup.DB)),
            DemoLearner("dash", "Dash", AgeBand.AGE_13_17, 4, 0.7, 0.8, 5, 2, myChild = false, onTeam = true, positions = listOf(PositionGroup.RB)),
            DemoLearner("bolt", "Bolt", AgeBand.AGE_13_17, 4, 0.66, 0.4, 3, 1, myChild = false, onTeam = true, positions = listOf(PositionGroup.DB)),
            DemoLearner("tree", "Tree", AgeBand.AGE_13_17, 4, 0.6, 0.1, 0, 9, myChild = false, onTeam = true, positions = listOf(PositionGroup.TE)),
            DemoLearner("ice", "Ice", AgeBand.AGE_13_17, 5, 0.9, 0.5, 15, 0, myChild = false, onTeam = true, positions = listOf(PositionGroup.LB, PositionGroup.ST)),
            DemoLearner("rook", "Rook", AgeBand.AGE_13_17, 4, 0.5, 0.3, 1, 3, myChild = false, onTeam = true, positions = listOf(PositionGroup.DL)),
        )
    }
}
