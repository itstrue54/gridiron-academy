package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.model.GameType
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Lesson
import io.github.itstrue54.gridironacademy.model.SchemePack
import io.github.itstrue54.gridironacademy.model.Track
import kotlinx.serialization.Serializable

/** Every assignment threshold (SPEC 7.4, 11.5 rule 6). */
object AssignmentRules {
    const val DEFAULT_PASS_PCT = 80
    const val NOTE_MAX_CHARS = 200
    /** Warn when a lesson is more than this many levels above the learner. */
    const val LEVELS_ABOVE_WARNING = 2
    /** Plan length estimates, shown as "about 35 min". */
    const val SECONDS_PER_STEP = 20
    const val MINUTES_PER_GAME = 3
    /** Inside this many days of the due date with under half the plan done: Behind. */
    const val BEHIND_DAYS_BEFORE_DUE = 2
    /** Warm-up lessons for missing basics in one plan, nearest prerequisites first. */
    const val MAX_WARMUP_LESSONS = 4
    /** Share of the progress ring the lessons fill; passing the check fills the rest. */
    const val LESSONS_SHARE = 0.9f
}

/** Who an assignment went to and how long their plan was when it was sent. */
@Serializable
data class AssignmentTarget(val learnerId: String, val lessonsAtSend: Int)

/**
 * A sent assignment (SPEC 7.4). Immutable once sent: an edit is a new [version].
 * Ids and times come from the caller, never from the engine.
 */
@Serializable
data class Assignment(
    val id: String,
    val version: Int = 1,
    val teamId: String? = null,
    /** Shown as "From Coach Ray". */
    val fromName: String,
    val packIds: List<String>,
    val targets: List<AssignmentTarget>,
    val dueDay: Long? = null,
    val passPct: Int = AssignmentRules.DEFAULT_PASS_PCT,
    val note: String = "",
    val sentDay: Long,
    /** Add warm-up lessons for prerequisites a learner hasn't mastered (SPEC 7.4, default on). */
    val includeWarmups: Boolean = true,
)

enum class NoteProblem { TOO_LONG, LINK, CONTACT }

/**
 * Assignment notes reach children, so they carry no links or contact details (SPEC 5.5).
 * The server checks again; this gives the coach the reason before Send.
 */
object NoteRules {
    private val link = Regex("""(?i)\b(https?://|www\.)|\b[a-z0-9-]+\.(com|net|org|io|co|app|gg|me|ly)\b""")
    private val email = Regex("""(?i)\b[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}\b""")
    private val phone = Regex("""(?:\+?\d[\s().-]*){7,}""")

    fun problems(note: String): List<NoteProblem> = buildList {
        if (note.length > AssignmentRules.NOTE_MAX_CHARS) add(NoteProblem.TOO_LONG)
        if (link.containsMatchIn(note)) add(NoteProblem.LINK)
        if (email.containsMatchIn(note) || phone.containsMatchIn(note)) add(NoteProblem.CONTACT)
    }
}

enum class StopKind { WARMUP, LESSON, GAME, CHECK }

/** One stop on a learner's plan, as the inbox's DriveBar shows it (DESIGN 9.12). */
data class PlanStop(
    val kind: StopKind,
    /** The lesson for [StopKind.LESSON] and [StopKind.WARMUP]. */
    val lessonId: String? = null,
    /** The game for [StopKind.GAME]. */
    val game: GameType? = null,
    /** Graded and learn steps for a lesson, questions for the check. */
    val steps: Int = 0,
    /** True when the lesson is more than [AssignmentRules.LEVELS_ABOVE_WARNING] levels up. */
    val wellAboveLevel: Boolean = false,
)

data class AssignmentPlan(
    val stops: List<PlanStop>,
    /** Warm-up lessons left out to keep the plan short ("and N more basics"). */
    val warmupsLeftOut: Int = 0,
) {
    /** Lessons to play, warm-ups included: this is what shrinks as concepts are mastered. */
    val lessons: Int get() = stops.count { it.kind == StopKind.LESSON || it.kind == StopKind.WARMUP }
    val warmups: Int get() = stops.count { it.kind == StopKind.WARMUP }
    val wellAboveLevel: Boolean get() = stops.any { it.wellAboveLevel }

    /** A rough length for the preview: "about 35 min". */
    val minutes: Int get() {
        val stepSeconds = stops.filter { it.kind != StopKind.GAME }.sumOf { it.steps } * AssignmentRules.SECONDS_PER_STEP
        val games = stops.count { it.kind == StopKind.GAME } * AssignmentRules.MINUTES_PER_GAME
        return (stepSeconds + 59) / 60 + games
    }
}

/**
 * The per-learner plan for an assignment (SPEC 7.4): warm-ups for missing basics, then
 * the packs' lessons in curriculum order minus any whose concepts the learner has all
 * mastered, then the packs' games, then the check. A learner who already knows the
 * material gets a shorter plan; one missing the basics gets them first.
 */
object AssignmentPlanner {

