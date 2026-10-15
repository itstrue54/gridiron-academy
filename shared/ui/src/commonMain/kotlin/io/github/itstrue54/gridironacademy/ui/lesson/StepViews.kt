package io.github.itstrue54.gridironacademy.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.engine.Rng
import io.github.itstrue54.gridironacademy.model.Diagram
import io.github.itstrue54.gridironacademy.model.DiagramTap
import io.github.itstrue54.gridironacademy.model.GameState
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Match
import io.github.itstrue54.gridironacademy.model.Order
import io.github.itstrue54.gridironacademy.model.Quality
import io.github.itstrue54.gridironacademy.model.Situation
import io.github.itstrue54.gridironacademy.ui.components.AnswerButton
import io.github.itstrue54.gridironacademy.ui.components.AnswerState
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.field.FieldDiagram
import io.github.itstrue54.gridironacademy.ui.field.MarkState
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.answer_false
import io.github.itstrue54.gridironacademy.ui.resources.answer_true
import io.github.itstrue54.gridironacademy.ui.resources.down_1
import io.github.itstrue54.gridironacademy.ui.resources.down_2
import io.github.itstrue54.gridironacademy.ui.resources.down_3
import io.github.itstrue54.gridironacademy.ui.resources.down_4
import io.github.itstrue54.gridironacademy.ui.resources.down_and_distance
import io.github.itstrue54.gridironacademy.ui.resources.down_and_goal
import io.github.itstrue54.gridironacademy.ui.resources.match_answer
import io.github.itstrue54.gridironacademy.ui.resources.match_hint
import io.github.itstrue54.gridironacademy.ui.resources.midfield
import io.github.itstrue54.gridironacademy.ui.resources.move_down
import io.github.itstrue54.gridironacademy.ui.resources.move_up
import io.github.itstrue54.gridironacademy.ui.resources.option_letter
import io.github.itstrue54.gridironacademy.ui.resources.order_hint
import io.github.itstrue54.gridironacademy.ui.resources.own_yard_line
import io.github.itstrue54.gridironacademy.ui.resources.quarter_clock
import io.github.itstrue54.gridironacademy.ui.resources.score_down
import io.github.itstrue54.gridironacademy.ui.resources.score_tied
import io.github.itstrue54.gridironacademy.ui.resources.score_up
import io.github.itstrue54.gridironacademy.ui.resources.selected
import io.github.itstrue54.gridironacademy.ui.resources.tap_many
import io.github.itstrue54.gridironacademy.ui.resources.tap_one_adult
import io.github.itstrue54.gridironacademy.ui.resources.tap_one_kid
import io.github.itstrue54.gridironacademy.ui.resources.their_yard_line
import io.github.itstrue54.gridironacademy.ui.resources.timeouts_left
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import org.jetbrains.compose.resources.stringResource

// Each view shows one step and reports the learner's answer as it changes. After
// grading ([graded] true) the views show what was right and stop taking input.

private val LETTERS = listOf("A", "B", "C", "D", "E", "F")

@Composable
fun Prompt(text: String) {
    Text(text, style = Gridiron.type.body, color = Gridiron.colors.ink, modifier = Modifier.semantics { heading() })
}

@Composable
private fun StepDiagram(diagram: Diagram?, description: String) {
    if (diagram != null) FieldDiagram(diagram, description = description, modifier = Modifier.fillMaxWidth())
}

@Composable
fun LearnView(step: Learn) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(step.title, style = Gridiron.type.headline, color = Gridiron.colors.ink, modifier = Modifier.semantics { heading() })
        StepDiagram(step.diagram, step.diagram?.caption?.ifEmpty { step.title } ?: "")
        Text(step.body, style = Gridiron.type.body, color = Gridiron.colors.ink)
    }
}

/** Multiple choice and situation options share this list (DESIGN 9.3). */
@Composable
fun ChoiceList(
    choices: List<String>,
    selected: Int?,
    graded: Boolean,
    isRight: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
) {
    val selectedWord = stringResource(Res.string.selected)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        choices.forEachIndexed { i, text ->
            val state = when {
                !graded -> if (selected == i) AnswerState.SELECTED else AnswerState.REST
                isRight(i) -> AnswerState.CORRECT
                selected == i -> AnswerState.RETRY
                else -> AnswerState.DISABLED
            }
            val letter = LETTERS.getOrElse(i) { "${i + 1}" }
            AnswerButton(
                text = text,
                state = state,
                onClick = { onSelect(i) },
                letter = letter,
                stateWords = listOfNotNull(
                    stringResource(Res.string.option_letter, letter),
                    selectedWord.takeIf { state == AnswerState.SELECTED },
                ).joinToString(", "),
            )
        }
    }
}

@Composable
fun MultipleChoiceView(prompt: String, diagram: Diagram?, choices: List<String>, answer: Int, selected: Int?, graded: Boolean, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt(prompt)
        StepDiagram(diagram, diagram?.caption?.ifEmpty { prompt } ?: "")
        ChoiceList(choices, selected, graded, isRight = { it == answer }, onSelect = onSelect)
    }
}

