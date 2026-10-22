package io.github.itstrue54.gridironacademy.ui

import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.engine.LessonSession
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.progress.AttemptEvent
import io.github.itstrue54.gridironacademy.progress.Profile
import io.github.itstrue54.gridironacademy.progress.LearnerSave
import io.github.itstrue54.gridironacademy.progress.AttemptContext
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.LessonController
import io.github.itstrue54.gridironacademy.ui.app.NextUp
import io.github.itstrue54.gridironacademy.ui.app.Screen
import io.github.itstrue54.gridironacademy.ui.app.RestoreResult
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import io.github.itstrue54.gridironacademy.ui.games.rightToPass
import kotlin.test.*

class LearnerAppStateTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    /** The ladder is exactly this track's levels (plus shared ones), in order, from Level 1. */
    private fun assertTrackLadder(s: LearnerAppState, track: Track) {
        val expected = content.curriculum.levels.filter { it.track == track || it.track == Track.SHARED }.sortedBy { it.number }
        assertEquals(expected.map { it.id }, s.levels.map { it.id })
        assertEquals(1, s.levels.first().number)
        assertTrue(s.levels.zipWithNext().all { (a, b) -> a.number < b.number })
    }

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
        assertTrackLadder(s, Track.KID)
        assertFalse(s.back(), "Home is the root")
    }

    @Test
    fun aReturningLearnerStartsAtHomeOnTheirTrack() {
        val store = InMemoryLearnerStore()
        LearnerAppState(content, store) { 1 }.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val s = LearnerAppState(content, store) { 2 }
        assertEquals(Screen.Home, s.screen)
        assertEquals(Skin.Adult, s.skin)
        assertTrackLadder(s, Track.ADULT)
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
    fun everyGradedAnswerIsLoggedWithItsContextTimeAndRetries() {
        var clock = 1_000_000L
        var ids = 0
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store, nowMillis = { clock }, newId = { "id-${ids++}" }) { 300 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        var missed: String? = null
        while (!c.finished) {
            val step = c.step!!
            clock += 4_000 // four seconds on every step
            val answer = if (missed == null && step is MultipleChoice) {
                missed = step.id
                Answer.Choice((step.answer + 1) % step.choices.size)
            } else perfect(step)
            c.submit(answer)
            if (!c.finished && c.grade != null) c.next()
        }
        s.finishLesson(level.id, lesson.id, c.result)

        val log = store.load()!!.attempts
        val graded = lesson.steps.count { it !is Learn }
        assertEquals(graded + 1, log.size, "every graded step once, plus the retry")
        assertTrue(log.all { it.context == AttemptContext.LESSON && it.epochDay == 300L && it.msSpent == 4_000 })
        assertEquals((0L until log.size).toList(), log.map { it.seq })
        assertEquals(listOf("id-0"), log.map { it.deviceId }.distinct())
        assertEquals(listOf("id-1"), log.map { it.learnerId }.distinct())
        val tries = log.filter { it.stepId == missed }
        assertEquals(listOf(true to 0f, false to 1f), tries.map { it.firstTry to it.pct })
        assertNotNull(tries.first().answer, "the wrong choice is kept for the coach's view")
        assertEquals(log.size.toLong(), store.load()!!.nextSeq)
    }

    @Test
    fun answersCountEvenWhenTheLearnerQuitsPartWay() {
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { 310 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        var answered = 0
        while (answered < 2) {
            val step = c.step!!
            c.submit(perfect(step))
            if (step !is Learn) answered++
            c.next()
        }
        assertEquals(2, s.unsavedAttempts)
        s.back()
        assertEquals(2, store.load()!!.attempts.size)
        assertFalse(store.load()!!.progress.lessonPassed(lesson.id), "the lesson itself doesn't count")
        assertEquals(0, s.unsavedAttempts)
    }

    @Test
    fun gatesLogTheirAnswersAsCheckAndGame() {
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { 320 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        val level = s.levels.first()
        passAllDrills(s, level)
        s.startCheckpoint(level)
        val test = s.checkpointController(level.id, testOut = false)
        playPerfect(test)
        s.finishCheckpoint(level.id, false, test.result)
        s.back()
        s.startBigGame(level)
        val round = s.speedRoundController(level.id)
        var t = 0L
        while (!round.over) { t += 1_000; round.tick(t); round.current?.let { round.answer(perfect(it)) } }
        s.finishSpeedRound(level.id, round)
        val byContext = store.load()!!.attempts.groupingBy { it.context }.eachCount()
        assertEquals(level.gate.testQuestions, byContext[AttemptContext.CHECK])
        assertEquals(round.answered, byContext[AttemptContext.GAME])
        val seqs = store.load()!!.attempts.map { it.seq }
        assertEquals(seqs.indices.map { it.toLong() }, seqs, "one unbroken sequence per device")
    }

    @Test
    fun aMissComesBackAsTomorrowsPracticeAndMovesUpABox() {
        var day = 500L
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { day }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        var missed: String? = null
        while (!c.finished) {
            val step = c.step!!
            val answer = if (missed == null && step is MultipleChoice) {
                missed = step.id; Answer.Choice((step.answer + 1) % step.choices.size)
            } else perfect(step)
            c.submit(answer)
            if (!c.finished && c.grade != null) c.next()
        }
        s.finishLesson(level.id, lesson.id, c.result)
        s.back()
        assertFalse(s.practice().urgent, "not due the same day")

        day = 501
        val practice = s.practice()
        assertTrue(practice.urgent)
        assertEquals(missed, practice.steps.first().id)
        assertFalse(s.practiceDoneToday)
        s.startPractice()
        val p = s.practiceControllerOrNull()!!
        playPerfect(p)
        s.finishPractice(p.result)
        val done = s.screen as Screen.PracticeDone
        assertEquals(100, done.scorePct)
        assertTrue(done.xpGained > 0)
        assertEquals(2, s.save.progress.review.getValue(missed!!).box, "a right review moves the card up")
        assertEquals(503L, s.save.progress.review.getValue(missed!!).dueDay)
        assertEquals(2, s.save.progress.streakDays, "practice counts for the streak")
        assertTrue(s.practiceDoneToday)
        assertTrue(store.load()!!.attempts.any { it.context == AttemptContext.REVIEW && it.stepId == missed })
        s.back()
        assertEquals(Screen.Home, s.screen)
    }

    @Test
    fun theGlossaryQuizzesOnlyReachableTermsAndQueuesMisses() {
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { 700 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        s.openGlossary()
        assertEquals(Screen.Glossary, s.screen)
        // Level 4's personnel isn't open to a new adult: no Quiz me, but it says where it's taught.
        assertEquals(emptyList(), s.quizFor("personnel"))
        assertEquals(4, s.learnedIn("personnel")!!.first.number)
        // A Level 1 term is.
        val concept = content.unitsOf(s.levels.first()).flatMap { it.lessons }.flatMap { it.steps }.first { it !is Learn && it.concept != null }.concept!!
        val quiz = s.quizFor(concept)
        assertTrue(quiz.isNotEmpty() && quiz.size <= 5)
        s.startQuiz(concept)
        val c = s.quizControllerOrNull(concept)!!
        var first = true
        while (!c.finished) {
            val step = c.step!!
            val a = if (first && step is MultipleChoice) { first = false; Answer.Choice((step.answer + 1) % step.choices.size) } else perfect(step)
            c.submit(a); if (!c.finished && c.grade != null) c.next()
        }
        s.finishQuiz(concept, c.result)
        val done = s.screen as Screen.QuizDone
        assertEquals(concept, done.concept)
        if (!first) assertTrue(s.save.progress.review.keys.any { k -> quiz.any { it.id == k } }, "a quiz miss becomes a review card")
        assertEquals(quiz.size, store.load()!!.attempts.size)
        s.back()
        assertEquals(Screen.Glossary, s.screen)
    }

    @Test
    fun threeWrongGateAnswersLockItForAMinuteAndLeavingTheAppClosesTheZone() {
        var clock = 1_000_000L
        val s = LearnerAppState(content, InMemoryLearnerStore(), nowMillis = { clock }) { 800 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        s.openGrownUpGate()
        repeat(2) { s.grownUpGateMissed() }
        assertEquals(0, s.grownUpGateLockedMillis())
        s.grownUpGateMissed()
        assertEquals(60_000, s.grownUpGateLockedMillis())
        clock += 59_000
        assertEquals(1_000, s.grownUpGateLockedMillis())
        clock += 1_000
        assertEquals(0, s.grownUpGateLockedMillis())

        s.enterGrownUpZone()
        assertEquals(Screen.GrownUpZone, s.screen)
        s.leftTheApp()
        assertEquals(Screen.Home, s.screen, "back at Home; the zone needs the gate again")
        s.leftTheApp()
        assertEquals(Screen.Home, s.screen, "leaving from anywhere else changes nothing")
    }

    @Test
    fun theAttemptLogKeepsOnlyTheNewestAnswers() {
        val old = (0 until LearnerSave.MAX_ATTEMPTS).map {
            AttemptEvent("l", "d", it.toLong(), "k1-field-03", AttemptContext.LESSON, pct = 1f, epochDay = 1, at = 1)
        }
        val store = InMemoryLearnerStore(LearnerSave(profile = Profile("Sam", AgeBand.AGE_18_PLUS), attempts = old, nextSeq = old.size.toLong()))
        val s = LearnerAppState(content, store) { 800 }
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        // Step past any learn cards to the first graded step, and answer it.
        while (!io.github.itstrue54.gridironacademy.engine.Grader.isGraded(c.step!!)) c.next()
        val answered = c.step!!.id
        c.submit(perfect(c.step!!))
        s.back()
        val kept = store.load()!!.attempts
        assertEquals(LearnerSave.MAX_ATTEMPTS, kept.size)
        assertEquals(1L, kept.first().seq, "the oldest went")
        assertEquals(answered, kept.last().stepId, "the newest is kept")
    }

    @Test
    fun theScreensComeBackAfterAndroidEndsTheAppButALessonComesBackAsItsLevel() {
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store) { 800 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val level = s.levels.first()
        s.openLevel(level)
        s.startLesson(level, content.unitsOf(level).first().lessons.first())
        assertEquals(listOf("home", "level:${level.id}"), s.restorableRoute())

        val again = LearnerAppState(content, store) { 800 }
        again.restoreRoute(s.restorableRoute() + listOf("map", "level:architect-8"))
        assertEquals(listOf(Screen.Home, Screen.LevelDetail(level.id), Screen.PlaybookMap), again.backStack,
            "a locked level is dropped")
        val fresh = LearnerAppState(content, InMemoryLearnerStore()) { 800 }
        fresh.restoreRoute(listOf("home", "glossary"))
        assertEquals(listOf(Screen.Welcome), fresh.backStack, "no profile, nothing to restore")
    }

    @Test
    fun aScreensOwnBackIsAskedFirst() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 800 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        s.openGlossary(); s.openPlaybookMap()
        var cardOpen = true
        s.backOverride = { if (cardOpen) { cardOpen = false; true } else false }
        s.requestBack()
        assertEquals(Screen.PlaybookMap, s.screen, "the first Back closes the card")
        s.requestBack()
        assertEquals(Screen.Glossary, s.screen)
    }

    @Test
    fun theGrownUpZoneRenamesChangesAgeAndBacksUp() {
        var ids = 0
        val store = InMemoryLearnerStore()
        val s = LearnerAppState(content, store, newId = { "id-${ids++}" }) { 800 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        s.openGrownUpGate()
        s.enterGrownUpZone()
        assertEquals(listOf(Screen.Home, Screen.GrownUpZone), s.backStack, "the zone replaces the gate")

        s.renameProfile("  Leo J  ")
        assertEquals("Leo J", s.profile!!.nickname)
        s.renameProfile("   ")
        assertEquals("Leo J", s.profile!!.nickname, "a blank name is ignored")
        s.changeAgeBand(AgeBand.AGE_13_17)
        assertEquals(Track.ADULT, s.profile!!.ageBand.track)
        assertTrue(s.levels.none { it.track == Track.KID })

        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        playPerfect(c)
        s.finishLesson(level.id, lesson.id, c.result)
        val backup = s.backupText()
        val oldDevice = s.save.deviceId

        // Restore onto a fresh install: everything comes back under a new device id.
        val other = LearnerAppState(content, InMemoryLearnerStore(), newId = { "new-${ids++}" }) { 801 }
        other.createProfile("Someone", AgeBand.AGE_18_PLUS)
        assertEquals(RestoreResult.Restored, other.restoreBackup(backup))
        assertEquals("Leo J", other.profile!!.nickname)
        assertTrue(other.save.progress.lessonPassed(lesson.id))
        assertEquals(s.save.attempts, other.save.attempts)
        assertNotEquals(oldDevice, other.save.deviceId)
        assertEquals(0, other.save.nextSeq)
        assertEquals(s.save.learnerId, other.save.learnerId, "the same learner")

        val before = other.save
        assertEquals(RestoreResult.TooNew, other.restoreBackup(backup.replace("\"schemaVersion\":2", "\"schemaVersion\":9")))
        assertEquals(RestoreResult.NotABackup, other.restoreBackup("hello"))
        assertEquals(RestoreResult.NotABackup, other.restoreBackup("{}"), "no profile, no restore")
        assertEquals(before, other.save, "a refused file changes nothing")

        val report = s.progressReport()
        assertEquals(s.levels.size, report.size)
        assertEquals(1, report.first().drillsPassed)
    }

    @Test
    fun leavingAPlayerAsksOnlyOnceSomethingWasAnswered() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 900 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val level = s.levels.first()
        // Nothing answered yet: Back just leaves.
        s.openLevel(level)
        s.startCheckpoint(level, testOut = true)
        s.checkpointController(level.id, testOut = true)
        assertTrue(s.requestBack())
        assertFalse(s.confirmingLeave)
        assertEquals(Screen.LevelDetail(level.id), s.screen)

        // One answer in: Back asks.
        s.startCheckpoint(level, testOut = true)
        val c = s.checkpointController(level.id, testOut = true)
        c.submit(perfect(c.step!!))
        assertTrue(s.requestBack())
        assertTrue(s.confirmingLeave)
        assertEquals(Screen.CheckpointPlay(level.id, true), s.screen, "still in the test")
        s.keepGoing()
        assertFalse(s.confirmingLeave)
        assertEquals(Screen.CheckpointPlay(level.id, true), s.screen)
        s.requestBack(); s.confirmLeave()
        assertEquals(Screen.LevelDetail(level.id), s.screen)
        assertFalse(s.confirmingLeave)
    }

    @Test
    fun aLearnCardIsNotAnAnswer() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 910 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        assertTrue(c.step is Learn, "lessons open on a learn card")
        c.submit(Answer.Seen)
        s.requestBack()
        assertFalse(s.confirmingLeave, "reading a card isn't progress worth asking about")
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
        // Passing Level 1 opens Level 2, and Continue goes straight to its first drill.
        val next = s.nextUp() as NextUp.Drill
        assertEquals("kid-2", next.level.id)
        assertTrue(s.levelUnlocked(next.level))
        assertEquals(content.unitsOf(next.level).first().lessons.first().id, next.lesson.id)
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
    fun theSpeedRoundStartsUntimedForEightToNineAndForScreenReaders() {
        fun default(band: AgeBand, reader: Boolean = false) =
            LearnerAppState(content, InMemoryLearnerStore(), screenReaderOn = { reader }) { 700 }
                .apply { createProfile("Sam", band) }.speedRoundUntimedByDefault()
        assertTrue(default(AgeBand.AGE_8_9))
        assertFalse(default(AgeBand.AGE_10_12))
        assertFalse(default(AgeBand.AGE_18_PLUS))
        assertTrue(default(AgeBand.AGE_18_PLUS, reader = true))
    }

    @Test
    fun anUntimedSpeedRoundKeepsScoringAndOnlyEndsWhenEveryQuestionIsAnswered() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 700 }
        s.createProfile("Leo", AgeBand.AGE_8_9)
        val level = s.levels.first()
        s.startBigGame(level)
        val round = s.speedRoundController(level.id)
        assertFalse(round.started)
        round.start(timed = false)
        round.tick(10 * 60_000)
        assertFalse(round.over, "no clock")
        repeat(3) { round.answer(perfect(round.current!!)) }
        assertEquals(10 + 10 + 20, round.points, "the streak still doubles")
        while (!round.over) round.answer(perfect(round.current!!))
        assertEquals(100, round.scorePct)
    }

    @Test
    fun theLeaveDialogClosesWhenTheScreenChangesUnderIt() {
        val s = LearnerAppState(content, InMemoryLearnerStore()) { 700 }
        s.createProfile("Sam", AgeBand.AGE_18_PLUS)
        val level = s.levels.first()
        s.startBigGame(level)
        val round = s.speedRoundController(level.id)
        round.start(timed = true)
        round.answer(perfect(round.current!!))
        s.requestBack()
        assertTrue(s.confirmingLeave)
        round.tick(60_000)
        s.finishSpeedRound(level.id, round)
        assertTrue(s.screen is Screen.GateDone)
        assertFalse(s.confirmingLeave, "a result screen never opens under a stale Leave? dialog")
    }

    @Test
    fun theRulesScreenSaysHowManyRightAnswersWin() {
        assertEquals(7, rightToPass(10, 70))
        assertEquals(9, rightToPass(12, 70))
        assertEquals(0, rightToPass(0, 70))
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
