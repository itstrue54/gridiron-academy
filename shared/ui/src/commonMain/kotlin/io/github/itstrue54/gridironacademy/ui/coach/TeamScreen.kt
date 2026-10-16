package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.data.HeatRow
import io.github.itstrue54.gridironacademy.data.MissedItem
import io.github.itstrue54.gridironacademy.data.PositionGroup
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.components.SecondaryButton
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.stringResource

/** Team view (DESIGN 14.3). */
@Composable
fun TeamScreen(state: CoachAppState, dash: CoachDashboard, teamId: String) {
    val c = Gridiron.colors
    val team = dash.snapshot.team(teamId) ?: return
    val assigned = dash.snapshot.assignments.filter { it.teamId == teamId }.flatMap { it.packIds }.distinct()
    // Packs this team was assigned come first; the rest follow in catalogue order.
    val packs = state.content.packs.sortedBy { if (it.id in assigned) 0 else 1 }
    var packId by remember(teamId) { mutableStateOf(packs.first().id) }
    var group by remember(teamId) { mutableStateOf<PositionGroup?>(null) }
    var table by remember { mutableStateOf(false) }
    var concept by remember(packId) { mutableStateOf<String?>(null) }
    val pack = state.content.pack(packId) ?: return
    val rows = dash.heatMap(teamId, pack, group)
    val concepts = state.content.conceptsOf(pack)
    val term = { id: String -> state.content.glossary.entries.firstOrNull { it.id == id }?.term ?: id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Gridiron.skin.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
                Monogram(teamMonogram(team.name), square = true, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(team.name, style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
                    Text("${team.variant} · ${team.ageGroup} · ${team.season}", style = Gridiron.type.label, color = c.inkDim)
                }
            }
        }
        item { SectionTitle(stringResource(Res.string.section_pack)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(packs, key = { it.id }) { p ->
                    Chip(p.title + if (p.id in assigned) " •" else "", selected = p.id == packId) { packId = p.id }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(stringResource(Res.string.all_positions), selected = group == null) { group = null } }
                val present = dash.snapshot.membersOf(teamId).flatMap { it.positions }.toSet()
                items(PositionGroup.entries.filter { it in present }, key = { it.name }) { g ->
                    Chip(g.name, selected = group == g) { group = if (group == g) null else g }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(Res.string.section_heat_map), Modifier.weight(1f))
                Chip(stringResource(Res.string.view_grid), selected = !table) { table = false }
                Spacer(Modifier.width(8.dp))
                Chip(stringResource(Res.string.view_table), selected = table) { table = true }
            }
        }
        item {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeatMap(rows, concepts, term, table, selected = concept, onConcept = { concept = if (concept == it) null else it })
                    Text(stringResource(Res.string.heat_map_hint), style = Gridiron.type.label, color = c.inkDim)
                    MasteryLegend()
                }
            }
        }
        concept?.let { id ->
            item { SectionTitle(stringResource(Res.string.section_most_missed, term(id))) }
            val missed = dash.mostMissed(teamId, id)
            if (missed.isEmpty()) item { Text(stringResource(Res.string.nothing_missed), style = Gridiron.type.body, color = c.inkDim) }
            items(missed, key = { "m-" + it.step.id }) { MissedCard(it) }
        }
        item { SectionTitle(stringResource(Res.string.section_roster)) }
        items(rows.sortedBy { it.readinessPct }, key = { "r-" + it.learner.id }) { r ->
            RosterRow(r, state.today) { state.open(CoachScreen.Learner(r.learner.id)) }
        }
        item {
            SecondaryButton(stringResource(Res.string.assign_to, team.name), onClick = {
                state.startAssign(packIds = listOf(pack.id), learnerIds = dash.snapshot.membersOf(teamId).map { it.id })
                state.edit { it.copy(teamId = teamId) }
            })
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Gridiron.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = Gridiron.type.label, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = c.gold, selectedLabelColor = c.onGold, labelColor = c.ink,
        ),
    )
}

/**
 * Learners down, concepts across: the state glyph on a tint of its colour, or the score
 * as a number in Table mode (DESIGN 14.3; never colour alone, SPEC 10).
 */
@Composable
private fun HeatMap(
    rows: List<HeatRow>,
    concepts: List<String>,
    term: (String) -> String,
    table: Boolean,
    selected: String?,
    onConcept: (String) -> Unit,
) {
    val c = Gridiron.colors
    Row {
        Column {
            Box(Modifier.height(HEADER_H))
            rows.forEach { r ->
                Box(Modifier.height(CELL).width(NAME_W), contentAlignment = Alignment.CenterStart) {
                    Text(r.learner.nickname, style = Gridiron.type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            concepts.forEach { id ->
                val name = term(id)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.height(HEADER_H).width(CELL_W)
                            .background(if (id == selected) c.gold.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.show_most_missed)) { onConcept(id) }
                            .padding(2.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(name, style = Gridiron.type.label, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                    rows.forEach { r ->
                        val m = r.cells[concepts.indexOf(id)]
                        val word = stringResource(masteryWord(m.state))
                        Box(
                            Modifier.height(CELL).width(CELL_W).padding(2.dp)
                                .background(masteryColor(m.state).copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                .clearAndSetSemantics { contentDescription = "${r.learner.nickname}, $name: $word, ${m.score}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (table) Text(if (m.attempts == 0) "–" else "${m.score}", style = Gridiron.type.label, color = c.ink)
                            else MasteryGlyph(m.state, size = 20.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissedCard(item: MissedItem) {
    val c = Gridiron.colors
    val prompt = when (val s = item.step) {
        is MultipleChoice -> s.prompt
        is TrueFalse -> s.statement
        is ReadDefense -> s.prompt
        is Situation -> s.prompt
        is DiagramTap -> s.prompt
        is Match -> s.prompt
        is Order -> s.prompt
        is Learn -> s.title
    }
    Card(edge = c.retry) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prompt, style = Gridiron.type.body, color = c.ink)
            Text(stringResource(Res.string.missed_rate, (item.missRate * 100).toInt(), item.tries), style = Gridiron.type.label, color = c.retry)
            item.commonWrongAnswer?.let {
                Text(stringResource(Res.string.common_wrong_answer, if (it == "true" || it == "false") stringResource(if (it == "true") Res.string.answer_true else Res.string.answer_false) else it),
                    style = Gridiron.type.label, color = c.inkDim)
            }
        }
    }
}

@Composable
private fun RosterRow(r: HeatRow, today: Long, onClick: () -> Unit) {
    val c = Gridiron.colors
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Monogram(r.learner.nickname, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(r.learner.nickname + if (r.learner.positions.isNotEmpty()) "  ·  " + r.learner.positions.joinToString("/") else "",
                    style = Gridiron.type.answer, color = c.ink)
                Text(learnerSubline(r.learner, today), style = Gridiron.type.label, color = c.inkDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${r.readinessPct}%", style = Gridiron.type.scoreboard, color = c.ink)
                Text(stringResource(Res.string.readiness), style = Gridiron.type.label, color = c.inkDim)
            }
        }
    }
}

private val NAME_W = 76.dp
private val CELL = 34.dp
private val CELL_W = 64.dp
private val HEADER_H = 56.dp
