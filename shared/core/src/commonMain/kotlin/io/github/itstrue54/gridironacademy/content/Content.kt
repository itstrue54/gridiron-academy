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
) {
    fun level(id: String): Level = curriculum.levels.first { it.id == id }
    fun unitsOf(level: Level): List<CourseUnit> = unitsByLevel.getValue(level.id)
    fun levelOfLesson(lessonId: String): Level? =
        curriculum.levels.firstOrNull { l -> unitsOf(l).any { u -> u.lessons.any { it.id == lessonId } } }
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
        return Content(curriculum, glossary, units)
    }
}
