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
        val ids = AssignmentPlanner.plan(listOf(tampa, coverage), content, emptyMap(), 4).stops.mapNotNull { it.lessonId }
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
