package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.data.supabase.*
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.*
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class SnapshotAssemblerTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val today = 20_000L
    private val me = AccountRow("ray", "Coach Ray")

    private fun row(e: EventPayload, seq: Long) = EventRow(
        "x", "dev", seq, EventPayload.json.encodeToJsonElement(EventPayload.serializer(), e).jsonObject,
    )

    private fun attempt(learner: String, step: String, day: Long, seq: Long) =
        row(EventPayload.Attempt(AttemptEvent(learner, "dev", seq, step, AttemptContext.LESSON, 1f, epochDay = day, at = day * 1000)), seq)

    private val v4Step = content.lessons.first { it.id == "v4-cover3" }.steps.first().id
    private val k1Step = content.lessons.first { it.id == "k1-field" }.steps.first().id

    private val rows = ServerRows(
        me = me,
        learners = listOf(
            LearnerRow("leo", "Leo", "8-9", "kid", managedBy = "ray"),
            LearnerRow("mia", "Mia", "10-12", "kid", managedBy = "pat"),
            LearnerRow("bad", "Odd", "99", "kid", managedBy = "ray"),
        ),
        teams = listOf(TeamRow("gulls", "ray", "Harbor City Gulls", "Youth"), TeamRow("other", "pat", "Their team")),
        staff = listOf(StaffRow("gulls", "ray", "owner"), StaffRow("other", "pat", "owner")),
        members = listOf(MemberRow("gulls", "mia", "approved", listOf("LB", "KR")), MemberRow("gulls", "leo", "pending")),
        events = listOf(
            attempt("mia", v4Step, today - 1, 0), attempt("mia", k1Step, today - 2, 1), attempt("mia", k1Step, today - 4, 2),
            row(EventPayload.Assignment(AssignmentEvent("mia", "dev", 9, "a1", AssignmentEventType.CHECK_RESULT, 90, today, 0)), 9),
            EventRow("mia", "dev", 10, kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("from_the_future")) }),
        ),
        assignments = listOf(AssignmentRow("a1", 1, "gulls", "ray", "Coach Ray", listOf("def-quarters"), today + 3, 80, "", today - 1)),
        targets = listOf(TargetRow("a1", "mia", 2)),
    )

    @Test fun `rows become the snapshot the screens read`() {
        val s = SnapshotAssembler.assemble(rows, content, today)
        assertFalse(s.demo)
        assertEquals("Coach Ray", s.account.displayName)
        assertTrue(s.account.isParent && s.account.isCoach)
        // Only teams I staff; unknown age bands are skipped, my children first.
        assertEquals(listOf("gulls"), s.teams.map { it.id })
        assertEquals(listOf("leo", "mia"), s.learners.map { it.id })
        val mia = s.learner("mia")!!
        assertEquals(Track.KID, mia.track)
        assertEquals(listOf("gulls"), mia.teamIds)
        assertEquals(listOf(PositionGroup.LB), mia.positions)
        // Leo's membership is pending: not on the team yet.
        assertEquals(emptyList(), s.learner("leo")!!.teamIds)
    }

    @Test fun `level, streak and last day are folded from events, unknown payloads skipped`() {
        val s = SnapshotAssembler.assemble(rows, content, today)
        val mia = s.learner("mia")!!
        assertEquals(4, mia.level)
        // Mon, Wed, Thu: the weekly rest day covers Tuesday, as on Mia's own Home.
        assertEquals(3, mia.streakDays)
        assertEquals(today - 1, mia.lastActiveDay)
        assertEquals(3, s.attempts.getValue("mia").size)
        assertEquals(1, s.assignmentEvents.size)
        assertEquals(listOf(AssignmentTarget("mia", 2)), s.assignments.single().targets)
        assertTrue(s.assignments.single().includeWarmups, "warm-ups default on")
        val off = SnapshotAssembler.assemble(rows.copy(assignments = rows.assignments.map { it.copy(includeWarmups = false) }), content, today)
        assertFalse(off.assignments.single().includeWarmups)
        assertEquals(1, s.learner("leo")!!.level)
    }

    // today (epoch day 20000) is a Friday; its week began on Monday, 19996.
    private val mon = today - 4

    private fun coachStreak(vararg days: Long): Int {
        val events = days.mapIndexed { i, d -> attempt("mia", k1Step, d, i.toLong()) }
        return SnapshotAssembler.assemble(rows.copy(events = events), content, today).learner("mia")!!.streakDays
    }

    /** What the learner's Home shows after practising on [days] (SPEC 4.7). */
    private fun homeStreak(vararg days: Long): Int =
        Streak.current(days.fold(Progress()) { p, d -> p.touch(d) }, today)

    @Test fun `a one-day gap covered by the rest day keeps the coach streak`() {
        assertEquals(4, coachStreak(mon, mon + 1, mon + 3, today), "missed Wednesday, played today")
        assertEquals(3, coachStreak(mon, mon + 1, mon + 2), "missed yesterday, today not played yet")
    }

    @Test fun `a second missed day in the same week breaks the coach streak`() {
        assertEquals(1, coachStreak(mon, mon + 2, today), "rest spent on Tuesday, Thursday missed too")
        assertEquals(0, coachStreak(mon, mon + 2), "same, today not played yet: lapsed")
    }

    @Test fun `two missed days in a row break the coach streak`() {
        assertEquals(2, coachStreak(mon, mon + 3, today))
        assertEquals(0, coachStreak(mon, mon + 1))
    }

    @Test fun `a lapsed streak reads 0 and a learner with no events reads 0`() {
        assertEquals(0, coachStreak(today - 10, today - 9, today - 8, today - 7, today - 6, today - 5))
        val leo = SnapshotAssembler.assemble(rows, content, today).learner("leo")!!
        assertEquals(0, leo.streakDays)
        assertNull(leo.lastActiveDay)
        assertEquals(0, SnapshotAssembler.streak(emptySet(), today))
    }

    @Test fun `the coach streak is the learner's Home streak`() {
        val histories = listOf(
            longArrayOf(mon, mon + 1, mon + 3, today),
            longArrayOf(mon, mon + 2, today),
            longArrayOf(mon - 3, mon - 1, mon, mon + 2, mon + 3),
            longArrayOf(mon - 1, mon + 1, mon + 2, mon + 3),
            longArrayOf(today - 2),
            longArrayOf(),
        )
        for (h in histories) assertEquals(homeStreak(*h), coachStreak(*h), h.joinToString())
    }
}
