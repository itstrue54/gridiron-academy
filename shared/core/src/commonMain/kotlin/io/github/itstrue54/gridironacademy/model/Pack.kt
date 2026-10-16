package io.github.itstrue54.gridironacademy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PackSide {
    @SerialName("offense") OFFENSE,
    @SerialName("defense") DEFENSE,
    @SerialName("special") SPECIAL,
    @SerialName("rules") RULES,
}

/** The assignment check at the end of a pack: a test drawn from the pack's lessons. */
@Serializable
data class PackCheck(val questions: Int = 10, val passPct: Int = 80)

/**
 * A scheme pack (SPEC 7.4): a curated bundle a coach or parent assigns, such as "4-3 Over
 * with Cover 2 and Tampa 2". Content, not code: `packs.json`. Its concepts are the
 * concepts its lessons teach, so they can never drift from the lessons.
 */
@Serializable
data class SchemePack(
    val id: String,
    val title: String,
    val blurb: String,
    val side: PackSide,
    /** The level the pack is written at; assignments warn when it's well above a learner. */
    val level: Int,
    val lessons: List<String>,
    val games: List<GameType> = emptyList(),
    val check: PackCheck = PackCheck(),
)

@Serializable
data class Packs(val version: Int, val packs: List<SchemePack>)
