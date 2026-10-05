package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.engine.*
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class EngineTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val kid = content.level("kid-1")
    private val adult = content.level("adult-1")

    private fun mc(id: String, ans: Int = 0) = MultipleChoice(id, "p", listOf("a", "b", "c"), ans, "e")

    // ---- Rng ----
    @Test fun `rng is deterministic and never out of bound`() {
        val a = Rng(42); val b = Rng(42)
        repeat(100) { assertEquals(a.nextInt(7), b.nextInt(7)) }
        val r = Rng(7); repeat(1000) { assertTrue(r.nextInt(5) in 0..4) }
        assertEquals((1..10).toList().sorted(), Rng(3).shuffled((1..10).toList()).sorted())
    }

    // ---- Grader ----
    @Test fun `multiple choice and true false grade`() {
        assertTrue(Grader.grade(mc("1", 1), Answer.Choice(1)).correct)
        assertFalse(Grader.grade(mc("1", 1), Answer.Choice(0)).correct)
        assertTrue(Grader.grade(TrueFalse("t", "s", true, "e"), Answer.Bool(true)).correct)
        assertFalse(Grader.grade(TrueFalse("t", "s", true, "e"), Answer.Seen).correct)
    }

    @Test fun `match gives partial credit`() {
        val m = Match("m", "p", listOf(MatchPair("a", "1"), MatchPair("b", "2")), "e")
        assertEquals(0.5f, Grader.grade(m, Answer.Matches(mapOf("a" to "1", "b" to "1"))).credit)
        assertTrue(Grader.grade(m, Answer.Matches(mapOf("a" to "1", "b" to "2"))).correct)
    }

    @Test fun `order credit is by position`() {
        val o = Order("o", "p", listOf("1", "2", "3", "4"), "e")
        assertTrue(Grader.grade(o, Answer.Ordering(listOf("1", "2", "3", "4"))).correct)
        assertEquals(0.5f, Grader.grade(o, Answer.Ordering(listOf("1", "2", "4", "3"))).credit)
    }

    @Test fun `diagram tap penalizes extra taps`() {
        val d = Diagram(players = listOf("a", "b", "c").map { DiagramPlayer(it, it, Side.OFFENSE, 0f, 0f) })
        val t = DiagramTap("d", "p", d, setOf("a", "b"), "e")
        assertTrue(Grader.grade(t, Answer.Taps(setOf("a", "b"))).correct)
        assertEquals(0.5f, Grader.grade(t, Answer.Taps(setOf("a"))).credit)
        assertEquals(0.5f, Grader.grade(t, Answer.Taps(setOf("a", "b", "c"))).credit)
        assertEquals(0f, Grader.grade(t, Answer.Taps(setOf("c"))).credit)
        assertEquals(0f, Grader.grade(t, Answer.Taps(emptySet())).credit)
    }

    @Test fun `situation grades best ok poor`() {
        val s = Situation("s", GameState(1, 100, 4, 1, 50, 0), "p", listOf(
            SituationOption("go", Quality.BEST, "w1"), SituationOption("fg", Quality.OK, "w2"), SituationOption("punt", Quality.POOR, "w3")), "e")
        assertTrue(Grader.grade(s, Answer.Choice(0)).correct)
        assertEquals(Grader.OK_CREDIT, Grader.grade(s, Answer.Choice(1)).credit)
        assertFalse(Grader.grade(s, Answer.Choice(1)).correct)
        assertEquals(0f, Grader.grade(s, Answer.Choice(2)).credit)
        assertTrue(Grader.grade(s, Answer.Choice(1)).feedback.startsWith("w2"))
    }

    // ---- LessonSession ----
    @Test fun `session requeues a miss once and scores first try only`() {
        val ses = LessonSession(listOf(Learn("l", "t", "b"), mc("a"), mc("b")))
        ses.submit(Answer.Seen); ses.next()
        ses.submit(Answer.Choice(2)); ses.next()          // a: wrong, requeued
        ses.submit(Answer.Choice(0)); ses.next()          // b: right
        assertEquals("a", ses.current?.id)
        ses.submit(Answer.Choice(2)); ses.next()          // a again: wrong again, not requeued twice
        assertTrue(ses.finished)
        assertEquals(50, ses.scorePct)
        assertEquals(setOf("a"), ses.missedIds)
    }

    @Test fun `stars thresholds`() {
        assertEquals(3, LessonSession.stars(100)); assertEquals(2, LessonSession.stars(80)); assertEquals(1, LessonSession.stars(79))
        assertEquals(3, LessonSession.stars(95)); assertEquals(2, LessonSession.stars(94))
        assertEquals(1, LessonSession.stars(60)); assertEquals(0, LessonSession.stars(59))
    }

    @Test fun `every shipped lesson can be played perfectly`() {
        for (lv in content.curriculum.levels) for (u in content.unitsOf(lv)) for (l in u.lessons) {
            val ses = LessonSession(l.steps)
            while (!ses.finished) { ses.submit(perfectAnswer(ses.current!!)); ses.next() }
            assertEquals(100, ses.scorePct, l.id)
        }
    }

    // ---- SpeedRound ----
    @Test fun `speed round streak bonus and time limit`() {
        val qs = (1..6).map { mc("q$it") }
        val r = SpeedRound(qs, limitMillis = 10_000)
        r.answer(Answer.Choice(0), 1000); r.answer(Answer.Choice(0), 2000)
        assertEquals(20, r.points)
        r.answer(Answer.Choice(0), 3000)       // streak 3: 20 points
        assertEquals(40, r.points)
        r.answer(Answer.Choice(1), 4000)       // wrong: streak resets
        assertEquals(0, r.streak); assertEquals(3, r.bestStreak)
        assertNull(r.answer(Answer.Choice(0), 10_000))
        assertTrue(r.isOver(10_000))
        assertEquals(75, r.accuracyPct)
    }

    @Test fun `speed round rejects diagram questions`() {
        assertFailsWith<IllegalArgumentException> {
            SpeedRound(listOf(DiagramTap("d", "p", Diagram(players = listOf(DiagramPlayer("a", "a", Side.OFFENSE, 0f, 0f))), setOf("a"), "e")))
        }
    }

    // ---- GameDay ----
    @Test fun `game day totals coach points`() {
        val sits = (1..4).map { i ->
            Situation("s$i", GameState(1, 100, 4, 1, 50, 0), "p", listOf(
                SituationOption("a", Quality.BEST, "w"), SituationOption("b", Quality.OK, "w"), SituationOption("c", Quality.POOR, "w")), "e")
        }
        val g = GameDay(sits)
        g.decide(0); g.decide(1); g.decide(2); g.decide(0)
        assertTrue(g.finished)
        assertEquals(2.5f, g.coachPoints); assertEquals(63, g.scorePct)
    }

    // ---- LevelTest ----
    @Test fun `level test and game pools are deterministic and the right kind`() {
        for (lv in content.curriculum.levels) {
            val units = content.unitsOf(lv)
            val t1 = LevelTest.test(units, lv.gate, 99).map { it.id }
            assertEquals(t1, LevelTest.test(units, lv.gate, 99).map { it.id })
            assertEquals(lv.gate.testQuestions, t1.size)
            assertTrue(LevelTest.test(units, lv.gate, 99).none { it is Learn })
            val pool = LevelTest.gamePool(units, lv.gate, 5)
            assertEquals(lv.gate.gameQuestions, pool.size)
            SpeedRound(pool) // must be valid speed round content
        }
    }

    // ---- Gating ----
    @Test fun `lessons unlock in order and the level gate flows lessons test game`() {
        val units = content.unitsOf(kid)
        val lessons = units.flatMap { it.lessons }
        var p = Progress()
        assertTrue(Gating.lessonUnlocked(kid, units, lessons[0].id, p))
        assertFalse(Gating.lessonUnlocked(kid, units, lessons[1].id, p))
        p = p.withLesson(lessons[0].id, 59)
        assertFalse(Gating.lessonUnlocked(kid, units, lessons[1].id, p))   // below pass mark
        p = p.withLesson(lessons[0].id, 60)
        assertTrue(Gating.lessonUnlocked(kid, units, lessons[1].id, p))
        assertFalse(Gating.testUnlocked(kid, units, p))
        lessons.forEach { p = p.withLesson(it.id, 90) }
        assertTrue(Gating.testUnlocked(kid, units, p))
        assertFalse(Gating.gameUnlocked(kid, p))
        p = Gating.recordTest(kid, p, 79)
        assertFalse(Gating.gameUnlocked(kid, p))
        p = Gating.recordTest(kid, p, 85)
        assertTrue(Gating.gameUnlocked(kid, p))
        p = Gating.recordGame(kid, p, 60)
        assertFalse(kid.id in p.levelsPassed)
        p = Gating.recordGame(kid, p, 80)
        assertTrue(kid.id in p.levelsPassed)
    }

    @Test fun `best scores never go down`() {
        var p = Progress().withLesson("x", 90).withLesson("x", 50)
        assertEquals(90, p.lessons["x"]!!.bestPct)
        p = Gating.recordTest(kid, p, 90); p = Gating.recordTest(kid, p, 10)
        assertEquals(90, p.levelTests[kid.id])
    }

    @Test fun `converging levels need any one prerequisite`() {
        val shared = Level("l4", 4, Track.SHARED, "t", "t", requiresAny = listOf("kid-3", "adult-3"), unitFiles = emptyList(), gate = LevelGate(game = GameType.SPEED_ROUND))
        assertFalse(Gating.levelUnlocked(shared, Progress()))
        assertTrue(Gating.levelUnlocked(shared, Progress(levelsPassed = setOf("adult-3"))))
        assertTrue(Gating.levelUnlocked(shared, Progress(levelsPassed = setOf("kid-3"))))
    }

    @Test fun `test out skips lessons only with a passing score`() {
        val units = content.unitsOf(adult)
        val low = Gating.testOut(adult, units, Progress(), 70)
        assertTrue(low.lessons.isEmpty())
        val ok = Gating.testOut(adult, units, Progress(), 90)
        assertTrue(Gating.allLessonsPassed(units, ok))
        assertEquals(90, ok.levelTests[adult.id])
        assertFalse(adult.id in ok.levelsPassed)  // the game still has to be played
    }

    // ---- Review ----
    @Test fun `review boxes climb and reset`() {
        var p = Review.miss(Progress(), "s", "down", today = 100)
        assertTrue(Review.due(p, 100).isEmpty()); assertEquals(1, Review.due(p, 101).size)
        p = Review.answered(p, "s", true, 101); assertEquals(2, p.review["s"]!!.box); assertEquals(103, p.review["s"]!!.dueDay)
        p = Review.answered(p, "s", false, 103); assertEquals(1, p.review["s"]!!.box)
        repeat(4) { p = Review.answered(p, "s", true, 200) }
        assertEquals(5, p.review["s"]!!.box)
        p = Review.answered(p, "s", true, 300)
        assertTrue(p.review.isEmpty())
    }

    // ---- Streak ----
    @Test fun `streak counts consecutive days`() {
        var p = Progress().touch(10)
        assertEquals(1, p.streakDays)
        p = p.touch(10); assertEquals(1, p.streakDays)
        p = p.touch(11); assertEquals(2, p.streakDays)
        p = p.touch(13); assertEquals(1, p.streakDays)
    }

    // ---- Placement ----
    @Test fun `placement starts at first missed level`() {
        val levels = (1..4).map { Level("l$it", it, Track.SHARED, "t", "t", unitFiles = emptyList(), gate = LevelGate(game = GameType.SPEED_ROUND)) }
        assertEquals(3, Placement.startingLevel(levels, mapOf(1 to 100, 2 to 85, 3 to 60, 4 to 100))?.number)
        assertEquals(2, Placement.startingLevel(levels, mapOf(1 to 100))?.number)   // unscored level starts there
        assertNull(Placement.startingLevel(levels, mapOf(1 to 90, 2 to 90, 3 to 90, 4 to 90)))
        val p = Placement.apply(levels, levels[2], Progress())
        assertEquals(setOf("l1", "l2"), p.levelsPassed)
    }

    private fun perfectAnswer(s: Step): Answer = when (s) {
        is Learn -> Answer.Seen
        is MultipleChoice -> Answer.Choice(s.answer)
        is ReadDefense -> Answer.Choice(s.answer)
        is TrueFalse -> Answer.Bool(s.answer)
        is Match -> Answer.Matches(s.pairs.associate { it.left to it.right })
        is Order -> Answer.Ordering(s.items)
        is DiagramTap -> Answer.Taps(s.targets)
        is Situation -> Answer.Choice(s.options.indexOfFirst { it.quality == Quality.BEST })
    }
}
