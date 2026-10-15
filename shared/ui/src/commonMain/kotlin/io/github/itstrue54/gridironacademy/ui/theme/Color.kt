package io.github.itstrue54.gridironacademy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The screen palette (DESIGN §3). Every colour in the app comes from here; no composable
 * writes a hex value. Contrast ratios are asserted by `ContrastTest`, so if a hex changes
 * the build says whether it still clears 4.5:1.
 */
@Immutable
data class GridironColors(
    /** The ground of every screen. */
    val surface: Color,
    /** Cards, sheets, answer buttons. */
    val surfaceRaised: Color,
    /** Hairlines, disabled. */
    val line: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text. */
    val inkDim: Color,
    /** The one action a screen is for; stars; streak. A fill, never text on surface. */
    val gold: Color,
    /** Text on gold. */
    val onGold: Color,
    val offense: Color,
    val defense: Color,
    /** A right answer: text, icon, edge. */
    val correct: Color,
    /** "Not quite": text, icon, edge. Amber, never red. */
    val retry: Color,
    /** Links and glossary terms. */
    val link: Color,
    val isDark: Boolean,
)

/** Light theme. */
val Daylight = GridironColors(
    surface = Color(0xFFF7F4EB),
    surfaceRaised = Color(0xFFFFFFFF),
    line = Color(0xFFD9D3C3),
    ink = Color(0xFF10201A),
    inkDim = Color(0xFF4A5850),
    gold = Color(0xFFF2B01E),
    onGold = Color(0xFF1B1400),
    offense = Color(0xFF1D4ED8),
    defense = Color(0xFFC2410C),
    correct = Color(0xFF176B3A),
    retry = Color(0xFF8A5A00),
    link = Color(0xFF14532D),
    isDark = false,
)

/** Dark theme. */
val Floodlight = GridironColors(
    surface = Color(0xFF0F1B16),
    surfaceRaised = Color(0xFF172A22),
    line = Color(0xFF2C4439),
    ink = Color(0xFFF3F0E7),
    inkDim = Color(0xFFAEBDB3),
    gold = Color(0xFFF2B01E),
    onGold = Color(0xFF1B1400),
    offense = Color(0xFF7DA8FF),
    defense = Color(0xFFFF9468),
    correct = Color(0xFF5BDB92),
    retry = Color(0xFFF5C451),
    link = Color(0xFF8FE3B0),
    isDark = true,
)

/**
 * The field diagram palette (DESIGN §3.1), identical in both themes: the field is dark
 * turf in both, because a light field washed out the side colours.
 */
object DiagramColors {
    val turfA = Color(0xFF1F5A3A)
    val turfB = Color(0xFF1A4E32)
    /** Yard lines, hash marks, LOS, pass routes, outlines, the ring around every player. */
    val chalk = Color(0xFFF3F0E7)
    val offenseFill = Color(0xFF1D4ED8)
    val defenseFill = Color(0xFFB73A07)
    /** Player labels on their fills. */
    val label = Color(0xFFFFFFFF)
    val routeRun = Color(0xFFF2B01E)
    val routeBlitz = Color(0xFFFF9468)
    val routeRotate = Color(0xFF8FE3FF)
    val routeDrop = Color(0xFFC9B8FF)
    val zoneFill = Color(0xFFFFFFFF).copy(alpha = 0.18f)
    val zoneEdge = Color(0xFFFFFFFF)
    /** The selection ring (DESIGN §7.3). */
    val select = Color(0xFFF2B01E)
    /** Grading rings on turf: the Floodlight values, which are the ones legible on dark green. */
    val gradeCorrect = Color(0xFF5BDB92)
    val gradeRetry = Color(0xFFF5C451)
    /** The glyph inside a grading badge: dark on the light badge fills. */
    val badgeInk = Color(0xFF0F1B16)
}
