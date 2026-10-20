package io.github.itstrue54.gridironacademy.ui.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.engine.Grade
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.engine.Grader
import io.github.itstrue54.gridironacademy.engine.LessonSession
import io.github.itstrue54.gridironacademy.engine.LevelTest
import io.github.itstrue54.gridironacademy.model.GameType
import io.github.itstrue54.gridironacademy.progress.Progress
import io.github.itstrue54.gridironacademy.model.Lesson
import io.github.itstrue54.gridironacademy.model.Level
import io.github.itstrue54.gridironacademy.model.Step
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.data.migration.SaveMigrations
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.progress.AttemptContext
import io.github.itstrue54.gridironacademy.progress.AttemptEvent
import io.github.itstrue54.gridironacademy.progress.Attempts
import io.github.itstrue54.gridironacademy.progress.DailyPractice
import io.github.itstrue54.gridironacademy.progress.GlossaryQuiz
import io.github.itstrue54.gridironacademy.progress.LevelReport
import io.github.itstrue54.gridironacademy.progress.ConceptMastery
import io.github.itstrue54.gridironacademy.progress.Mastery
import io.github.itstrue54.gridironacademy.progress.ProgressReport
import io.github.itstrue54.gridironacademy.progress.TrackChange
import io.github.itstrue54.gridironacademy.progress.TrackSwitch
import io.github.itstrue54.gridironacademy.data.migration.SaveTooNew
import io.github.itstrue54.gridironacademy.progress.Practice
import io.github.itstrue54.gridironacademy.progress.Gating
import io.github.itstrue54.gridironacademy.progress.LearnerSave
import io.github.itstrue54.gridironacademy.progress.Profile
import io.github.itstrue54.gridironacademy.progress.Review
import io.github.itstrue54.gridironacademy.ui.theme.Skin

enum class RestoreResult { Restored, TooNew, NotABackup }

/**
 * What only the platform can do for the learner app: files. Android uses the system's
 * save and open pickers, so a backup goes where the parent chooses, never to a cloud by
 * itself (SPEC 5.4).
 */
interface LearnerPlatform {
    /** Ask where to save [text] as [fileName]; report whether it was written. */
    fun exportBackup(text: String, fileName: String, done: (Boolean) -> Unit)
    /** Ask for a backup file; hand back its text, or null if cancelled or unreadable. */
    fun pickBackup(done: (String?) -> Unit)
    val appVersion: String
}

/** For tests and previews: no files. */
object NoPlatform : LearnerPlatform {
    override fun exportBackup(text: String, fileName: String, done: (Boolean) -> Unit) = done(false)
    override fun pickBackup(done: (String?) -> Unit) = done(null)
    override val appVersion: String = "dev"
}

/** Where the learner is. The back stack is a plain list so it is easy to test and restore. */
sealed interface Screen {
    data object Welcome : Screen
    data object Onboarding : Screen
    data object Home : Screen
    data class LevelDetail(val levelId: String) : Screen
    data class LessonPlay(val levelId: String, val lessonId: String) : Screen
    /** The level test (DESIGN 9.2). [testOut]: an adult skipping the level's drills. */
    data class CheckpointPlay(val levelId: String, val testOut: Boolean) : Screen
    /** The level game (Speed round for Level 1). */
    data class BigGamePlay(val levelId: String) : Screen
    /** Today's practice (SPEC 6.4). */
    data object PracticePlay : Screen
    data class PracticeDone(val scorePct: Int, val count: Int, val xpGained: Int) : Screen
    /** The grown-up zone (DESIGN 9.9) and the gate in front of it. */
    data object GrownUpGate : Screen
    data object GrownUpZone : Screen
    /** The glossary (DESIGN 9.10) and its "Quiz me" mini-drill. */
    data object Glossary : Screen
    /** The concept graph as a skill tree (DESIGN 9.14). */
    data object PlaybookMap : Screen
    data class GlossaryQuizPlay(val concept: String) : Screen
    data class QuizDone(val concept: String, val scorePct: Int, val count: Int) : Screen
    /** The result of a Checkpoint or Big Game, and the level-complete moment. */
    data class GateDone(
        val levelId: String,
        val gate: Gate,
        val scorePct: Int,
        val passPct: Int,
        val passed: Boolean,
        /** True only when this result is the one that passed the level (DESIGN 9.7). */
        val levelPassed: Boolean,
        val testOut: Boolean = false,
        /** Speed round extras; zero for other games. */
        val points: Int = 0,
        val bestStreak: Int = 0,
        /** What to look at again: the questions missed, with their answers. */
        val review: List<ReviewItem> = emptyList(),
    ) : Screen
    data class LessonDone(
        val levelId: String,
        val lessonId: String,
        val scorePct: Int,
        val stars: Int,
        val xpGained: Int,
        val passed: Boolean,
        val missedCount: Int,
    ) : Screen
}

