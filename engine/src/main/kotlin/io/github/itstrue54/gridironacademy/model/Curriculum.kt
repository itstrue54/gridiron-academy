package io.github.itstrue54.gridironacademy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Track {
    /** Written for roughly ages 8-12: short text, big pictures, read-aloud. */
    @SerialName("kid") KID,
    /** Written for adults new to the game: same ideas, quicker, no cartoons. */
    @SerialName("adult") ADULT,
    /** Both tracks converge here and share content. */
    @SerialName("shared") SHARED,
}

@Serializable
enum class GameType {
    /** Timed rapid-fire questions, streak bonus. */
    @SerialName("speed_round") SPEED_ROUND,
    /** A run of pre-snap pictures; name the coverage / front / pressure. */
    @SerialName("read_defense") READ_DEFENSE,
    /** A game's worth of down-distance-clock decisions. */
    @SerialName("game_day") GAME_DAY,
    /** Tap-the-diagram challenges. */
    @SerialName("diagram_dash") DIAGRAM_DASH,
}

@Serializable
data class LevelGate(
    /** Share of graded items to get right on the level test to pass. */
    val testPassPct: Int = 80,
    val testQuestions: Int = 12,
    val game: GameType,
    val gameQuestions: Int = 10,
    /** Share to hit in the level game to pass. */
    val gamePassPct: Int = 70,
)

@Serializable
data class Level(
    val id: String,
    val number: Int,
    val track: Track,
    val title: String,
    val tagline: String,
    /** Levels that must all be passed first. */
    val requires: List<String> = emptyList(),
    /** If non-empty, at least one of these must be passed (the tracks converge this way). */
    val requiresAny: List<String> = emptyList(),
    val unitFiles: List<String>,
    val gate: LevelGate,
)

@Serializable
data class Curriculum(val version: Int, val levels: List<Level>)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val blurb: String = "",
    val steps: List<Step>,
)

@Serializable
data class CourseUnit(
    val id: String,
    val title: String,
    val lessons: List<Lesson>,
)

@Serializable
data class GlossaryEntry(
    val id: String,
    val term: String,
    /** Plain, short, read-aloud-friendly. */
    val kid: String,
    val full: String,
    /** Level number where the term is first taught. */
    val level: Int,
)

@Serializable
data class Glossary(val entries: List<GlossaryEntry>)
