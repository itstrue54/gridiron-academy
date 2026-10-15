package io.github.itstrue54.gridironacademy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.atkinson_hyperlegible_bold
import io.github.itstrue54.gridironacademy.ui.resources.atkinson_hyperlegible_regular
import io.github.itstrue54.gridironacademy.ui.resources.barlow_semi_condensed_bold
import io.github.itstrue54.gridironacademy.ui.resources.fredoka_variable
import org.jetbrains.compose.resources.Font

/**
 * The three families (DESIGN §4), bundled under the OFL (docs/licenses/fonts).
 * Body: Atkinson Hyperlegible. Display: Fredoka 600 (Kid), Barlow Semi Condensed 700 (Adult).
 */
@Immutable
data class GridironFontFamilies(val body: FontFamily, val kidDisplay: FontFamily, val adultDisplay: FontFamily) {
    companion object {
        /** Before composition has loaded the bundled fonts, e.g. a CompositionLocal default. */
        val System = GridironFontFamilies(FontFamily.Default, FontFamily.Default, FontFamily.Default)
    }
}

/** Loads the bundled fonts. Font resources can only be read from composition. */
@Composable
fun rememberGridironFonts(): GridironFontFamilies {
    val regular = Font(Res.font.atkinson_hyperlegible_regular, FontWeight.Normal)
    val bold = Font(Res.font.atkinson_hyperlegible_bold, FontWeight.Bold)
    // Fredoka ships only as a variable font; pin its weight axis to the 600 DESIGN asks for.
    val fredoka = Font(
        Res.font.fredoka_variable,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight)),
    )
    val barlow = Font(Res.font.barlow_semi_condensed_bold, FontWeight.Bold)
    return remember(regular, bold, fredoka, barlow) {
        GridironFontFamilies(
            body = FontFamily(regular, bold),
            kidDisplay = FontFamily(fredoka),
            adultDisplay = FontFamily(barlow),
        )
    }
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

fun kidType(f: GridironFontFamilies) = GridironType(
    displayLarge = style(f.kidDisplay, 40, 48, FontWeight.SemiBold),
    headline = style(f.kidDisplay, 28, 34, FontWeight.SemiBold),
    title = style(f.kidDisplay, 22, 28, FontWeight.SemiBold),
    body = style(f.body, 20, 28),
    answer = style(f.body, 20, 26, FontWeight.Bold),
    label = style(f.body, 16, 20),
    scoreboard = style(f.body, 28, 28, FontWeight.Bold, TABULAR),
)

fun adultType(f: GridironFontFamilies) = GridironType(
    displayLarge = style(f.adultDisplay, 36, 40, FontWeight.Bold),
    headline = style(f.adultDisplay, 24, 30, FontWeight.Bold),
    title = style(f.adultDisplay, 20, 26, FontWeight.Bold),
    body = style(f.body, 16, 24),
    answer = style(f.body, 17, 24, FontWeight.Bold),
    label = style(f.body, 14, 18),
    scoreboard = style(f.body, 24, 24, FontWeight.Bold, TABULAR),
)
