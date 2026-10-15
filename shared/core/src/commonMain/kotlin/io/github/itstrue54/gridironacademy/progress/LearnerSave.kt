package io.github.itstrue54.gridironacademy.progress

import io.github.itstrue54.gridironacademy.model.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The age screen's bands (SPEC 5.2). Neutral, no default; the band decides the track,
 * the starting level and the skin. No birthday or real age is ever stored.
 */
@Serializable
enum class AgeBand(val label: String) {
    @SerialName("4-5") AGE_4_5("4-5"),
    @SerialName("6-7") AGE_6_7("6-7"),
    @SerialName("8-9") AGE_8_9("8-9"),
    @SerialName("10-12") AGE_10_12("10-12"),
    @SerialName("13-17") AGE_13_17("13-17"),
    @SerialName("18+") AGE_18_PLUS("18+");

    /** 4-12 take the kid track; 13+ the adult one (DESIGN 9.8). */
    val track: Track get() = if (this <= AGE_10_12) Track.KID else Track.ADULT

    /** Pre-readers start at Level 0, Little Huddle (SPEC 3.1). */
    val preReader: Boolean get() = this <= AGE_6_7

    /** The optional "I know some football" placement is offered from 10 (SPEC 4.5). */
    val offeredPlacement: Boolean get() = this >= AGE_10_12

    /** Under 13: COPPA rules apply to anything that would leave the device (SPEC 5.4). */
    val child: Boolean get() = this <= AGE_10_12
}

/** One local profile (SPEC 5.1): a nickname and an age band. Never a real name. */
@Serializable
data class Profile(
    val nickname: String,
    val ageBand: AgeBand,
)

/**
 * Everything the learner app keeps on the device for one profile. [schemaVersion] is
 * bumped with every format change, each with a migration (SPEC 9).
 */
@Serializable
data class LearnerSave(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val profile: Profile? = null,
    val progress: Progress = Progress(),
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
