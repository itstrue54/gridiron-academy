package io.github.itstrue54.gridironacademy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Font families (DESIGN §4). Body: Atkinson Hyperlegible. Display: Fredoka W600 (Kid),
 * Barlow Semi Condensed W700 (Adult). The font files are not bundled yet, so these fall
 * back to the platform default; swapping them in is the only change needed here.
 */
object GridironFonts {
    val body: FontFamily = FontFamily.Default
    val kidDisplay: FontFamily = FontFamily.Default
    val adultDisplay: FontFamily = FontFamily.Default
}

/** The type scale (DESIGN §4). All sizes are sp and scale with the system. */
@Immutable
data class GridironType(
    val displayLarge: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val answer: TextStyle,
    val label: TextStyle,
    /** Tabular figures so a scoreboard or down-and-distance never jitters. */
    val scoreboard: TextStyle,
)

private const val TABULAR = "tnum"

private fun style(family: FontFamily, size: Int, line: Int, weight: FontWeight = FontWeight.Normal, features: String? = null) =
    TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontFeatureSettings = features,
    )

val KidType = GridironType(
    displayLarge = style(GridironFonts.kidDisplay, 40, 48, FontWeight.SemiBold),
    headline = style(GridironFonts.kidDisplay, 28, 34, FontWeight.SemiBold),
    title = style(GridironFonts.kidDisplay, 22, 28, FontWeight.SemiBold),
    body = style(GridironFonts.body, 20, 28),
    answer = style(GridironFonts.body, 20, 26, FontWeight.Bold),
    label = style(GridironFonts.body, 16, 20),
    scoreboard = style(GridironFonts.body, 28, 28, FontWeight.Bold, TABULAR),
)

val AdultType = GridironType(
    displayLarge = style(GridironFonts.adultDisplay, 36, 40, FontWeight.Bold),
    headline = style(GridironFonts.adultDisplay, 24, 30, FontWeight.Bold),
    title = style(GridironFonts.adultDisplay, 20, 26, FontWeight.Bold),
    body = style(GridironFonts.body, 16, 24),
    answer = style(GridironFonts.body, 17, 24, FontWeight.Bold),
    label = style(GridironFonts.body, 14, 18),
    scoreboard = style(GridironFonts.body, 24, 24, FontWeight.Bold, TABULAR),
)
