package io.github.itstrue54.gridironacademy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.ui.theme.DiagramColors
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron

/** The one action a screen is for: gold fill, full width (DESIGN 5). At most one per screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Gridiron.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Gridiron.skin.buttonShape,
        color = if (enabled) c.gold else c.line,
        contentColor = if (enabled) c.onGold else c.inkDim,
        modifier = modifier.fillMaxWidth().heightIn(min = Gridiron.skin.touchTarget),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text, style = Gridiron.type.answer, textAlign = TextAlign.Center)
        }
    }
}

/** Transparent with a 2dp ink outline (DESIGN 5). */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Gridiron.colors
    Surface(
        onClick = onClick,
        shape = Gridiron.skin.buttonShape,
        color = Color.Transparent,
        contentColor = c.ink,
        border = BorderStroke(2.dp, c.ink),
        modifier = modifier.fillMaxWidth().heightIn(min = Gridiron.skin.touchTarget),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text, style = Gridiron.type.answer, textAlign = TextAlign.Center)
        }
    }
}

/** A glyph button with a spoken label, at the skin's touch target. */
@Composable
fun IconAction(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Gridiron.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = c.ink,
        modifier = modifier.size(Gridiron.skin.touchTarget).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) { GlyphIcon(glyph, if (enabled) c.ink else c.line) }
    }
}

enum class AnswerState { REST, SELECTED, CORRECT, RETRY, DISABLED }

/**
 * A full-width answer (DESIGN 8): letter chip, text, and after grading a check or a dash
 * so colour is never the only signal. [letter] null for True/False.
 */
@Composable
fun AnswerButton(
    text: String,
    state: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    letter: String? = null,
    stateWords: String = "",
) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    val edge = when (state) {
        AnswerState.SELECTED -> c.gold
        AnswerState.CORRECT -> c.correct
        AnswerState.RETRY -> c.retry
        else -> c.line
    }
    Surface(
        onClick = onClick,
        enabled = state == AnswerState.REST || state == AnswerState.SELECTED,
        shape = skin.buttonShape,
        color = c.surfaceRaised,
        contentColor = if (state == AnswerState.DISABLED) c.inkDim else c.ink,
        border = BorderStroke(if (state == AnswerState.REST || state == AnswerState.DISABLED) 2.dp else 3.dp, edge),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = skin.touchTarget)
            .semantics {
                role = Role.Button
                selected = state == AnswerState.SELECTED
                if (stateWords.isNotEmpty()) stateDescription = stateWords
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (letter != null) {
                Surface(
                    shape = skin.chipShape,
                    color = if (state == AnswerState.SELECTED) c.gold else c.surface,
                    contentColor = if (state == AnswerState.SELECTED) c.onGold else c.inkDim,
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier.size(32.dp).clearAndSetSemantics { },
                ) { Box(contentAlignment = Alignment.Center) { Text(letter, style = Gridiron.type.label) } }
                Spacer(Modifier.width(12.dp))
            }
            Text(text, style = Gridiron.type.answer, modifier = Modifier.weight(1f))
            when (state) {
                AnswerState.CORRECT -> GlyphIcon(Glyph.CHECK, c.correct)
                AnswerState.RETRY -> GlyphIcon(Glyph.DASH, c.retry)
                else -> Unit
            }
        }
    }
}

/**
 * Lesson progress as a drive (DESIGN 8): a strip of turf with yard ticks and a ball
 * moving from the goal line to the end zone as steps are done.
 */
@Composable
fun DriveBar(done: Int, total: Int, label: String, modifier: Modifier = Modifier) {
    val fraction = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(20.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        val r = size.height / 2
        drawRoundRect(DiagramColors.turfB, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
        drawRoundRect(DiagramColors.turfA, size = Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
        for (i in 1..9) {
            val x = size.width * i / 10f
            drawLine(DiagramColors.chalk.copy(alpha = 0.45f), Offset(x, size.height * 0.25f), Offset(x, size.height * 0.75f), 1.dp.toPx())
        }
        // The ball: a brown oval with white laces, riding the leading edge.
        val bx = (size.width * fraction).coerceIn(r * 1.3f, size.width - r * 1.3f)
        drawOval(BALL, Offset(bx - r * 1.3f, r * 0.25f), Size(r * 2.6f, r * 1.5f))
        drawLine(DiagramColors.chalk, Offset(bx - r * 0.45f, r), Offset(bx + r * 0.45f, r), 1.5.dp.toPx())
    }
}

private val BALL = Color(0xFF8B4A1C)

/** Three stars with the count in words for screen readers (DESIGN 8). */
@Composable
fun StarRow(stars: Int, label: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 20.dp) {
    val c = Gridiron.colors
    Row(modifier.clearAndSetSemantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            GlyphIcon(Glyph.STAR, if (i < stars) c.gold else c.line, size = size, filled = i < stars)
        }
    }
}

enum class Verdict { RIGHT, PARTIAL, NOT_QUITE }

/**
 * After every graded step (DESIGN 8): icon, one-line verdict, why, and Continue. Never
 * red; never taller than 45% of the screen, scrolling inside if the explanation is long.
 */
@Composable
fun FeedbackSheet(
    verdict: Verdict,
    headline: String,
    body: String,
    continueLabel: String,
    onContinue: () -> Unit,
    maxHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    detail: String = "",
) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    val accent = when (verdict) {
        Verdict.RIGHT -> c.correct
        Verdict.PARTIAL, Verdict.NOT_QUITE -> c.retry
    }
    Surface(
        shape = skin.sheetShape,
        color = c.surfaceRaised,
        contentColor = c.ink,
        border = BorderStroke(2.dp, accent),
        modifier = modifier.fillMaxWidth().heightIn(max = maxHeight),
    ) {
        Column(Modifier.padding(skin.cardPadding)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(if (verdict == Verdict.RIGHT) Glyph.CHECK else Glyph.DASH, accent, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(headline, style = Gridiron.type.title, color = accent, modifier = Modifier.semantics { heading() })
                }
                if (detail.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(detail, style = Gridiron.type.body, color = c.ink)
                }
                if (body.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(body, style = Gridiron.type.body, color = c.inkDim)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(continueLabel, onContinue)
        }
    }
}

/** A raised card with the skin's shape and a 1dp line (elevation is a tint, DESIGN 5). */
@Composable
fun Card(modifier: Modifier = Modifier, edge: Color? = null, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val c = Gridiron.colors
    val border = BorderStroke(1.dp, c.line)
    if (onClick != null) {
        Surface(onClick = onClick, shape = Gridiron.skin.cardShape, color = c.surfaceRaised, contentColor = c.ink, border = border, modifier = modifier.fillMaxWidth()) {
            CardBody(edge, content)
        }
    } else {
        Surface(shape = Gridiron.skin.cardShape, color = c.surfaceRaised, contentColor = c.ink, border = border, modifier = modifier.fillMaxWidth()) {
            CardBody(edge, content)
        }
    }
}

@Composable
private fun CardBody(edge: Color?, content: @Composable () -> Unit) {
    // A 4dp left edge marks state (passed, locked) as well as the card's text does.
    Row(Modifier.height(IntrinsicSize.Min)) {
        if (edge != null) Box(Modifier.width(4.dp).fillMaxHeight().background(edge))
        Box(Modifier.weight(1f).padding(Gridiron.skin.cardPadding)) { content() }
    }
}
