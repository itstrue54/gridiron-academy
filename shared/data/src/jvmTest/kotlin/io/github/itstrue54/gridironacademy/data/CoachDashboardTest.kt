package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class CoachDashboardTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val today = 20_000L
    private val snap = DemoCoachRepository(content, today).snapshot.value
    private val d = CoachDashboard(snap, content, today)
    private val coverage = content.pack("def-coverage-0-4")!!

    @Test fun `weakest are touched, not secure, lowest first`() {
        val w = d.weakest("tank")
        assertTrue(w.size in 1..DashboardRules.WEAKEST_COUNT)
        assertTrue(w.all { it.attempts > 0 && !it.secure })
        assertEquals(w.sortedBy { it.score }.map { it.score }, w.filter { it.state != MasteryState.RUSTY }.map { it.score } + w.filter { it.state == MasteryState.RUSTY }.map { it.score })
    }

    @Test fun `stronger players are readier`() {
        assertTrue(d.readiness("ace", coverage) > d.readiness("rook", coverage))
    }

    @Test fun `heat map is the team by the pack's concepts and filters by position`() {
        val rows = d.heatMap("gulls", coverage)
        assertEquals(snap.membersOf("gulls").size, rows.size)
        assertTrue(rows.all { it.cells.size == content.conceptsOf(coverage).size })
        assertTrue(d.heatMap("gulls", coverage, PositionGroup.DB).all { PositionGroup.DB in it.learner.positions })
    }

    @Test fun `most missed items come with the usual wrong answer`() {
        val concept = content.conceptsOf(coverage).first { d.mostMissed("gulls", it).isNotEmpty() }
        val items = d.mostMissed("gulls", concept)
        assertTrue(items.size <= DashboardRules.MOST_MISSED_ITEMS)
        assertEquals(items.sortedByDescending { it.missRate }.map { it.missRate }, items.map { it.missRate })
        assertTrue(items.any { it.commonWrongAnswer != null })
    }

    @Test fun `a week of activity, today first`() {
        val a = d.activity("jordan")
        assertEquals(DashboardRules.ACTIVITY_DAYS, a.size)
        assertEquals(today, a.first().epochDay)
    }

    @Test fun `the draft carries each learner's plan size`() {
        val draft = d.draft(listOf(coverage), listOf("leo", "ace"), today + 7, 80, "  Read the safeties.  ", null)
        assertEquals(listOf("leo", "ace"), draft.targets.map { it.learnerId })
        assertEquals("Read the safeties.", draft.note)
        // Leo is on Level 2: Level 4 is in reach, Level 7 disguise is flagged.
        assertFalse(d.preview(listOf(coverage), listOf("leo")).single().second.wellAboveLevel)
        assertTrue(d.preview(listOf(content.pack("def-disguise-rotations")!!), listOf("leo")).single().second.wellAboveLevel)
    }
}