    fun plan(
        packs: List<SchemePack>,
        content: Content,
        mastery: Map<String, ConceptMastery>,
        learnerLevel: Int,
        track: Track? = null,
        includeWarmups: Boolean = true,
    ): AssignmentPlan {
        val wanted = packs.flatMap { it.lessons }.toSet()
        val (warmups, leftOut) = if (includeWarmups) warmups(packs, content, mastery, learnerLevel, track, wanted) else emptyList<Lesson>() to 0
        val lessons = content.lessons.filter { it.id in wanted }.filter { l ->
            content.conceptsOf(l).any { mastery[it]?.secure != true }
        }
        val lessonStops = lessons.map { l ->
            val level = content.levelOfLesson(l.id)?.number ?: learnerLevel
            PlanStop(
                kind = StopKind.LESSON,
                lessonId = l.id,
                steps = l.steps.size,
                wellAboveLevel = level - learnerLevel > AssignmentRules.LEVELS_ABOVE_WARNING,
            )
        }
        val warmupStops = warmups.map { l -> PlanStop(StopKind.WARMUP, lessonId = l.id, steps = l.steps.size) }
        val games = packs.flatMap { it.games }.distinct().map { PlanStop(StopKind.GAME, game = it) }
        val check = PlanStop(StopKind.CHECK, steps = packs.maxOfOrNull { it.check.questions } ?: 0)
        return AssignmentPlan(warmupStops + lessonStops + games + check, leftOut)
    }

    /**
     * Lessons for the prerequisites of the pack concepts still to learn, where the learner
     * hasn't mastered them and isn't past the level that teaches them: for each missing concept, nearest first, the lesson that first teaches it on the learner's own track.
     * At most [AssignmentRules.MAX_WARMUP_LESSONS], played in curriculum order.
     */
    private fun warmups(
        packs: List<SchemePack>,
        content: Content,
        mastery: Map<String, ConceptMastery>,
        learnerLevel: Int,
        track: Track?,
        packLessons: Set<String>,
    ): Pair<List<Lesson>, Int> {
        // Only what's still to learn needs its basics: a pack already mastered needs no warm-ups.
        val packConcepts = packs.flatMap { content.conceptsOf(it) }.distinct().filter { mastery[it]?.secure != true }
        val ladder = content.curriculum.levels
            .filter { track == null || it.track == track || it.track == Track.SHARED }
            .sortedBy { it.number }
        // Known: mastered, or taught on a level the learner is already past. "Mastered" needs
        // right answers on two days, so a Level 6 player who passed Level 2 before the answer
        // log existed (or just hasn't been quizzed twice) still knows what a gap is.
        val missing = content.graph.prerequisites(packConcepts).filter { c ->
            mastery[c]?.secure != true && (content.firstTaughtLevel(c) ?: 0) >= learnerLevel
        }
        val lessons = missing.mapNotNull { c -> GlossaryQuiz.firstLesson(content, c, ladder)?.second }
            .filter { it.id !in packLessons }
            .distinctBy { it.id }
        val kept = lessons.take(AssignmentRules.MAX_WARMUP_LESSONS).map { it.id }.toSet()
        return content.lessons.filter { it.id in kept } to (lessons.size - kept.size)
    }

    /** The graded steps an assignment check draws from. */
    fun checkPool(packs: List<SchemePack>, content: Content) =
        packs.flatMap { it.lessons }.distinct().mapNotNull { content.lesson(it) }
            .flatMap { l -> l.steps.filter { it !is Learn } }
}

enum class AssignmentStatus { ON_TRACK, BEHIND, DONE }

object AssignmentProgress {

    /** Passed the check at the assignment's bar. */
    fun done(a: Assignment, learnerId: String, events: List<AssignmentEvent>): Boolean =
        events.any {
            it.assignmentId == a.id && it.learnerId == learnerId &&
                it.type == AssignmentEventType.CHECK_RESULT && it.pct >= a.passPct
        }

    /**
     * On track, Behind or Done (DESIGN 14.8: never "Failing"). [planNow] is the learner's
     * plan today; it shrinks as concepts are mastered, which is how progress shows.
     */
    fun status(
        a: Assignment,
        learnerId: String,
        planNow: AssignmentPlan,
        events: List<AssignmentEvent>,
        today: Long,
    ): AssignmentStatus {
        if (done(a, learnerId, events)) return AssignmentStatus.DONE
        val due = a.dueDay ?: return AssignmentStatus.ON_TRACK
        if (today > due) return AssignmentStatus.BEHIND
        val atSend = a.targets.firstOrNull { it.learnerId == learnerId }?.lessonsAtSend ?: return AssignmentStatus.ON_TRACK
        val halfDone = atSend == 0 || planNow.lessons * 2 <= atSend
        return if (due - today <= AssignmentRules.BEHIND_DAYS_BEFORE_DUE && !halfDone) AssignmentStatus.BEHIND
        else AssignmentStatus.ON_TRACK
    }

    /** 0-1 for the progress ring: lessons cleared since sending, the check counting as the rest. */
    fun fraction(a: Assignment, learnerId: String, planNow: AssignmentPlan, events: List<AssignmentEvent>): Float {
        if (done(a, learnerId, events)) return 1f
        val atSend = a.targets.firstOrNull { it.learnerId == learnerId }?.lessonsAtSend ?: return 0f
        if (atSend == 0) return AssignmentRules.LESSONS_SHARE
        return (atSend - planNow.lessons).coerceAtLeast(0) * AssignmentRules.LESSONS_SHARE / atSend
    }
}
