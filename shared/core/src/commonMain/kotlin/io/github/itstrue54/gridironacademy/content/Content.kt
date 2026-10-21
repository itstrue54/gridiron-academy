package io.github.itstrue54.gridironacademy.content

import io.github.itstrue54.gridironacademy.model.*
import kotlinx.serialization.json.Json

/** Where content text comes from. The app backs this with assets; tests with the classpath. */
fun interface ContentSource {
    /** Returns the file's text, or null if it does not exist. */
    fun read(path: String): String?
}

class ContentException(message: String) : Exception(message)

class Content(
    val curriculum: Curriculum,
    val glossary: Glossary,
    /** level id -> its units, in order. */
    val unitsByLevel: Map<String, List<CourseUnit>>,
    val scenes: AppScenes,
    /** Scheme packs for assignments (SPEC 7.4). */
    val packs: List<SchemePack> = emptyList(),
    /** Football sign-offs (SPEC 13.3). */
    val review: ReviewLog = ReviewLog(),
) {
    private val signed: Set<String> by lazy { review.signoffs.map { it.step }.toSet() }

    /** True once a coach has signed this step off (DESIGN 8: no ReviewBadge). */
    fun signedOff(stepId: String): Boolean = stepId in signed

    fun level(id: String): Level = curriculum.levels.first { it.id == id }
    fun unitsOf(level: Level): List<CourseUnit> = unitsByLevel.getValue(level.id)
    fun levelOfLesson(lessonId: String): Level? = levelByLesson[lessonId]

    /** Every lesson in curriculum order (levels as listed, then units, then lessons). */
    val lessons: List<Lesson> by lazy {
        curriculum.levels.flatMap { lv -> unitsOf(lv).flatMap { it.lessons } }
    }
    private val lessonById by lazy { lessons.associateBy { it.id } }
    private val levelByLesson by lazy {
        curriculum.levels.flatMap { lv -> unitsOf(lv).flatMap { u -> u.lessons.map { it.id to lv } } }.toMap()
    }
    private val conceptByStep by lazy {
        lessons.flatMap { l -> l.steps.mapNotNull { s -> s.concept?.let { s.id to it } } }.toMap()
    }

    fun lesson(id: String): Lesson? = lessonById[id]

    val graph: ConceptGraph by lazy { ConceptGraph(glossary.entries) }

    /** The lowest level number of any lesson that teaches [concept]; null if none does. */
    fun firstTaughtLevel(concept: String): Int? = firstLevelByConcept[concept]
    private val firstLevelByConcept by lazy {
        val m = mutableMapOf<String, Int>()
        curriculum.levels.forEach { lv ->
            unitsOf(lv).forEach { u -> u.lessons.forEach { l -> conceptsOf(l).forEach { c -> m[c] = minOf(m[c] ?: lv.number, lv.number) } } }
        }
        m
    }
    fun pack(id: String): SchemePack? = packs.firstOrNull { it.id == id }

    /** The concept a step teaches, for folding attempts into mastery. */
    fun conceptOfStep(stepId: String): String? = conceptByStep[stepId]

    /** The concepts a lesson teaches, in the order they first appear. */
    fun conceptsOf(lesson: Lesson): List<String> = lesson.steps.mapNotNull { it.concept }.distinct()

    /** A pack's concepts are exactly its lessons' concepts. */
    fun conceptsOf(pack: SchemePack): List<String> =
        pack.lessons.mapNotNull { lesson(it) }.flatMap { conceptsOf(it) }.distinct()
}

object ContentLoader {
    val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = false
        encodeDefaults = false
        explicitNulls = false
    }

    fun load(source: ContentSource): Content {
        fun text(path: String) = source.read(path) ?: throw ContentException("missing content file: $path")
        fun <T> parse(path: String, f: (String) -> T): T = try {
            f(text(path))
        } catch (e: ContentException) {
            throw e
        } catch (e: Exception) {
            throw ContentException("$path: ${e.message}")
        }

        val curriculum = parse("curriculum.json") { json.decodeFromString<Curriculum>(it) }
        val glossary = parse("glossary.json") { json.decodeFromString<Glossary>(it) }
        val units = curriculum.levels.associate { lv ->
            lv.id to lv.unitFiles.map { f -> parse(f) { json.decodeFromString<CourseUnit>(it) } }
        }
        val scenes = parse("app/scenes.json") { json.decodeFromString<AppScenes>(it) }
        val packs = parse("packs.json") { json.decodeFromString<Packs>(it) }.packs
        val review = parse("review.json") { json.decodeFromString<ReviewLog>(it) }
        return Content(curriculum, glossary, units, scenes, packs, review)
    }
}
