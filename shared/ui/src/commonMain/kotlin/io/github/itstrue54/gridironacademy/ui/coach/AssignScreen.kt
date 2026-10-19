package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.data.LinkedLearner
import io.github.itstrue54.gridironacademy.model.PackSide
import io.github.itstrue54.gridironacademy.model.SchemePack
import io.github.itstrue54.gridironacademy.progress.AssignmentRules
import io.github.itstrue54.gridironacademy.progress.NoteProblem
import io.github.itstrue54.gridironacademy.progress.NoteRules
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Assign (DESIGN 14.4): What -> Who -> Options -> Preview, one primary per step. */
@Composable
fun AssignScreen(state: CoachAppState, dash: CoachDashboard) {
    val c = Gridiron.colors
    val d = state.draft
    Column(Modifier.fillMaxSize().padding(horizontal = Gridiron.skin.gutter)) {
        Text(stringResource(Res.string.assign_title), style = Gridiron.type.headline, color = c.ink,
            modifier = Modifier.padding(top = Gridiron.skin.gutter).semantics { heading() })
        StepIndicator(d.step)
        Box(Modifier.weight(1f)) {
            when (d.step) {
                AssignStep.WHAT -> WhatStep(state)
                AssignStep.WHO -> WhoStep(state, dash)
                AssignStep.OPTIONS -> OptionsStep(state)
                AssignStep.PREVIEW -> PreviewStep(state, dash)
            }
        }
        Column(Modifier.padding(vertical = 12.dp)) {
            if (d.step == AssignStep.PREVIEW) {
                PrimaryButton(stringResource(Res.string.send), onClick = { state.send(dash) }, enabled = !state.sending)
            } else {
                PrimaryButton(stringResource(Res.string.next), onClick = state::next, enabled = d.canContinue)
            }
        }
    }
}

@Composable
private fun StepIndicator(step: AssignStep) {
    val c = Gridiron.colors
    val names = listOf(Res.string.step_what, Res.string.step_who, Res.string.step_options, Res.string.step_preview)
    val label = stringResource(Res.string.assign_step, step.ordinal + 1, names.size, stringResource(names[step.ordinal]))
    Row(
        // The bar and labels are drawn; screen readers hear one line: "Step 2 of 4: Who".
        Modifier.fillMaxWidth().padding(vertical = 10.dp).clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        names.forEachIndexed { i, r ->
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(4.dp).padding(end = 2.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawRect(if (i <= step.ordinal) c.gold else c.line) }
                }
                Text(stringResource(r), style = Gridiron.type.label, color = if (i == step.ordinal) c.ink else c.inkDim)
            }
        }
    }
}

@Composable
private fun WhatStep(state: CoachAppState) {
    val c = Gridiron.colors
    val d = state.draft
    val packs = state.content.packs.filter { d.side == null || it.side == d.side }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SideChip(stringResource(Res.string.side_all), d.side == null) { state.edit { it.copy(side = null) } } }
                items(PackSide.entries, key = { it.name }) { s ->
                    SideChip(stringResource(sideName(s)), d.side == s) { state.edit { it.copy(side = s) } }
                }
            }
        }
        items(packs, key = { it.id }) { p ->
            val on = p.id in d.packIds
            PackCard(p, state.content.conceptsOf(p).size, on) {
                state.edit { it.copy(packIds = if (on) it.packIds - p.id else it.packIds + p.id) }
            }
        }
    }
}

@Composable
private fun PackCard(p: SchemePack, concepts: Int, selected: Boolean, onToggle: () -> Unit) {
    val c = Gridiron.colors
    Card(
        edge = if (selected) c.gold else null,
        modifier = Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.title, style = Gridiron.type.answer, color = c.ink)
                Text(p.blurb, style = Gridiron.type.label, color = c.inkDim)
                Text(stringResource(Res.string.pack_meta, stringResource(sideName(p.side)), p.level, p.lessons.size, concepts),
                    style = Gridiron.type.label, color = c.inkDim)
            }
            Checkbox(checked = selected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = c.gold, checkmarkColor = c.onGold))
        }
    }
}

@Composable
private fun WhoStep(state: CoachAppState, dash: CoachDashboard) {
    val c = Gridiron.colors
    val d = state.draft
    val snap = dash.snapshot
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        snap.teams.forEach { t ->
            val ids = snap.membersOf(t.id).map { it.id }
            val all = ids.isNotEmpty() && d.learnerIds.containsAll(ids)
            item(key = "team-" + t.id) {
                CheckRow(stringResource(Res.string.whole_team, t.name), pluralStringResource(Res.plurals.players_count, ids.size, ids.size), all) {
                    state.edit { it.copy(learnerIds = if (all) it.learnerIds - ids.toSet() else (it.learnerIds + ids).distinct(), teamId = if (all) null else t.id) }
                }
            }
        }
        item { SectionTitle(stringResource(Res.string.individuals)) }
        items(snap.learners, key = { it.id }) { l ->
            val on = l.id in d.learnerIds
            CheckRow(l.nickname, learnerSubline(l, state.today), on, monogram = l.nickname) {
                state.edit { it.copy(learnerIds = if (on) it.learnerIds - l.id else it.learnerIds + l.id) }
            }
        }
    }
}