enum class Gate { CHECKPOINT, BIG_GAME }

/**
 * A missed question shown after a game: what was asked, the answer, and why. A true/false
 * answer is kept as [answerTrue] so the screen words it in the learner's language.
 */
data class ReviewItem(val question: String, val why: String, val answerText: String? = null, val answerTrue: Boolean? = null)

/** What Home's Continue opens next (DESIGN 9.1). */
sealed interface NextUp {
    val level: Level
    data class Drill(override val level: Level, val lesson: Lesson) : NextUp
    data class Checkpoint(override val level: Level) : NextUp
    data class BigGame(override val level: Level) : NextUp
}

/**
 * Persists the one local save. The app shell implements it on disk; tests keep it in
 * memory. Writes must not block the UI thread.
 */
interface LearnerStore {
    fun load(): LearnerSave?
    fun save(save: LearnerSave)
}

class InMemoryLearnerStore(private var saved: LearnerSave? = null) : LearnerStore {
    override fun load(): LearnerSave? = saved
    override fun save(save: LearnerSave) { saved = save }
}

/**
 * The learner app's state: the save and the back stack. Every rule (gating, scoring,
 * review) is a call into `core`; this class only wires them to screens.
 *
 * @param today the current epoch day, from the platform clock. Passed in so tests can
 *   pick the day and `core` stays clock-free.
 * @param nowMillis epoch milliseconds, for attempt times and time spent.
 * @param newId a fresh unique id (the device's, the profile's), from the platform.
 */
