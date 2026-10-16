package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.AssignmentLine
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.progress.AssignmentStatus
import io.github.itstrue54.gridironacademy.progress.ConceptMastery
import io.github.itstrue54.gridironacademy.progress.MasteryState
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.components.SecondaryButton
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.stringResource

/** The learner card (DESIGN 14.2). */
@Composable
fun LearnerScreen(state: CoachAppState, dash: CoachDashboard, learnerId: String) {
    val c = Gridiron.colors
    val l = dash.snapshot.learner(learnerId) ?: return
    val term = { id: String -> state.content.glossary.entries.firstOrNull { it.id == id }?.term ?: id }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Gridiron.skin.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
                Monogram(l.nickname, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(l.nickname, style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
                    Text(dash.levelOf(l)?.let { stringResource(Res.string.level_named, it.number, it.title) } ?: "",
                        style = Gridiron.type.label, color = c.inkDim)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("${l.streakDays}", stringResource(Res.string.stat_streak))
                Stat("${dash.minutesThisWeek(l.id)}", stringResource(Res.string.stat_minutes_week))
                Stat("${dash.mastery(l.id).values.count { it.secure }}", stringResource(Res.string.stat_mastered))
            }
        }

        val lines = dash.lines(l.id)
        item { SectionTitle(stringResource(Res.string.section_assignments)) }
        if (lines.isEmpty()) {
            item { Text(stringResource(Res.string.no_assignments), style = Gridiron.type.body, color = c.inkDim) }
        }
        items(lines, key = { "a-" + it.assignment.id }) { AssignmentCardCoach(it, dash, state.today) }
        item { SecondaryButton(stringResource(Res.string.assign_to, l.nickname), onClick = { state.startAssign(learnerIds = listOf(l.id)) }) }

        val weakest = dash.weakest(l.id)
        if (weakest.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.section_work_on)) }
            items(weakest, key = { "w-" + it.concept }) { m ->
                WeakConcept(m, term(m.concept), dash.packFor(m.concept)?.let { p -> { state.startAssign(listOf(p.id), listOf(l.id)) } })
            }
        }

        val concepts = dash.levelConcepts(l)
        if (concepts.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.section_mastery, l.level)) }
            item { MasteryStrip(concepts.map { dash.mastery(l.id, it) }, term) }
        }

        item { SectionTitle(stringResource(Res.string.section_activity)) }
        item { ActivityStrip(dash, l.id) }
    }
}

/** Coach-side AssignmentCard (DESIGN 8): title, due, ring, note, status. */
@Composable
fun AssignmentCardCoach(line: AssignmentLine, dash: CoachDashboard, today: Long) {
    val c = Gridiron.colors
    val a = line.assignment
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressRing(line.fraction)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dash.packsOf(a).joinToString(" + ") { it.title }, style = Gridiron.type.answer, color = c.ink)
                Text(dueText(a.dueDay, today), style = Gridiron.type.label, color = if (line.status == AssignmentStatus.BEHIND) c.retry else c.inkDim)
                if (a.note.isNotBlank()) Text("“${a.note}”", style = Gridiron.type.label, color = c.inkDim)
                if (line.status != AssignmentStatus.DONE) {
                    Text(stringResource(Res.string.plan_left, line.planNow.lessons, line.planNow.minutes), style = Gridiron.type.label, color = c.inkDim)
                }
            }
            StatusPill(line.status)
        }
    }
}

@Composable
private fun WeakConcept(m: ConceptMastery, name: String, onAssign: (() -> Unit)?) {
    val c = Gridiron.colors
    Card(edge = masteryColor(m.state)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MasteryGlyph(m.state)
            Column(Modifier.weight(1f)) {
                Text(name, style = Gridiron.type.answer, color = c.ink)
                Text(stringResource(Res.string.concept_score, stringResource(masteryWord(m.state)), m.score), style = Gridiron.type.label, color = c.inkDim)
            }
            if (onAssign != null) {
                androidx.compose.material3.TextButton(onClick = onAssign) {
                    Text(stringResource(Res.string.assign_practice), style = Gridiron.type.label, color = c.link)
                }
            }
        }
    }
}

/** The level's concepts as glyph + name chips, wrapping (DESIGN 14.2 Mastery). */
@Composable
private fun MasteryStrip(cells: List<ConceptMastery>, term: (String) -> String) {
    val c = Gridiron.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.forEach { m ->
            val word = stringResource(masteryWord(m.state))
            val name = term(m.concept)
            Row(
                Modifier.clearAndSetSemantics { contentDescription = "$name: $word" }.widthIn(max = 200.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MasteryGlyph(m.state, size = 18.dp)
                Text(name, style = Gridiron.type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    MasteryLegend()
}

@Composable
fun MasteryLegend() {
    val c = Gridiron.colors
    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MasteryState.entries.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MasteryGlyph(s, size = 14.dp)
                Text(stringResource(masteryWord(s)), style = Gridiron.type.label, color = c.inkDim)
            }
        }
    }
}

/** The last seven days, oldest first: answers and minutes, as bars plus numbers. */
@Composable
private fun ActivityStrip(dash: CoachDashboard, learnerId: String) {
    val c = Gridiron.colors
    val days = dash.activity(learnerId).reversed()
    val most = (days.maxOfOrNull { it.answers } ?: 0).coerceAtLeast(1)
    Card {
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            days.forEach { d ->
                val label = stringResource(Res.string.activity_day, weekday(d.epochDay), d.answers, d.minutes)
                Column(
                    Modifier.weight(1f).clearAndSetSemantics { contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(if (d.answers > 0) "${d.minutes}m" else "", style = Gridiron.type.label, color = c.inkDim)
                    Box(
                        Modifier.padding(top = 2.dp).width(14.dp).height((60f * d.answers / most).dp.coerceAtLeast(2.dp))
                            .background(if (d.answers > 0) c.gold else c.line, RoundedCornerShape(3.dp)),
                    )
                    Text(weekdayShort(d.epochDay), style = Gridiron.type.label, color = c.inkDim, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