@Composable
private fun CheckRow(title: String, sub: String, checked: Boolean, monogram: String? = null, onToggle: () -> Unit) {
    val c = Gridiron.colors
    Card(modifier = Modifier.toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (monogram != null) Monogram(monogram, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = Gridiron.type.answer, color = c.ink)
                Text(sub, style = Gridiron.type.label, color = c.inkDim)
            }
            Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = c.gold, checkmarkColor = c.onGold))
        }
    }
}

@Composable
private fun OptionsStep(state: CoachAppState) {
    val c = Gridiron.colors
    val d = state.draft
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle(stringResource(Res.string.due)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DUE_CHOICES) { days ->
                    SideChip(
                        if (days == null) stringResource(Res.string.due_none) else dueText(state.today + days, state.today),
                        d.dueInDays == days,
                    ) { state.edit { it.copy(dueInDays = days) } }
                }
            }
        }
        item { SectionTitle(stringResource(Res.string.check_score, d.passPct)) }
        item {
            Slider(
                value = d.passPct.toFloat(),
                onValueChange = { v -> state.edit { it.copy(passPct = (v / 5).toInt() * 5) } },
                valueRange = MIN_PASS.toFloat()..100f,
                steps = (100 - MIN_PASS) / 5 - 1,
                colors = SliderDefaults.colors(thumbColor = c.gold, activeTrackColor = c.gold),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().toggleable(d.includeWarmups, role = Role.Switch, onValueChange = { v -> state.edit { it.copy(includeWarmups = v) } }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.include_warmups), style = Gridiron.type.answer, color = c.ink)
                    Text(stringResource(Res.string.include_warmups_body), style = Gridiron.type.label, color = c.inkDim)
                }
                Switch(checked = d.includeWarmups, onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.gold, checkedThumbColor = c.onGold))
            }
        }
        item { SectionTitle(stringResource(Res.string.note)) }
        item {
            val problems = NoteRules.problems(d.note)
            OutlinedTextField(
                value = d.note,
                onValueChange = { v -> state.edit { it.copy(note = v.take(AssignmentRules.NOTE_MAX_CHARS + 20)) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.note_hint), style = Gridiron.type.body) },
                supportingText = {
                    Column {
                        Text(stringResource(Res.string.note_count, d.note.length, AssignmentRules.NOTE_MAX_CHARS),
                            color = if (NoteProblem.TOO_LONG in problems) c.retry else c.inkDim)
                        if (NoteProblem.LINK in problems) Text(stringResource(Res.string.note_no_links), color = c.retry)
                        if (NoteProblem.CONTACT in problems) Text(stringResource(Res.string.note_no_contact), color = c.retry)
                    }
                },
                isError = problems.isNotEmpty(),
                textStyle = Gridiron.type.body.copy(color = c.ink),
                minLines = 2,
            )
        }
    }
}

@Composable
private fun PreviewStep(state: CoachAppState, dash: CoachDashboard) {
    val c = Gridiron.colors
    val d = state.draft
    val packs = d.packIds.mapNotNull { state.content.pack(it) }
    val plans = dash.preview(packs, d.learnerIds, d.includeWarmups)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(packs.joinToString(" + ") { it.title }, style = Gridiron.type.answer, color = c.ink)
                    Text(stringResource(Res.string.preview_meta, dueText(d.dueInDays?.let { state.today + it }, state.today), d.passPct),
                        style = Gridiron.type.label, color = c.inkDim)
                    if (d.note.isNotBlank()) Text("“${d.note.trim()}”", style = Gridiron.type.label, color = c.inkDim)
                }
            }
        }
        items(plans, key = { it.first.id }) { (l, plan) -> PlanRow(l, plan) }
    }
}

@Composable
private fun PlanRow(l: LinkedLearner, plan: io.github.itstrue54.gridironacademy.progress.AssignmentPlan) {
    val c = Gridiron.colors
    val warn = plan.wellAboveLevel
    Card(edge = if (warn) c.retry else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Monogram(l.nickname, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (plan.warmups > 0) pluralStringResource(Res.plurals.plan_line_warmups, plan.warmups, l.nickname, plan.warmups, plan.lessons - plan.warmups, plan.minutes)
                    else stringResource(Res.string.plan_line, l.nickname, plan.lessons, plan.minutes),
                    style = Gridiron.type.answer, color = c.ink,
                )
                if (plan.warmupsLeftOut > 0) {
                    Text(pluralStringResource(Res.plurals.warmups_left_out, plan.warmupsLeftOut, plan.warmupsLeftOut), style = Gridiron.type.label, color = c.inkDim)
                }
                if (warn) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GlyphIcon(Glyph.DASH, c.retry, size = 14.dp)
                        Text(stringResource(Res.string.plan_well_above, l.level), style = Gridiron.type.label, color = c.retry)
                    }
                }
            }
        }
    }
}

@Composable
private fun SideChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Gridiron.colors
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(text, style = Gridiron.type.label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.gold, selectedLabelColor = c.onGold, labelColor = c.ink),
    )
}

private fun sideName(s: PackSide) = when (s) {
    PackSide.OFFENSE -> Res.string.side_offense
    PackSide.DEFENSE -> Res.string.side_defense
    PackSide.SPECIAL -> Res.string.side_special
    PackSide.RULES -> Res.string.side_rules
}

private val DUE_CHOICES = listOf(3, 7, 14, null)
private const val MIN_PASS = 60
