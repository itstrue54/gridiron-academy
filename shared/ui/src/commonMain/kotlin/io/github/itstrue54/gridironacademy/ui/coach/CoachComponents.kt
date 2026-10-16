package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.LinkedLearner
import io.github.itstrue54.gridironacademy.progress.AssignmentStatus
import io.github.itstrue54.gridironacademy.progress.MasteryState
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The state's colour (DESIGN 8.1). Never shown without the glyph or the word. */
@Composable
fun masteryColor(s: MasteryState): Color {
    val c = Gridiron.colors
    return when (s) {
        MasteryState.NEW -> c.line
        MasteryState.LEARNING -> c.inkDim
        MasteryState.PRACTICED -> c.offense
        MasteryState.MASTERED -> c.correct
        MasteryState.RUSTY -> c.retry
    }
}

fun masteryWord(s: MasteryState): StringResource = when (s) {
    MasteryState.NEW -> Res.string.mastery_new
    MasteryState.LEARNING -> Res.string.mastery_learning
    MasteryState.PRACTICED -> Res.string.mastery_practiced
    MasteryState.MASTERED -> Res.string.mastery_mastered
    MasteryState.RUSTY -> Res.string.mastery_rusty
}

/**
 * The mastery glyph (DESIGN 8.1): empty, quarter, half, full with a check, full with a
 * dash. The shape carries the state; the colour only repeats it.
 */
@Composable
fun MasteryGlyph(state: MasteryState, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val color = masteryColor(state)
    val mark = Gridiron.colors.surface
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2 - 1.5f
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val ring = Stroke(width = 2.5f)
        val box = Size(r * 2, r * 2)
        val topLeft = Offset(center.x - r, center.y - r)
        when (state) {
            MasteryState.NEW -> drawCircle(color, r, center, style = ring)
            MasteryState.LEARNING -> {
                drawCircle(color, r, center, style = ring)
                drawArc(color, -90f, 90f, useCenter = true, topLeft = topLeft, size = box)
            }
            MasteryState.PRACTICED -> {
                drawCircle(color, r, center, style = ring)
                drawArc(color, -90f, 180f, useCenter = true, topLeft = topLeft, size = box)
            }
            MasteryState.MASTERED -> {
                drawCircle(color, r, center)
                val u = r / 8
                drawLine(mark, Offset(center.x - 4 * u, center.y), Offset(center.x - u, center.y + 3 * u), 2.2f, StrokeCap.Round)
                drawLine(mark, Offset(center.x - u, center.y + 3 * u), Offset(center.x + 4.5f * u, center.y - 3.5f * u), 2.2f, StrokeCap.Round)
            }
            MasteryState.RUSTY -> {
                drawCircle(color, r, center)
                drawLine(mark, Offset(center.x - r / 2, center.y), Offset(center.x + r / 2, center.y), 2.6f, StrokeCap.Round)
            }
        }
    }
}

/** "On track", "Behind", "Done" with an icon (DESIGN 14.2, 14.8). Never "Failing". */
@Composable
fun StatusPill(status: AssignmentStatus, modifier: Modifier = Modifier) {
    val c = Gridiron.colors
    val (word, color, glyph) = when (status) {
        AssignmentStatus.ON_TRACK -> Triple(Res.string.status_on_track, c.offense, Glyph.UP)
        AssignmentStatus.BEHIND -> Triple(Res.string.status_behind, c.retry, Glyph.DASH)
        AssignmentStatus.DONE -> Triple(Res.string.status_done, c.correct, Glyph.CHECK)
    }
    Row(
        modifier.border(1.5.dp, color, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GlyphIcon(glyph, color, size = 14.dp)
        Text(stringResource(word), style = Gridiron.type.label, color = c.ink)
    }
}

/**
 * A person's avatar stand-in until the helmet avatars ship: their initial in a ring. A
 * [square] mark is a TeamMark (DESIGN 8): two letters.
 */
@Composable
fun Monogram(text: String, modifier: Modifier = Modifier, size: Dp = 40.dp, square: Boolean = false) {
    val c = Gridiron.colors
    val shape = if (square) RoundedCornerShape(8.dp) else CircleShape
    Box(
        modifier.size(size).background(c.surfaceRaised, shape).border(2.dp, c.gold, shape).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(text.take(if (square) 2 else 1).uppercase(), style = Gridiron.type.title, color = c.ink)
    }
}

/** Assignment progress (DESIGN 8 AssignmentCard): a ring with the percent as text. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val c = Gridiron.colors
    val pct = (fraction * 100).toInt()
    val label = stringResource(Res.string.percent_done, pct)
    Box(modifier.size(size).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 4.dp.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(c.line, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(c.gold, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text("$pct", style = Gridiron.type.label, color = c.ink)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = Gridiron.type.eyebrow, color = Gridiron.colors.inkDim,
        modifier = modifier.padding(top = 8.dp).semantics { heading() },
    )
}

/** Nickname, level and when they last played: the line under a roster name. */
@Composable
fun learnerSubline(l: LinkedLearner, today: Long): String {
    val active = when (val d = l.lastActiveDay) {
        null -> stringResource(Res.string.active_never)
        today -> stringResource(Res.string.active_today)
        today - 1 -> stringResource(Res.string.active_yesterday)
        else -> stringResource(Res.string.active_days_ago, (today - d).toInt())
    }
    return stringResource(Res.string.learner_subline, l.level, l.streakDays, active)
}

/** "Due Friday", "Due tomorrow", "Past due" (DESIGN 8 AssignmentCard): relative, not a date. */
@Composable
fun dueText(dueDay: Long?, today: Long): String = when {
    dueDay == null -> stringResource(Res.string.due_none)
    dueDay < today -> stringResource(Res.string.due_past)
    dueDay == today -> stringResource(Res.string.due_today)
    dueDay == today + 1 -> stringResource(Res.string.due_tomorrow)
    dueDay - today < 7 -> stringResource(Res.string.due_weekday, weekday(dueDay))
    else -> stringResource(Res.string.due_in_days, (dueDay - today).toInt())
}

/** Monday-first weekday name for an epoch day (1970-01-01 was a Thursday). */
@Composable
fun weekday(epochDay: Long): String = stringResource(WEEKDAYS[(((epochDay + 3) % 7 + 7) % 7).toInt()])

@Composable
fun weekdayShort(epochDay: Long): String = weekday(epochDay).take(3)

private val WEEKDAYS = listOf(
    Res.string.weekday_mon, Res.string.weekday_tue, Res.string.weekday_wed, Res.string.weekday_thu,
    Res.string.weekday_fri, Res.string.weekday_sat, Res.string.weekday_sun,
)

/** One line of label + value, for small stat rows. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = Gridiron.colors
    Column(modifier) {
        Text(value, style = Gridiron.type.scoreboard, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = Gridiron.type.label, color = c.inkDim)
    }
}