@Stable
class LearnerAppState(
    val content: Content,
    private val store: LearnerStore,
    nowMillis: (() -> Long)? = null,
    private val newId: () -> String = { "local" },
    // Last, so `LearnerAppState(content, store) { day }` keeps reading as "today is day".
    private val today: () -> Long,
) {
    /** Epoch millis; tests that don't care get the start of [today]. */
    private val now: () -> Long = nowMillis ?: { today() * MILLIS_PER_DAY }

    var save: LearnerSave by mutableStateOf(SaveMigrations.withIds(store.load() ?: LearnerSave(), newId))
        private set

    /**
     * Answers graded since the last save. They go to disk with the lesson or gate result,
     * or when the learner leaves part-way: an answer counts even if the lesson doesn't.
     */
    private val pending = mutableListOf<AttemptEvent>()

    private fun recorder(context: AttemptContext) = { step: Step, grade: Grade, answer: Answer, firstTry: Boolean, ms: Long ->
        if (Grader.isGraded(step)) {
            pending += AttemptEvent(
                learnerId = save.learnerId,
                deviceId = save.deviceId,
                seq = save.nextSeq + pending.size,
                stepId = step.id,
                context = context,
                pct = grade.credit,
                firstTry = firstTry,
                answer = Attempts.answerText(step, answer),
                msSpent = ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                epochDay = today(),
                at = now(),
            )
        }
    }

    /** Attempts waiting to be saved; for tests. */
    val unsavedAttempts: Int get() = pending.size

    var backStack: List<Screen> by mutableStateOf(listOf(if (save.profile == null) Screen.Welcome else Screen.Home))
        private set

    val screen: Screen get() = backStack.last()

    /**
     * The lesson in progress lives here, not in composition, so it survives the screen
     * being rebuilt (a phone rotation) as long as this state does.
     */
    private var activeLesson: Pair<String, LessonController>? = null

    // A finished lesson's controller is kept until the next lesson starts: the outgoing
    // screen is still drawn during the cross-fade and must not restart the lesson.
    /**
     * The controller for a lesson. It is created only while that lesson is the screen on
     * top: an outgoing screen still drawn during the cross-fade gets the existing one, or
     * null after a quit, and never starts a new attempt behind the learner's back.
     */
    fun lessonControllerOrNull(levelId: String, lessonId: String): LessonController? =
        activeLesson?.takeIf { it.first == lessonId }?.second
            ?: if (screen == Screen.LessonPlay(levelId, lessonId)) {
                LessonController(lesson(levelId, lessonId).steps, now = now, onGraded = recorder(AttemptContext.LESSON))
                    .also { activeLesson = lessonId to it }
            } else null

    fun lessonController(levelId: String, lessonId: String): LessonController =
        checkNotNull(lessonControllerOrNull(levelId, lessonId)) { "lesson $lessonId is not on screen" }

    /** The Checkpoint or Big Game in progress, keyed by its screen, like [activeLesson]. */
    private var activeGate: Pair<Screen, Any>? = null

    /** Gate attempts this session, so every retake draws a new set of questions. */
    private var gateAttempts = 0

    /**
     * A seed per level, day and attempt. The attempt count makes a same-day retake ask
     * different questions (it can't be learned from the last try's feedback); the level
     * and day keep a single attempt reproducible.
     */
    private fun gateSeed(level: Level): Long =
        (level.id.hashCode().toLong() * SEED_MIX + today()) * SEED_MIX + gateAttempts

    private inline fun <reified T : Any> gate(key: Screen, make: () -> T): T? {
        (activeGate?.takeIf { it.first == key }?.second as? T)?.let { return it }
        if (screen != key) return null
        return make().also { activeGate = key to it }
    }

    fun checkpointControllerOrNull(levelId: String, testOut: Boolean): LessonController? =
        gate(Screen.CheckpointPlay(levelId, testOut)) {
            val level = content.level(levelId)
            LessonController(LevelTest.test(content.unitsOf(level), level.gate, gateSeed(level)), retryMisses = false,
                now = now, onGraded = recorder(AttemptContext.CHECK))
        }

    fun checkpointController(levelId: String, testOut: Boolean): LessonController =
        checkNotNull(checkpointControllerOrNull(levelId, testOut)) { "checkpoint $levelId is not on screen" }

    /** Speed round levels get the timed game; other games play through the lesson player. */
    fun bigGameIsSpeedRound(levelId: String): Boolean = content.level(levelId).gate.game == GameType.SPEED_ROUND

    fun speedRoundControllerOrNull(levelId: String): SpeedRoundController? =
        gate(Screen.BigGamePlay(levelId)) {
            val level = content.level(levelId)
            SpeedRoundController(LevelTest.gamePool(content.unitsOf(level), level.gate, gateSeed(level)), SPEED_ROUND_MILLIS,
                onGraded = recorder(AttemptContext.GAME))
        }

    fun speedRoundController(levelId: String): SpeedRoundController =
        checkNotNull(speedRoundControllerOrNull(levelId)) { "big game $levelId is not on screen" }

    fun gameControllerOrNull(levelId: String): LessonController? =
        gate(Screen.BigGamePlay(levelId)) {
            val level = content.level(levelId)
            LessonController(LevelTest.gamePool(content.unitsOf(level), level.gate, gateSeed(level)), retryMisses = false,
                now = now, onGraded = recorder(AttemptContext.GAME))
        }

    /** The practice in progress, created once so its questions don't change mid-way. */
    private var activePractice: Pair<List<Step>, LessonController>? = null

    /** The level the learner is working on: the next thing to do, or the highest open level. */
    val currentLevel: Level? get() = nextUp()?.level ?: levels.lastOrNull { levelUnlocked(it) }

    /** Today's practice: the same set all day for this profile (SPEC 6.4). */
    fun practice(): Practice =
        DailyPractice.pick(save.progress, save.attempts, content, currentLevel, today(), save.learnerId.hashCode().toLong())

    val practiceDoneToday: Boolean get() = DailyPractice.doneToday(save.attempts, today())

    fun startPractice() {
        activePractice = null
        go(Screen.PracticePlay)
    }

    fun practiceControllerOrNull(): LessonController? =
        activePractice?.second ?: if (screen == Screen.PracticePlay) {
            val steps = practice().steps
            if (steps.isEmpty()) null
            else LessonController(steps, retryMisses = false, now = now, onGraded = recorder(AttemptContext.REVIEW))
                .also { activePractice = steps to it }
        } else null

    /**
     * Records practice: a review card answered right moves up a box, one missed goes back
     * to box 1, and a practice miss that wasn't a card becomes one. Counts for the streak.
     */
    fun finishPractice(session: LessonSession) {
        if (screen != Screen.PracticePlay) return
        val steps = activePractice?.first ?: return
        val before = save.progress
        val reviewed = steps.fold(before) { p, s ->
            val right = s.id !in session.missedIds
            when {
                s.id in p.review -> Review.answered(p, s.id, right, today())
                !right -> Review.miss(p, s.id, s.concept, today())
                else -> p
            }
        }
        val after = reviewed.copy(xp = reviewed.xp + session.scorePct / XP_DIVISOR).touch(today())
        commit(save.copy(progress = after))
        replaceTop(Screen.PracticeDone(session.scorePct, steps.size, after.xp - before.xp))
    }

    /** The quiz in progress: its term, its questions and its controller, made once. */
    private var activeQuiz: Triple<String, List<Step>, LessonController>? = null

    fun openGlossary() = go(Screen.Glossary)

    fun openPlaybookMap() = go(Screen.PlaybookMap)

    /** Each answered concept's mastery (SPEC 4.6); a concept missing from it is New. */
    fun conceptMastery(): Map<String, ConceptMastery> = Mastery.forLearner(save.attempts, content::conceptOfStep, today())

    /** Where a term is first taught on this profile's ladder ("Where you learned it"). */
    fun learnedIn(concept: String): Pair<Level, Lesson>? = GlossaryQuiz.firstLesson(content, concept, levels)

    /** The quiz for a term, only from levels this learner can open. Empty means no Quiz me. */
    fun quizFor(concept: String): List<Step> =
        GlossaryQuiz.quiz(content, concept, levels.filter { levelUnlocked(it) }, concept.hashCode().toLong() * SEED_MIX + today())

    fun startQuiz(concept: String) {
        activeQuiz = null
        go(Screen.GlossaryQuizPlay(concept))
    }

    fun quizControllerOrNull(concept: String): LessonController? =
        activeQuiz?.takeIf { it.first == concept }?.third ?: if (screen == Screen.GlossaryQuizPlay(concept)) {
            val steps = quizFor(concept)
            if (steps.isEmpty()) null
            else LessonController(steps, retryMisses = false, now = now, onGraded = recorder(AttemptContext.LESSON))
                .also { activeQuiz = Triple(concept, steps, it) }
        } else null

    /** A quiz feeds review like a lesson: misses become cards. It counts for the streak. */
    fun finishQuiz(concept: String, session: LessonSession) {
        if (screen != Screen.GlossaryQuizPlay(concept)) return
        val steps = activeQuiz?.takeIf { it.first == concept }?.second ?: return
        val missed = steps.filter { it.id in session.missedIds }
        val after = missed.fold(save.progress) { p, s -> Review.miss(p, s.id, s.concept, today()) }.touch(today())
        commit(save.copy(progress = after))
        replaceTop(Screen.QuizDone(concept, session.scorePct, steps.size))
    }

    fun openGrownUpGate() = go(Screen.GrownUpGate)

    /** The platform clock, for seeding the gate's question (UI only, never the engine). */
    val clockMillis: Long get() = now()

    /** The gate was passed: the zone takes its place, so Back from the zone goes Home. */
    fun enterGrownUpZone() {
        if (screen == Screen.GrownUpGate) replaceTop(Screen.GrownUpZone)
    }

    fun renameProfile(nickname: String) {
        val p = profile ?: return
        commit(save.copy(profile = p.copy(nickname = nickname.trim().take(NICKNAME_MAX).ifEmpty { p.nickname })))
    }

    /** Behind the gate (SPEC 5.2). Progress stays; the band changes the track and skin. */
    fun changeAgeBand(band: AgeBand) {
        val p = profile ?: return
        commit(save.copy(profile = p.copy(ageBand = band)))
    }

    /** What changing to [band] would do to the track, or null if the track stays the same. */
    fun trackChangeFor(band: AgeBand): TrackChange? {
        val from = profile?.ageBand?.track ?: return null
        if (band.track == from) return null
        return TrackSwitch.preview(content.curriculum.levels, save.progress, from, band.track)
    }

    fun progressReport(): List<LevelReport> =
        ProgressReport.levels(content, levels, save.progress, save.attempts, today())

    fun minutesThisWeek(): Int = ProgressReport.minutes(save.attempts, today())

    /** The whole save as a backup file (SPEC 9), with anything not yet saved. */
    fun backupText(): String {
        if (pending.isNotEmpty()) commit(save)
        return SaveMigrations.encode(save)
    }

    /**
     * Restores a backup over this profile. A file from a newer app or not a save at all
     * is refused with the reason, and nothing changes. The restored log continues under a
     * new device id, so (device, seq) stays unique if the old device is still in use.
     */
    fun restoreBackup(text: String): RestoreResult {
        val restored = try {
            SaveMigrations.decode(text, newId)
        } catch (e: SaveTooNew) {
            return RestoreResult.TooNew
        } catch (e: Exception) {
            return RestoreResult.NotABackup
        }
        if (restored.profile == null) return RestoreResult.NotABackup
        pending.clear()
        activeLesson = null; activeGate = null; activePractice = null; activeQuiz = null
        commit(restored.copy(deviceId = newId(), nextSeq = 0))
        return RestoreResult.Restored
    }

    val profile: Profile? get() = save.profile

    /** Level 0 isn't written yet, so pre-readers get the Kid skin for now (SPEC 3.1). */
    val skin: Skin get() = if (profile?.ageBand?.track == Track.ADULT) Skin.Adult else Skin.Kid

    /** This profile's ladder: its own track's levels plus the shared ones, in order. */
    val levels: List<Level>
        get() {
            val track = profile?.ageBand?.track ?: return emptyList()
            return content.curriculum.levels.filter { it.track == track || it.track == Track.SHARED }.sortedBy { it.number }
        }

    fun levelUnlocked(level: Level): Boolean = Gating.levelUnlocked(level, save.progress)

    fun lessonUnlocked(level: Level, lessonId: String): Boolean =
        Gating.lessonUnlocked(level, content.unitsOf(level), lessonId, save.progress)

    /** What Home's Continue opens: the first open, unpassed lesson on the ladder. */
    fun nextLesson(): Pair<Level, Lesson>? =
        levels.firstNotNullOfOrNull { level -> Gating.nextLesson(level, content.unitsOf(level), save.progress)?.let { level to it } }

    fun checkpointOpen(level: Level): Boolean = Gating.testUnlocked(level, content.unitsOf(level), save.progress)

    fun bigGameOpen(level: Level): Boolean = Gating.gameUnlocked(level, save.progress)

    /** Adults may take the Checkpoint before the drills to skip them (DESIGN 9.2, SPEC 4.5). */
    fun canTestOut(level: Level): Boolean =
        profile?.ageBand?.track == Track.ADULT && levelUnlocked(level) && !checkpointOpen(level) &&
            level.id !in save.progress.levelsPassed

    fun nextUp(): NextUp? {
        for (level in levels) {
            if (!levelUnlocked(level) || level.id in save.progress.levelsPassed) continue
            Gating.nextLesson(level, content.unitsOf(level), save.progress)?.let { return NextUp.Drill(level, it) }
            if (!bigGameOpen(level)) return if (checkpointOpen(level)) NextUp.Checkpoint(level) else null
            return NextUp.BigGame(level)
        }
        return null
    }

    fun lessonsPassed(level: Level): Int = content.unitsOf(level).flatMap { it.lessons }.count { save.progress.lessonPassed(it.id) }

    fun lessonCount(level: Level): Int = content.unitsOf(level).sumOf { it.lessons.size }

    fun lesson(levelId: String, lessonId: String): Lesson =
        content.unitsOf(content.level(levelId)).flatMap { it.lessons }.first { it.id == lessonId }

    // ---- Navigation ----

    fun go(screen: Screen) { backStack = backStack + screen }

    /** Set while "Leave? This try won't count" is on screen. */
    var confirmingLeave: Boolean by mutableStateOf(false)
        private set

    /** Answers given on the player on screen, so leaving can ask first once there are any. */
    private fun answeredHere(): Int = when (val s = screen) {
        is Screen.LessonPlay -> activeLesson?.takeIf { it.first == s.lessonId }?.second?.graded ?: 0
        is Screen.CheckpointPlay, is Screen.BigGamePlay -> when (val g = activeGate?.second) {
            is LessonController -> g.graded
            is SpeedRoundController -> g.answered
            else -> 0
        }
        Screen.PracticePlay -> activePractice?.second?.graded ?: 0
        is Screen.GlossaryQuizPlay -> activeQuiz?.third?.graded ?: 0
        else -> 0
    }

    /**
     * ✕ or the system Back. On a player with answers given it asks first (a misclick
     * mid-Checkpoint shouldn't throw the try away); anywhere else it's [back].
     */
    fun requestBack(): Boolean {
        if (answeredHere() > 0) { confirmingLeave = true; return true }
        return back()
    }

    fun confirmLeave() { confirmingLeave = false; back() }

    fun keepGoing() { confirmingLeave = false }

    /** Returns false at the root, so the platform can close the app. */
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        // Quitting drops the lesson's result, but the answers given still count.
        if (pending.isNotEmpty()) commit(save)
        if (screen is Screen.LessonPlay) activeLesson = null
        if (screen is Screen.CheckpointPlay || screen is Screen.BigGamePlay) activeGate = null
        backStack = backStack.dropLast(1)
        return true
    }

    fun startOnboarding() { backStack = listOf(Screen.Onboarding) }

    fun createProfile(nickname: String, band: AgeBand) {
        commit(save.copy(profile = Profile(nickname.trim().ifEmpty { DEFAULT_NICKNAME }, band)))
        backStack = listOf(Screen.Home)
    }

    fun openLevel(level: Level) = go(Screen.LevelDetail(level.id))

    fun startLesson(level: Level, lesson: Lesson) {
        activeLesson = null
        go(Screen.LessonPlay(level.id, lesson.id))
    }

    fun startCheckpoint(level: Level, testOut: Boolean = false) {
        activeGate = null
        gateAttempts++
        go(Screen.CheckpointPlay(level.id, testOut))
    }

    fun startBigGame(level: Level) {
        activeGate = null
        gateAttempts++
        go(Screen.BigGamePlay(level.id))
    }

    /** Records a Checkpoint; a test-out that passes also marks the level's drills passed. */
    fun finishCheckpoint(levelId: String, testOut: Boolean, session: LessonSession) {
        if (screen != Screen.CheckpointPlay(levelId, testOut)) return
        val level = content.level(levelId)
        val units = content.unitsOf(level)
        val steps = session.missedIds
        val missed = units.flatMap { u -> u.lessons.flatMap { it.steps } }.filter { it.id in steps }.associate { it.id to it.concept }
        val before = save.progress
        val recorded = if (testOut) Gating.testOut(level, units, before, session.scorePct) else Gating.recordTest(level, before, session.scorePct)
        val after = missed.entries.fold(recorded) { acc, (id, concept) -> Review.miss(acc, id, concept, today()) }.touch(today())
        commit(save.copy(progress = after))
        replaceTop(gateDone(level, Gate.CHECKPOINT, session.scorePct, level.gate.testPassPct, before, after).copy(testOut = testOut))
    }

    /** Records a Big Game played through the lesson player (non-speed-round games). */
    fun finishBigGame(levelId: String, session: LessonSession) {
        if (screen != Screen.BigGamePlay(levelId)) return
        recordGame(content.level(levelId), session.scorePct, review = emptyList())
    }

    fun finishSpeedRound(levelId: String, round: SpeedRoundController) {
        if (screen != Screen.BigGamePlay(levelId)) return
        recordGame(content.level(levelId), round.scorePct, round.review, round.points, round.bestStreak)
    }

    private fun recordGame(level: Level, pct: Int, review: List<ReviewItem>, points: Int = 0, bestStreak: Int = 0) {
        val before = save.progress
        val after = Gating.recordGame(level, before, pct).touch(today())
        commit(save.copy(progress = after))
        replaceTop(gateDone(level, Gate.BIG_GAME, pct, level.gate.gamePassPct, before, after).copy(points = points, bestStreak = bestStreak, review = review))
    }

    private fun gateDone(level: Level, gate: Gate, pct: Int, passPct: Int, before: Progress, after: Progress) = Screen.GateDone(
        levelId = level.id,
        gate = gate,
        scorePct = pct,
        passPct = passPct,
        passed = pct >= passPct,
        levelPassed = level.id in after.levelsPassed && level.id !in before.levelsPassed,
    )

    private fun replaceTop(s: Screen) { backStack = backStack.dropLast(1) + s }

    /** Records a finished lesson and replaces the player with its result screen. */
    fun finishLesson(levelId: String, lessonId: String, session: LessonSession) {
        // Only the lesson on screen can finish, and only once: a recomposed screen must
        // not award the lesson twice or pop the result screen.
        if (screen != Screen.LessonPlay(levelId, lessonId)) return
        val steps = lesson(levelId, lessonId).steps.associateBy { it.id }
        val missed = session.missedIds.associateWith { steps[it]?.concept }
        val before = save.progress
        val after = Review.afterLesson(before, lessonId, session.scorePct, missed, today())
        commit(save.copy(progress = after))
        val done = Screen.LessonDone(
            levelId = levelId,
            lessonId = lessonId,
            scorePct = session.scorePct,
            stars = LessonSession.stars(session.scorePct),
            xpGained = after.xp - before.xp,
            passed = session.scorePct >= LessonSession.LESSON_PASS_PCT,
            missedCount = missed.size,
        )
        backStack = backStack.dropLast(1) + done
    }

    /** Saves [next] with any answers graded since the last save. */
    private fun commit(next: LearnerSave) {
        val withLog = if (pending.isEmpty()) next else next.copy(
            attempts = next.attempts + pending,
            nextSeq = next.nextSeq + pending.size,
        )
        pending.clear()
        save = withLog
        store.save(withLog)
    }

    private companion object {
        const val DEFAULT_NICKNAME = "Player"
        const val SEED_MIX = 1_000_003L
        /** DESIGN 9.6: a one-minute round. The Untimed option arrives with profile settings. */
        const val SPEED_ROUND_MILLIS = 60_000L
        const val MILLIS_PER_DAY = 86_400_000L
        const val NICKNAME_MAX = 20
        /** Practice XP, the same rate as a lesson: a tenth of the score. */
        const val XP_DIVISOR = 10
    }
}

