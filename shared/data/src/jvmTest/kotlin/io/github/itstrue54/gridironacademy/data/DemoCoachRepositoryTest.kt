package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.progress.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DemoCoachRepositoryTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val today = 20_000L

    @Test fun `the demo is the same every time for the same day`() {
        val a = DemoCoachRepository(content, today).snapshot.value
        val b = DemoCoachRepository(content, today).snapshot.value
        assertEquals(a, b)
        assertTrue(a.demo)
    }

    @Test fun `a parent who coaches has two children, a team and an assignment in flight`() {
        val s = DemoCoachRepository(content, today).snapshot.value
        assertTrue(s.account.isParent && s.account.isCoach)
        assertEquals(2, s.learners.count { it.myChild })
        val team = s.teams.single()
        assertTrue(s.membersOf(team.id).size >= 10)
        val a = s.assignments.single()
        val statuses = a.targets.map { t ->
            val m = Mastery.forLearner(s.attempts.getValue(t.learnerId), content::conceptOfStep, today)
            val plan = AssignmentPlanner.plan(a.packIds.map { content.pack(it)!! }, content, m, s.learner(t.learnerId)!!.level)
            AssignmentProgress.status(a, t.learnerId, plan, s.assignmentEvents, today)
        }.toSet()
        // A demo is only useful if the dashboard shows more than one kind of player.
        assertTrue(AssignmentStatus.DONE in statuses, "$statuses")
        assertTrue(statuses.size >= 2, "$statuses")
    }

    @Test fun `every attempt points at a real step`() {
        val s = DemoCoachRepository(content, today).snapshot.value
        val stepIds = content.lessons.flatMap { l -> l.steps.map { it.id } }.toSet()
        s.attempts.values.flatten().forEach { assertTrue(it.stepId in stepIds, it.stepId) }
        assertTrue(s.attempts.values.flatten().all { it.epochDay <= today })
    }

    @Test fun `send numbers assignments and refuses notes with contact details`() = runBlocking {
        val repo = DemoCoachRepository(content, today)
        val draft = Assignment("", fromName = "", packIds = listOf("def-quarters"),
            targets = listOf(AssignmentTarget("ace", 2)), sentDay = 0)
        val sent = repo.send(draft)
        assertIs<SendResult.Sent>(sent)
        assertEquals("a2", sent.assignment.id)
        assertEquals(today, sent.assignment.sentDay)
        assertEquals("Coach Ray", sent.assignment.fromName)
        assertIs<SendResult.Rejected>(repo.send(draft.copy(note = "call me 555 123 4567")))
        assertEquals(2, repo.snapshot.value.assignments.size)
    }

    @Test fun `sent assignments are handed out to keep and come back on restart`() = runBlocking {
        var kept = emptyList<Assignment>()
        val repo = DemoCoachRepository(content, today, onSent = { kept = it })
        repo.send(Assignment("", fromName = "", packIds = listOf("def-quarters"), targets = listOf(AssignmentTarget("ace", 2)), sentDay = 0))
        assertEquals(listOf("a2"), kept.map { it.id })
        val again = DemoCoachRepository(content, today, sent = kept)
        assertEquals(listOf("a1", "a2"), again.snapshot.value.assignments.map { it.id })
    }
}
