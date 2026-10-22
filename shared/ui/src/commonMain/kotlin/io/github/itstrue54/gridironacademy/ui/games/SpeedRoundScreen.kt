package io.github.itstrue54.gridironacademy.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.model.MultipleChoice
import io.github.itstrue54.gridironacademy.model.TrueFalse
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.SpeedRoundController
import io.github.itstrue54.gridironacademy.ui.components.AnswerButton
import io.github.itstrue54.gridironacademy.ui.components.AnswerState
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.lesson.Prompt
import io.github.itstrue54.gridironacademy.ui.lesson.choiceOrder
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.answer_false
import io.github.itstrue54.gridironacademy.ui.resources.answer_true
import io.github.itstrue54.gridironacademy.ui.resources.close_game
import io.github.itstrue54.gridironacademy.ui.resources.speed_points
import io.github.itstrue54.gridironacademy.ui.resources.speed_question_of
import io.github.itstrue54.gridironacademy.ui.resources.speed_round
import io.github.itstrue54.gridironacademy.ui.resources.speed_streak
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import io.github.itstrue54.gridironacademy.ui.resources.speed_seconds_left
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.resources.speed_clock
import io.github.itstrue54.gridironacademy.ui.resources.speed_clock_off
import io.github.itstrue54.gridironacademy.ui.resources.speed_clock_on
import io.github.itstrue54.gridironacademy.ui.resources.speed_rules_adult
import io.github.itstrue54.gridironacademy.ui.resources.speed_rules_kid
import io.github.itstrue54.gridironacademy.ui.resources.speed_rules_win_adult
import io.github.itstrue54.gridironacademy.ui.resources.speed_rules_win_kid
import io.github.itstrue54.gridironacademy.ui.resources.speed_start
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import kotlin.math.roundToInt

private val LETTERS = listOf("A", "B", "C", "D")

/**
 * The Speed round (DESIGN 9.6): a draining timer ring, points and streak, one question at
 * a time. An answer moves on at once with no sheet; misses are reviewed at the end.
 * [round] defaults to the level's game in [state]; a test can pass one at any point.
 */
@Composable
fun SpeedRoundScreen(state: LearnerAppState, levelId: String, round: SpeedRoundController? = state.speedRoundControllerOrNull(levelId), clock: Boolean = true) {
    if (round == null) return // a quit game fading out
    val c = Gridiron.colors
    val skin = Gridiron.skin
    if (clock) {
        LaunchedEffect(round) {
            // Time adds up frame by frame from the round's own elapsed time, so a rebuilt
            // screen resumes where it was. A gap longer than MAX_FRAME_GAP is the app
            // being away (a call, the home button): that time doesn't count. Nor does time
            // on the rules screen or under the Leave dialog.
            var last = withFrameMillis { it }
            while (!round.over) {
                withFrameMillis { now ->
                    val paused = !round.started || state.confirmingLeave
                    if (!paused) round.tick(round.elapsedMillis + (now - last).coerceIn(0, MAX_FRAME_GAP_MS))
                    last = now
                }
            }
        }
    }
    LaunchedEffect(round.over) { if (round.over) state.finishSpeedRound(levelId, round) }

    if (!round.started) {
        val level = state.content.level(levelId)
        SpeedRoundRules(
            questions = round.questionCount,
            toWin = rightToPass(round.questionCount, level.gate.gamePassPct),
            seconds = (round.limitMillis / 1000).toInt(),
            untimedByDefault = remember(round) { state.speedRoundUntimedByDefault() },
            onClose = { state.requestBack() },
            onStart = round::start,
        )
        return
    }

    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        Row(Modifier.padding(start = 4.dp, end = skin.gutter), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.CLOSE, stringResource(Res.string.close_game), onClick = { state.requestBack() })
            Text(stringResource(Res.string.speed_round), style = Gridiron.type.title, color = c.ink, modifier = Modifier.weight(1f))
            Chip(stringResource(Res.string.speed_points, round.points))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = skin.gutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (round.timed) {
                    TimerRing(round.millisLeft, round.limitMillis)
                    Spacer(Modifier.width(16.dp))
                }
                Column {
                    Text(
                        stringResource(Res.string.speed_question_of, (round.answered + 1).coerceAtMost(round.questionCount), round.questionCount),
                        style = Gridiron.type.label, color = c.inkDim,
                    )
                    if (round.streak >= 2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlyphIcon(Glyph.FLAME, c.gold, size = 20.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.speed_streak, round.streak), style = Gridiron.type.answer, color = c.ink)
                        }
                    }
                }
            }
            // A fresh set of buttons for each question, so nothing stays selected.
            key(round.answered) {
                when (val step = round.current) {
                    is MultipleChoice -> {
                        Prompt(step.prompt)
                        choiceOrder(step.id, step.choices).forEachIndexed { shown, i ->
                            AnswerButton(step.choices[i], AnswerState.REST, onClick = { round.answer(Answer.Choice(i)) }, letter = LETTERS.getOrElse(shown) { "${shown + 1}" })
                        }
                    }
                    is TrueFalse -> {
                        Prompt(step.statement)
                        AnswerButton(stringResource(Res.string.answer_true), AnswerState.REST, onClick = { round.answer(Answer.Bool(true)) })
                        AnswerButton(stringResource(Res.string.answer_false), AnswerState.REST, onClick = { round.answer(Answer.Bool(false)) })
                    }
                    else -> Unit
                }
            }
            Spacer(Modifier.size(skin.gutter))
        }
    }
}