/**
 * One lesson in progress: the engine's [LessonSession] made observable. The session is
 * the source of truth; this mirrors what the screen reads.
 */
@Stable
class LessonController(
    steps: List<Step>,
    retryMisses: Boolean = true,
    private val now: () -> Long = { 0L },
    /** Told about every submitted answer: the step, its grade, the answer, first try or retry, time on the step. */
    private val onGraded: ((Step, Grade, Answer, Boolean, Long) -> Unit)? = null,
) {
    private val session = LessonSession(steps, retryMisses)
    private val seen = mutableSetOf<String>()
    private var shownAt = now()

    var step: Step? by mutableStateOf(session.current)
        private set
    var grade: Grade? by mutableStateOf(null)
        private set
    var answer: Answer? by mutableStateOf(null)
        private set
    /** Steps finished, for the DriveBar. */
    var done: Int by mutableStateOf(0)
        private set
    /** Steps in the drive, growing when a miss is queued for a retry. */
    var total: Int by mutableStateOf(steps.size)
        private set
    /**
     * What the learner has picked but not checked, kept here (not in the screen) so a
     * rotation doesn't clear it. [matchDraft] holds match pairs before all are paired.
     */
    var draft: Answer? by mutableStateOf(null)
    var matchDraft: Map<String, String> by mutableStateOf(emptyMap())

    /** Graded answers submitted so far (learn cards don't count). */
    var graded: Int by mutableStateOf(0)
        private set

    /** True when the current step was missed once already: its feedback is gentler. */
    var isRetry: Boolean by mutableStateOf(false)
        private set

    val finished: Boolean get() = step == null
    val result: LessonSession get() = session

    fun submit(a: Answer) {
        val s = step ?: return
        val g = session.submit(a)
        if (Grader.isGraded(s)) graded += 1
        grade = g
        answer = a
        onGraded?.invoke(s, g, a, s.id !in seen, now() - shownAt)
        total = done + session.remaining
        if (!Grader.isGraded(s)) next()
    }

    fun next() {
        step?.let { seen += it.id }
        session.next()
        done += 1
        grade = null
        answer = null
        draft = null
        matchDraft = emptyMap()
        step = session.current
        isRetry = step?.id in seen
        total = done + session.remaining
        shownAt = now()
    }
}