@Composable
fun TrueFalseView(statement: String, answer: Boolean, selected: Boolean?, graded: Boolean, onSelect: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt(statement)
        listOf(true, false).forEach { value ->
            val state = when {
                !graded -> if (selected == value) AnswerState.SELECTED else AnswerState.REST
                value == answer -> AnswerState.CORRECT
                selected == value -> AnswerState.RETRY
                else -> AnswerState.DISABLED
            }
            AnswerButton(
                text = stringResource(if (value) Res.string.answer_true else Res.string.answer_false),
                state = state,
                onClick = { onSelect(value) },
            )
        }
    }
}

/** Tap the right player(s) or zone(s) (DESIGN 9.3); zoom works here too. */
@Composable
fun DiagramTapView(step: DiagramTap, tapped: Set<String>, graded: Boolean, onChange: (Set<String>) -> Unit) {
    val kid = Gridiron.skin.voice == Voice.KID
    val marks: Map<String, MarkState> = if (!graded) {
        tapped.associateWith { MarkState.SELECTED }
    } else {
        val ids = step.diagram.players.map { it.id } + step.diagram.zones.map { it.id }
        ids.mapNotNull { id ->
            when {
                id in tapped && id in step.targets -> id to MarkState.CORRECT
                id in tapped -> id to MarkState.WRONG
                id in step.targets -> id to MarkState.MISSED
                else -> null
            }
        }.toMap()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt(step.prompt)
        Text(
            if (step.targets.size > 1) stringResource(Res.string.tap_many, step.targets.size)
            else stringResource(if (kid) Res.string.tap_one_kid else Res.string.tap_one_adult),
            style = Gridiron.type.label,
            color = Gridiron.colors.inkDim,
        )
        FieldDiagram(
            step.diagram,
            description = step.diagram.caption.ifEmpty { step.prompt },
            marks = marks,
            onTap = if (graded) null else { id ->
                onChange(
                    when {
                        id in tapped -> tapped - id
                        step.targets.size == 1 -> setOf(id) // one answer: a new tap replaces the old
                        else -> tapped + id
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Match (DESIGN 8, MatchBoard): tap a left item, then its partner on the right. A paired
 * right item shows its partner's letter. Tap a paired left item to undo it.
 */
@Composable
fun MatchView(step: Match, rightOrder: List<String>, pairs: Map<String, String>, selectedLeft: String?, graded: Boolean, onSelectLeft: (String?) -> Unit, onPairs: (Map<String, String>) -> Unit) {
    val c = Gridiron.colors
    val lefts = step.pairs.map { it.left }
    val truth = step.pairs.associate { it.left to it.right }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt(step.prompt)
        Text(stringResource(Res.string.match_hint), style = Gridiron.type.label, color = c.inkDim)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lefts.forEachIndexed { i, left ->
                    val state = when {
                        graded -> if (pairs[left] == truth[left]) AnswerState.CORRECT else AnswerState.RETRY
                        selectedLeft == left -> AnswerState.SELECTED
                        else -> AnswerState.REST
                    }
                    MatchChip(LETTERS[i], left, state, paired = left in pairs) {
                        if (left in pairs) onPairs(pairs - left)
                        onSelectLeft(if (selectedLeft == left) null else left)
                    }
                    if (graded && pairs[left] != truth[left]) {
                        Text(stringResource(Res.string.match_answer, truth[left].orEmpty()), style = Gridiron.type.label, color = c.correct, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rightOrder.forEach { right ->
                    val owner = pairs.entries.firstOrNull { it.value == right }?.key
                    val letter = owner?.let { LETTERS[lefts.indexOf(it)] }
                    val state = when {
                        graded -> AnswerState.DISABLED
                        owner != null -> AnswerState.SELECTED
                        else -> AnswerState.REST
                    }
                    MatchChip(letter, right, state, paired = owner != null) {
                        val left = selectedLeft ?: return@MatchChip
                        // Pairing a right item takes it from whoever had it.
                        onPairs(pairs.filterValues { it != right } + (left to right))
                        onSelectLeft(lefts.firstOrNull { it != left && it !in pairs && it != owner })
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchChip(letter: String?, text: String, state: AnswerState, paired: Boolean, onClick: () -> Unit) {
    val c = Gridiron.colors
    val edge = when (state) {
        AnswerState.SELECTED -> c.gold
        AnswerState.CORRECT -> c.correct
        AnswerState.RETRY -> c.retry
        else -> c.line
    }
    Surface(
        onClick = onClick,
        enabled = state != AnswerState.DISABLED && state != AnswerState.CORRECT && state != AnswerState.RETRY,
        shape = Gridiron.skin.buttonShape,
        color = c.surfaceRaised,
        contentColor = c.ink,
        border = BorderStroke(if (state == AnswerState.REST) 2.dp else 3.dp, edge),
        modifier = Modifier.fillMaxWidth().heightIn(min = Gridiron.skin.touchTarget).semantics {
            role = Role.Button
            selected = state == AnswerState.SELECTED
            if (letter != null && paired) stateDescription = letter
        },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (letter != null) {
                Text(letter, style = Gridiron.type.label, color = c.inkDim, modifier = Modifier.width(18.dp).clearAndSetSemantics { })
            }
            Text(text, style = Gridiron.type.label, modifier = Modifier.weight(1f))
            when (state) {
                AnswerState.CORRECT -> GlyphIcon(Glyph.CHECK, c.correct, size = 18.dp)
                AnswerState.RETRY -> GlyphIcon(Glyph.DASH, c.retry, size = 18.dp)
                else -> Unit
            }
        }
    }
}

/** Order (DESIGN 8, OrderList): up and down buttons; no drag needed. */
@Composable
fun OrderView(step: Order, order: List<String>, graded: Boolean, onChange: (List<String>) -> Unit) {
    val c = Gridiron.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Prompt(step.prompt)
        Text(stringResource(Res.string.order_hint), style = Gridiron.type.label, color = c.inkDim)
        order.forEachIndexed { i, item ->
            val state = when {
                !graded -> AnswerState.REST
                step.items[i] == item -> AnswerState.CORRECT
                else -> AnswerState.RETRY
            }
            val edge = when (state) { AnswerState.CORRECT -> c.correct; AnswerState.RETRY -> c.retry; else -> c.line }
            Surface(shape = Gridiron.skin.buttonShape, color = c.surfaceRaised, contentColor = c.ink, border = BorderStroke(2.dp, edge)) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = Gridiron.type.answer, color = c.inkDim, modifier = Modifier.width(24.dp))
                    Text(item, style = Gridiron.type.body, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                    if (graded) {
                        GlyphIcon(if (state == AnswerState.CORRECT) Glyph.CHECK else Glyph.DASH, edge, Modifier.padding(12.dp))
                    } else {
                        IconAction(Glyph.UP, stringResource(Res.string.move_up, item), enabled = i > 0, onClick = { onChange(order.swap(i, i - 1)) })
                        IconAction(Glyph.DOWN, stringResource(Res.string.move_down, item), enabled = i < order.lastIndex, onClick = { onChange(order.swap(i, i + 1)) })
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> = toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

/** Game-state decision: the situation strip, then the options (DESIGN 9.5). */
@Composable
fun SituationView(step: Situation, selected: Int?, graded: Boolean, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SituationStrip(step.state)
        Prompt(step.prompt)
        ChoiceList(step.options.map { it.text }, selected, graded, isRight = { step.options[it].quality == Quality.BEST }, onSelect = onSelect)
    }
}

/** Down and distance large, then the clock, field position, score and timeouts. */
@Composable
fun SituationStrip(s: GameState) {
    val c = Gridiron.colors
    val down = stringResource(
        when (s.down) { 1 -> Res.string.down_1; 2 -> Res.string.down_2; 3 -> Res.string.down_3; else -> Res.string.down_4 },
    )
    val goalToGo = s.toGo >= 100 - s.yardLine
    val headline = if (goalToGo) stringResource(Res.string.down_and_goal, down) else stringResource(Res.string.down_and_distance, down, s.toGo)
    val clock = "${s.clock / 60}:${(s.clock % 60).toString().padStart(2, '0')}"
    val spot = when {
        s.yardLine < 50 -> stringResource(Res.string.own_yard_line, s.yardLine)
        s.yardLine == 50 -> stringResource(Res.string.midfield)
        else -> stringResource(Res.string.their_yard_line, 100 - s.yardLine)
    }
    val score = when {
        s.scoreDiff > 0 -> stringResource(Res.string.score_up, s.scoreDiff)
        s.scoreDiff < 0 -> stringResource(Res.string.score_down, -s.scoreDiff)
        else -> stringResource(Res.string.score_tied)
    }
    Surface(shape = Gridiron.skin.cardShape, color = c.surfaceRaised, contentColor = c.ink, border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Gridiron.skin.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(headline, style = Gridiron.type.scoreboard, color = c.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(Res.string.quarter_clock, s.quarter, clock), style = Gridiron.type.label.copy(fontFeatureSettings = "tnum"), color = c.ink)
                Text(spot, style = Gridiron.type.label, color = c.ink)
                Text(score, style = Gridiron.type.label, color = c.ink)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) { }) {
                repeat(3) { i ->
                    Box(Modifier.size(10.dp).padding(1.dp)) {
                        androidx.compose.foundation.Canvas(Modifier.size(8.dp)) { drawCircle(if (i < s.timeouts) c.gold else c.line) }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.timeouts_left, s.timeouts), style = Gridiron.type.label, color = c.inkDim)
            }
        }
    }
}

/** A fixed shuffle per step, so a retry and a screenshot show the same order (SPEC 4.3). */
fun <T> stepShuffle(stepId: String, items: List<T>, avoid: List<T>? = null): List<T> {
    val rng = Rng(stepId.hashCode().toLong() * SHUFFLE_SALT + 1)
    var out = rng.shuffled(items)
    // An order step must never start already solved.
    while (avoid != null && items.size > 1 && out == avoid) out = rng.shuffled(items)
    return out
}

private const val SHUFFLE_SALT = 2654435761L
