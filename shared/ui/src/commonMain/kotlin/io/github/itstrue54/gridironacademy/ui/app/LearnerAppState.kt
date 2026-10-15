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
import io.github.itstrue54.gridironacademy.model.Lesson
import io.github.itstrue54.gridironacademy.model.Level
import io.github.itstrue54.gridironacademy.model.Step
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.progress.Gating
import io.github.itstrue54.gridironacademy.progress.LearnerSave
import io.github.itstrue54.gridironacademy.progress.Profile
import io.github.itstrue54.gridironacademy.progress.Review
import io.github.itstrue54.gridironacademy.ui.theme.Skin

/** Where the learner is. The back stack is a plain list so it is easy to test and restore. */
sealed interface Screen {
    data object Welcome : Screen
    data object Onboarding : Screen
    data object Home : Screen
    data class LevelDetail(val levelId: String) : Screen
    data class LessonPlay(val levelId: String, val lessonId: String) : Screen
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
 */
@Stable
class LearnerAppState(
    val content: Content,
    private val store: LearnerStore,
    private val today: () -> Long,
) {
    var save: LearnerSave by mutableStateOf(store.load() ?: LearnerSave())
        private set

    var backStack: List<Screen> by mutableStateOf(listOf(if (save.profile == null) Screen.Welcome else Screen.Home))
        private set

    val screen: Screen get() = backStack.last()

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

    fun lessonsPassed(level: Level): Int = content.unitsOf(level).flatMap { it.lessons }.count { save.progress.lessonPassed(it.id) }

    fun lessonCount(level: Level): Int = content.unitsOf(level).sumOf { it.lessons.size }

    fun lesson(levelId: String, lessonId: String): Lesson =
        content.unitsOf(content.level(levelId)).flatMap { it.lessons }.first { it.id == lessonId }

    // ---- Navigation ----

    fun go(screen: Screen) { backStack = backStack + screen }

    /** Returns false at the root, so the platform can close the app. */
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack = backStack.dropLast(1)
        return true
    }

    fun startOnboarding() { backStack = listOf(Screen.Onboarding) }

    fun createProfile(nickname: String, band: AgeBand) {
        commit(save.copy(profile = Profile(nickname.trim().ifEmpty { DEFAULT_NICKNAME }, band)))
        backStack = listOf(Screen.Home)
    }

    fun openLevel(level: Level) = go(Screen.LevelDetail(level.id))

    fun startLesson(level: Level, lesson: Lesson) = go(Screen.LessonPlay(level.id, lesson.id))

    /** Records a finished lesson and replaces the player with its result screen. */
    fun finishLesson(levelId: String, lessonId: String, session: LessonSession) {
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

    private fun commit(next: LearnerSave) {
        save = next
        store.save(next)
    }

    private companion object {
        const val DEFAULT_NICKNAME = "Player"
    }
}

/**
 * One lesson in progress: the engine's [LessonSession] made observable. The session is
 * the source of truth; this mirrors what the screen reads.
 */
@Stable
class LessonController(steps: List<Step>) {
    private val session = LessonSession(steps)
    private val seen = mutableSetOf<String>()

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
    /** True when the current step was missed once already: its feedback is gentler. */
    var isRetry: Boolean by mutableStateOf(false)
        private set

    val finished: Boolean get() = step == null
    val result: LessonSession get() = session

    fun submit(a: Answer) {
        val s = step ?: return
        grade = session.submit(a)
        answer = a
        total = done + session.remaining
        if (!Grader.isGraded(s)) next()
    }

    fun next() {
        step?.let { seen += it.id }
        session.next()
        done += 1
        grade = null
        answer = null
        step = session.current
        isRetry = step?.id in seen
        total = done + session.remaining
    }
}
