package io.github.itstrue54.gridironacademy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Which voice the app speaks in (DESIGN §2). Chosen by the profile's track. */
enum class Voice { KID, ADULT }

/**
 * Everything that differs between the Kid ("Pee-wee") and Adult ("Film Room") skins
 * (DESIGN §2), plus Little Huddle mode for Level 0 (DESIGN §2.1). A component is written
 * once and reads its skin from [LocalSkin].
 */
@Immutable
data class Skin(
    val voice: Voice,
    /** Level 0: pre-readers, picture answers, recorded voice, no timers (DESIGN §2.1). */
    val littleHuddle: Boolean,
    /** Prime Time (DESIGN 2.2): the always-dark night-game look for teens and adults. */
    val primeTime: Boolean = false,
    val cardShape: RoundedCornerShape,
    val buttonShape: RoundedCornerShape,
    val chipShape: RoundedCornerShape,
    /** Sheets round only their top corners (DESIGN §5). */
    val sheetShape: RoundedCornerShape,
    /** Minimum touch target for anything tappable, diagram players included. */
    val touchTarget: Dp,
    val gutter: Dp,
    val cardPadding: Dp,
    /** Multiplies every animation duration (DESIGN §10). */
    val motionScale: Float,
    /** Drawn size of a player mark on the field (DESIGN §7.3). */
    val playerMark: Dp,
    /** Kid draws helmets with jersey numbers; Adult draws discs and squares with position labels. */
    val helmets: Boolean,
    /** Field shows "LOS" at the left end of the line of scrimmage (Adult only, DESIGN §7.2). */
    val losTag: Boolean,
    val readAloudByDefault: Boolean,
    val soundByDefault: Boolean,
) {
    /** The read-the-defense reveal (DESIGN §7.7): 900ms Kid, 630ms Adult, scaled in Little Huddle. */
    val revealMillis: Int get() = (REVEAL_BASE_MILLIS * motionScale).toInt()

    companion object {
        private const val REVEAL_BASE_MILLIS = 900

        val Kid = Skin(
            voice = Voice.KID,
            littleHuddle = false,
            cardShape = RoundedCornerShape(20.dp),
            buttonShape = RoundedCornerShape(16.dp),
            chipShape = RoundedCornerShape(percent = 50),
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            touchTarget = 56.dp,
            gutter = 20.dp,
            cardPadding = 20.dp,
            motionScale = 1.0f,
            playerMark = 30.dp,
            helmets = true,
            losTag = false,
            readAloudByDefault = true,
            soundByDefault = true,
        )

        val Adult = Skin(
            voice = Voice.ADULT,
            littleHuddle = false,
            cardShape = RoundedCornerShape(10.dp),
            buttonShape = RoundedCornerShape(8.dp),
            chipShape = RoundedCornerShape(4.dp),
            sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            touchTarget = 48.dp,
            gutter = 16.dp,
            cardPadding = 16.dp,
            motionScale = 0.7f,
            playerMark = 22.dp,
            primeTime = true,
            helmets = false,
            losTag = true,
            readAloudByDefault = false,
            soundByDefault = false,
        )

        /** Level 0 only. A 4-7 profile that moves on to Kid L1 gets [Kid]. */
        val LittleHuddle = Kid.copy(
            littleHuddle = true,
            touchTarget = 64.dp,
            motionScale = 1.2f,
        )
    }
}

val LocalSkin = staticCompositionLocalOf { Skin.Adult }
