package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class AssignmentTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val coverage = content.pack("def-coverage-0-4")!!

    private fun mastered(concepts: Collection<String>) =
        concepts.associateWith { ConceptMastery(it, MasteryState.MASTERED, 100, 5, 1) }

    @Test fun `packs ship and validate`() {
        assertTrue(content.packs.size >= 10)
        assertTrue(content.conceptsOf(coverage).isNotEmpty())
    }

    @Test fun `a new learner gets every lesson, the games and the check`() {
        val plan = AssignmentPlanner.plan(listOf(coverage), content, emptyMap(), learnerLevel = 4)
        assertEquals(coverage.lessons, plan.stops.filter { it.kind == StopKind.LESSON }.map { it.lessonId })
        assertEquals(StopKind.CHECK, plan.stops.last().kind)
        assertTrue(plan.stops.any { it.kind == StopKind.GAME })
        assertTrue(plan.minutes > 0)
        assertFalse(plan.wellAboveLevel)
    }

    @Test fun `mastered lessons drop out of the plan`() {
        val first = content.lesson(coverage.lessons.first())!!
        val plan = AssignmentPlanner.plan(listOf(coverage), content, mastered(content.conceptsOf(first)), 4)
        assertTrue(plan.stops.none { it.lessonId == first.id })
        val all = AssignmentPlanner.plan(listOf(coverage), content, mastered(content.conceptsOf(coverage)), 4)
        assertEquals(0, all.lessons)
        assertEquals(StopKind.CHECK, all.stops.last().kind)
    }

    @Test fun `lessons well above the learner are flagged`() {
        val disguise = content.pack("def-disguise-rotations")!!
        assertTrue(AssignmentPlanner.plan(listOf(disguise), content, emptyMap(), learnerLevel = 2).wellAboveLevel)
        assertFalse(AssignmentPlanner.plan(listOf(disguise), content, emptyMap(), learnerLevel = 5).wellAboveLevel)
    }

    @Test fun `combined packs keep curriculum order without duplicates`() {
        val tampa = content.pack("def-43-tampa")!!
        // Warm-ups go first by design; the pack lessons themselves follow the curriculum.
        val ids = AssignmentPlanner.plan(listOf(tampa, coverage), content, emptyMap(), 4).stops.filter { it.kind == StopKind.LESSON }.mapNotNull { it.lessonId }
        assertEquals(ids.distinct(), ids)
        assertEquals(content.lessons.map { it.id }.filter { it in ids }, ids)
    }

    @Test fun `notes reject links and contact details`() {
        assertEquals(emptyList(), NoteRules.problems("Learn this before Tuesday's practice!"))
        assertTrue(NoteProblem.LINK in NoteRules.problems("see www.example.com"))
        assertTrue(NoteProblem.LINK in NoteRules.problems("go to https://x.y"))
        assertTrue(NoteProblem.CONTACT in NoteRules.problems("text me 555 123 4567"))
        assertTrue(NoteProblem.CONTACT in NoteRules.problems("coach@team.org"))
        assertTrue(NoteProblem.TOO_LONG in NoteRules.problems("x".repeat(201)))
    }

    @Test fun `status goes on track, behind, done`() {
        val a = Assignment("a1", fromName = "Coach Ray", packIds = listOf(coverage.id),
            targets = listOf(AssignmentTarget("l1", 5)), dueDay = 110, sentDay = 100)
        val full = AssignmentPlanner.plan(listOf(coverage), content, emptyMap(), 4)
        fun st(today: Long, plan: AssignmentPlan = full, ev: List<AssignmentEvent> = emptyList()) =
            AssignmentProgress.status(a, "l1", plan, ev, today)
        assertEquals(AssignmentStatus.ON_TRACK, st(101))
        assertEquals(AssignmentStatus.BEHIND, st(109))
        assertEquals(AssignmentStatus.BEHIND, st(111))
        val pass = AssignmentEvent("l1", "d", 0, "a1", AssignmentEventType.CHECK_RESULT, 85, 105, 0)
        val fail = pass.copy(pct = 70)
        assertEquals(AssignmentStatus.DONE, st(111, ev = listOf(pass)))
        assertEquals(AssignmentStatus.BEHIND, st(111, ev = listOf(fail)))
        assertEquals(1f, AssignmentProgress.fraction(a, "l1", full, listOf(pass)))
    }
}

class WarmupTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val coverage = content.pack("def-coverage-0-4")!!
    private val disguise = content.pack("def-disguise-rotations")!!
    private fun mastered(cs: Collection<String>) = cs.associateWith { ConceptMastery(it, MasteryState.MASTERED, 100, 5, 1) }

    @Test fun `a new learner gets warm-ups for missing basics, first and capped`() {
        val plan = AssignmentPlanner.plan(listOf(disguise), content, emptyMap(), 1, io.github.itstrue54.gridironacademy.model.Track.KID)
        assertTrue(plan.warmups in 1..AssignmentRules.MAX_WARMUP_LESSONS)
        assertTrue(plan.warmupsLeftOut > 0, "a Level 7 pack for a beginner leaves basics out and says so")
        val kinds = plan.stops.map { it.kind }
        assertEquals(kinds.sortedBy { it.ordinal }, kinds, "warm-ups, then lessons, then games, then the check")
        // Warm-ups come from the learner's own track.
        assertTrue(plan.stops.filter { it.kind == StopKind.WARMUP }.all { s ->
            content.levelOfLesson(s.lessonId!!)!!.track != io.github.itstrue54.gridironacademy.model.Track.ADULT
        })
    }

    @Test fun `mastered basics need no warm-up, and the switch turns them off`() {
        val basics = content.graph.prerequisites(content.conceptsOf(coverage))
        val plan = AssignmentPlanner.plan(listOf(coverage), content, mastered(basics), 4)
        assertEquals(0, plan.warmups)
        val off = AssignmentPlanner.plan(listOf(coverage), content, emptyMap(), 4, includeWarmups = false)
        assertEquals(0, off.warmups)
        assertEquals(0, off.warmupsLeftOut)
    }

    @Test fun `a player past the basics' levels gets no warm-ups for them`() {
        // Cover 0-4 builds on Levels 1-3; a Level 6 player is past all of it.
        val plan = AssignmentPlanner.plan(listOf(coverage), content, emptyMap(), 6, io.github.itstrue54.gridironacademy.model.Track.ADULT)
        assertEquals(0, plan.warmups)
        assertEquals(0, plan.warmupsLeftOut)
    }

    @Test fun `warm-ups count toward the plan and never repeat a pack lesson`() {
        val plan = AssignmentPlanner.plan(listOf(coverage), content, emptyMap(), 1, io.github.itstrue54.gridironacademy.model.Track.ADULT)
        assertTrue(plan.warmups > 0)
        val ids = plan.stops.mapNotNull { it.lessonId }
        assertEquals(ids.distinct(), ids)
        assertEquals(plan.stops.count { it.kind == StopKind.LESSON || it.kind == StopKind.WARMUP }, plan.lessons)
    }
}
