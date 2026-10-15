package io.github.itstrue54.gridironacademy.ui

import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.engine.LessonSession
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.LessonController
import io.github.itstrue54.gridironacademy.ui.app.NextUp
import io.github.itstrue54.gridironacademy.ui.app.Screen
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import kotlin.test.*

class LearnerAppStateTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    /** The right answer for any graded step, so a test can play a perfect lesson. */
    private fun perfect(step: Step): Answer = when (step) {
        is Learn -> Answer.Seen
        is MultipleChoice -> Answer.Choice(step.answer)
        is ReadDefense -> Answer.Choice(step.answer)
        is TrueFalse -> Answer.Bool(step.answer)
        is Match -> Answer.Matches(step.pairs.associate { it.left to it.right })
        is Order -> Answer.Ordering(step.items)
        is DiagramTap -> Answer.Taps(step.targets)
        is Situation -> Answer.Choice(step.options.indexOfFirst { it.quality == Quality.BEST })
    }

    @Test
    fun firstRunGoesThroughOnboardingToHomeAndSaves() {
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { 100 }
        assertEquals(Screen.Welcome, s.screen)
        s.startOnboarding()
        assertEquals(Screen.Onboarding, s.screen)
        s.createProfile("  Leo ", AgeBand.AGE_8_9)
        assertEquals(listOf(Screen.Home), s.backStack)
        assertEquals("Leo", store.load()?.profile?.nickname)
        assertEquals(Skin.Kid, s.skin)
        assertEquals(listOf("kid-1"), s.levels.map { it.id })
        assertFalse(s.back(), "Home is the root")
    }

    @Test
    fun aReturningLearnerStartsAtHomeOnTheirTrack() {
        val store = InMemoryLearnerStore()
        LearnerAppState(content, store) { 1 }.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val s = LearnerAppState(content, store) { 2 }
        assertEquals(Screen.Home, s.screen)
        assertEquals(Skin.Adult, s.skin)
        assertEquals(listOf("adult-1"), s.levels.map { it.id })
    }

    @Test
    fun blankNicknameBecomesPlayer() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 1 }
        s.createProfile("   ", AgeBand.AGE_13_17)
        assertEquals("Player", s.profile?.nickname)
    }

    @Test
    fun aPerfectLessonIsRecordedAndOpensTheNextOne() {
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { 200 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        val (level, first) = assertNotNull(s.nextLesson())
        s.openLevel(level)
        s.startLesson(level, first)

        val c = LessonController(first.steps)
        while (!c.finished) {
            c.submit(perfect(c.step!!))
            if (!c.finished && c.grade != null) c.next()
        }
        s.finishLesson(level.id, first.id, c.result)

        val done = s.screen as Screen.LessonDone
        assertEquals(100, done.scorePct)
        assertEquals(3, done.stars)
        assertTrue(done.passed)
        assertTrue(done.xpGained > 0)
        assertEquals(1, store.load()!!.progress.streakDays)
        assertNotEquals(first.id, s.nextLesson()?.second?.id)
        assertTrue(s.back())
        assertEquals(Screen.LevelDetail(level.id), s.screen, "the result replaces the player, so back goes to the level")
    }

    @Test
    fun missesAreRetriedOnceAndQueuedForReview() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 300 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = LessonController(lesson.steps)
        var missed: String? = null
        var retries = 0
        while (!c.finished) {
            val step = c.step!!
            if (c.isRetry) retries++
            val answer = if (missed == null && step is MultipleChoice) {
                missed = step.id
                Answer.Choice((step.answer + 1) % step.choices.size)
            } else perfect(step)
            c.submit(answer)
            if (!c.finished && c.grade != null) c.next()
        }
        assertEquals(1, retries)
        s.finishLesson(level.id, lesson.id, c.result)
        assertTrue(missed!! in s.save.progress.review, "first-try miss goes to review")
        assertTrue((s.screen as Screen.LessonDone).scorePct < 100)
    }

    @Test
    fun learnStepsAdvanceWithoutAGrade() {
        val steps = content.unitsOf(content.level("kid-1")).first().lessons.first().steps
        val c = LessonController(steps)
        assertTrue(c.step is Learn)
        c.submit(Answer.Seen)
        assertNull(c.grade)
        assertEquals(1, c.done)
        assertEquals(steps[1].id, c.step?.id)
    }

    @Test
    fun scoreMatchesTheEngine() {
        val steps = content.unitsOf(content.level("adult-1")).first().lessons.first().steps
        val c = LessonController(steps)
        while (!c.finished) { c.submit(perfect(c.step!!)); if (!c.finished && c.grade != null) c.next() }
        assertEquals(100, c.result.scorePct)
        assertEquals(3, LessonSession.stars(c.result.scorePct))
    }

    @Test
    fun aLessonFinishesOnlyOnceAndKeepsItsControllerForTheExitFade() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 400 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        val (level, lesson) = s.nextLesson()!!
        s.openLevel(level)
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        while (!c.finished) { c.submit(perfect(c.step!!)); if (!c.finished && c.grade != null) c.next() }
        s.finishLesson(level.id, lesson.id, c.result)
        val xp = s.save.progress.xp
        val stack = s.backStack
        s.finishLesson(level.id, lesson.id, c.result) // a recomposed screen firing again
        assertEquals(xp, s.save.progress.xp)
        assertEquals(stack, s.backStack)
        assertSame(c, s.lessonController(level.id, lesson.id), "the fading-out screen sees the finished lesson")
        s.back()
        s.startLesson(level, lesson)
        assertNotSame(c, s.lessonController(level.id, lesson.id), "starting again is a fresh attempt")
    }

    @Test
    fun theDraftAnswerLivesOnTheControllerAndClearsOnNext() {
        val steps = content.unitsOf(content.level("adult-1")).flatMap { it.lessons }.flatMap { it.steps }
        val mc = steps.filterIsInstance<MultipleChoice>().first()
        val c = LessonController(listOf(mc, mc.copy(id = "again")))
        c.draft = Answer.Choice(0)
        c.matchDraft = mapOf("a" to "b")
        c.submit(Answer.Choice(mc.answer))
        c.next()
        assertNull(c.draft)
        assertTrue(c.matchDraft.isEmpty())
    }

    @Test
    fun theWelcomeSceneComesFromContent() {
        assertTrue(content.scenes.welcome.diagram.players.isNotEmpty())
        assertTrue(content.scenes.welcome.description.isNotBlank())
    }

    private fun playPerfect(c: LessonController) {
        while (!c.finished) { c.submit(perfect(c.step!!)); if (!c.finished && c.grade != null) c.next() }
    }

    private fun passAllDrills(s: LearnerAppState, level: Level) {
        content.unitsOf(level).flatMap { it.lessons }.forEach { lesson ->
            s.startLesson(level, lesson)
            val c = s.lessonController(level.id, lesson.id)
            playPerfect(c)
            s.finishLesson(level.id, lesson.id, c.result)
            s.back()
        }
    }

    @Test
    fun aKidPassesLevelOneThroughDrillsCheckpointAndSpeedRound() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 600 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        val level = s.levels.first()
        s.openLevel(level)
        assertFalse(s.checkpointOpen(level))
        assertFalse(s.canTestOut(level), "only adults test out")
        passAllDrills(s, level)
        assertEquals(NextUp.Checkpoint(level), s.nextUp())

        s.startCheckpoint(level)
        val test = s.checkpointController(level.id, testOut = false)
        assertEquals(level.gate.testQuestions, test.total)
        playPerfect(test)
        s.finishCheckpoint(level.id, false, test.result)
        val cp = s.screen as Screen.GateDone
        assertTrue(cp.passed && !cp.levelPassed)
        assertEquals(NextUp.BigGame(level), s.nextUp())

        s.back()
        s.startBigGame(level)
        assertTrue(s.bigGameIsSpeedRound(level.id))
        val round = s.speedRoundController(level.id)
        var t = 0L
        while (!round.over) {
            t += 1_000
            round.tick(t)
            round.current?.let { round.answer(perfect(it)) }
        }
        s.finishSpeedRound(level.id, round)
        val game = s.screen as Screen.GateDone
        assertEquals(100, game.scorePct)
        assertTrue(game.passed && game.levelPassed)
        assertTrue(level.id in s.save.progress.levelsPassed)
        assertNull(s.nextUp(), "nothing left on this track until Level 2 is written")
    }

    @Test
    fun theSpeedRoundClockEndsTheGameAndCountsWhatWasLeft() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 700 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val level = s.levels.first()
        s.startBigGame(level)
        val round = s.speedRoundController(level.id)
        round.current?.let { round.answer(perfect(it)) }
        round.tick(61_000)
        assertTrue(round.over)
        assertEquals(100 / level.gate.gameQuestions, round.scorePct)
        val before = round.points
        round.current?.let { round.answer(perfect(it)) }
        assertEquals(before, round.points, "no answers after the whistle")
    }

    @Test
    fun anAdultCanTestOutAndTheDrillsCountAsPassed() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 800 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val level = s.levels.first()
        s.openLevel(level)
        assertTrue(s.canTestOut(level))
        s.startCheckpoint(level, testOut = true)
        val test = s.checkpointController(level.id, testOut = true)
        playPerfect(test)
        s.finishCheckpoint(level.id, true, test.result)
        val done = s.screen as Screen.GateDone
        assertTrue(done.passed && done.testOut)
        assertEquals(s.lessonCount(level), s.lessonsPassed(level))
        assertTrue(s.bigGameOpen(level))
    }

    @Test
    fun aFailedCheckpointQueuesMissesAndKeepsTheGameLocked() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 900 }
        s.createProfile("Leo", AgeBand.AGE_10_12)
        val level = s.levels.first()
        passAllDrills(s, level)
        s.startCheckpoint(level)
        val test = s.checkpointController(level.id, testOut = false)
        while (!test.finished) { test.submit(Answer.Choice(-1)); if (!test.finished) test.next() }
        assertEquals(level.gate.testQuestions, test.done, "a test asks each question once")
        s.finishCheckpoint(level.id, false, test.result)
        assertFalse((s.screen as Screen.GateDone).passed)
        assertFalse(s.bigGameOpen(level))
        assertTrue(s.save.progress.review.isNotEmpty())
    }

    @Test
    fun aRetakeDrawsDifferentQuestions() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 1_000 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val level = s.levels.first()
        s.openLevel(level)
        fun drawn(): List<String> {
            s.startCheckpoint(level, testOut = true)
            val c = s.checkpointController(level.id, true)
            val ids = mutableListOf<String>()
            while (!c.finished) { ids += c.step!!.id; c.submit(Answer.Choice(-1)); c.next() }
            s.back()
            return ids
        }
        val first = drawn()
        val second = drawn()
        assertEquals(level.gate.testQuestions, first.size)
        assertNotEquals(first, second, "a retake is a new draw, not the same 12 again")
    }

    @Test
    fun aScreenThatIsNotOnTopNeverStartsANewAttempt() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 1_100 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        val level = s.levels.first()
        val lesson = s.nextLesson()!!.second
        s.openLevel(level)
        assertNull(s.lessonControllerOrNull(level.id, lesson.id))
        assertNull(s.speedRoundControllerOrNull(level.id))
        assertNull(s.checkpointControllerOrNull(level.id, false))
        s.startLesson(level, lesson)
        assertNotNull(s.lessonControllerOrNull(level.id, lesson.id))
        s.back() // quit: the fading-out lesson screen asks again
        assertNull(s.lessonControllerOrNull(level.id, lesson.id))
    }
}