/**
 * Before the first question (DESIGN 9.6): what the round is, how to win, and the clock
 * switch. Nothing is timed until Start.
 */
@Composable
private fun SpeedRoundRules(
    questions: Int,
    toWin: Int,
    seconds: Int,
    untimedByDefault: Boolean,
    onClose: () -> Unit,
    onStart: (timed: Boolean) -> Unit,
) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    val kid = skin.voice == Voice.KID
    var timed by rememberSaveable { mutableStateOf(!untimedByDefault) }
    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        Row(Modifier.padding(start = 4.dp, end = skin.gutter), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.CLOSE, stringResource(Res.string.close_game), onClick = onClose)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = skin.gutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.speed_round), style = Gridiron.type.displayLarge,
                color = if (skin.primeTime) c.gold else c.ink, modifier = Modifier.semantics { heading() })
            Text(stringResource(if (kid) Res.string.speed_rules_kid else Res.string.speed_rules_adult, questions),
                style = Gridiron.type.body, color = c.ink)
            Text(stringResource(if (kid) Res.string.speed_rules_win_kid else Res.string.speed_rules_win_adult, toWin, questions),
                style = Gridiron.type.body, color = c.ink)
            Row(
                Modifier.fillMaxWidth().toggleable(timed, role = Role.Switch, onValueChange = { timed = it }).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.speed_clock), style = Gridiron.type.answer, color = c.ink)
                    Text(
                        if (timed) stringResource(Res.string.speed_clock_on, seconds) else stringResource(Res.string.speed_clock_off),
                        style = Gridiron.type.label, color = c.inkDim,
                    )
                }
                Switch(checked = timed, onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.gold, checkedThumbColor = c.onGold))
            }
        }
        PrimaryButton(stringResource(Res.string.speed_start), onClick = { onStart(timed) },
            modifier = Modifier.fillMaxWidth().padding(skin.gutter))
    }
}

/** The fewest right answers whose score reaches [passPct], as [SpeedRound.scorePct] rounds it. */
internal fun rightToPass(questions: Int, passPct: Int): Int =
    (0..questions).firstOrNull { k -> questions == 0 || (100f * k / questions).roundToInt() >= passPct } ?: questions

@Composable
private fun Chip(text: String) {
    val c = Gridiron.colors
    Surface(shape = Gridiron.skin.chipShape, color = c.surfaceRaised, contentColor = c.ink, border = BorderStroke(1.dp, c.line)) {
        Text(text, style = Gridiron.type.label.copy(fontFeatureSettings = "tnum"), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

/**
 * DESIGN 8: a gold ring draining over the limit, with the seconds as text. Under ten
 * seconds the ring gets thicker; it never turns red.
 */
@Composable
fun TimerRing(millisLeft: Long, limitMillis: Long) {
    val c = Gridiron.colors
    val seconds = ((millisLeft + 999) / 1000).toInt()
    val label = pluralStringResource(Res.plurals.speed_seconds_left, seconds, seconds)
    Box(Modifier.size(72.dp).clearAndSetSemantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = (if (seconds <= LOW_SECONDS) 8 else 5).dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(c.line, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            val fraction = if (limitMillis <= 0) 0f else millisLeft.toFloat() / limitMillis
            drawArc(c.gold, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("$seconds", style = Gridiron.type.scoreboard, color = c.ink)
    }
}

private const val LOW_SECONDS = 10
private const val MAX_FRAME_GAP_MS = 250L
