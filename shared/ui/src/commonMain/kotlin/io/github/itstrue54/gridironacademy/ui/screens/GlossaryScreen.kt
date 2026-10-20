package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.model.GlossaryEntry
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import org.jetbrains.compose.resources.stringResource

/**
 * The glossary (DESIGN 9.10): every term, alphabetical or by level, searchable. Each shows
 * the plain wording first, the full definition on request, where it's taught and a Quiz me.
 */
@Composable
fun GlossaryScreen(state: LearnerAppState) {
    val c = Gridiron.colors
    var query by rememberSaveable { mutableStateOf("") }
    var byLevel by rememberSaveable { mutableStateOf(false) }
    val all = state.content.glossary.entries
    val shown = remember(query, byLevel) {
        val q = query.trim().lowercase()
        all.filter { q.isEmpty() || q in it.term.lowercase() || q in it.kid.lowercase() || q in it.full.lowercase() }
            .sortedWith(if (byLevel) compareBy({ it.level }, { it.term.lowercase() }) else compareBy { it.term.lowercase() })
    }
    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(stringResource(Res.string.glossary), style = Gridiron.type.headline, color = c.ink,
                modifier = Modifier.weight(1f).semantics { heading() })
            IconAction(Glyph.PLAYBOOK, stringResource(Res.string.playbook_map), onClick = state::openPlaybookMap)
        }
        Column(Modifier.padding(horizontal = Gridiron.skin.gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.glossary_search), style = Gridiron.type.body) },
                singleLine = true, textStyle = Gridiron.type.body.copy(color = c.ink),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip(stringResource(Res.string.glossary_a_z), !byLevel) { byLevel = false }
                SortChip(stringResource(Res.string.glossary_by_level), byLevel) { byLevel = true }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Gridiron.skin.gutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (shown.isEmpty()) item { Text(stringResource(Res.string.glossary_none, query), style = Gridiron.type.body, color = c.inkDim) }
            var lastLevel = -1
            shown.forEach { e ->
                if (byLevel && e.level != lastLevel) {
                    lastLevel = e.level
                    item(key = "level-${e.level}") {
                        Text(stringResource(Res.string.level_n, e.level).uppercase(), style = Gridiron.type.eyebrow, color = c.inkDim,
                            modifier = Modifier.padding(top = 6.dp).semantics { heading() })
                    }
                }
                item(key = e.id) { TermCard(state, e) }
            }
        }
    }
}

@Composable
internal fun TermCard(state: LearnerAppState, e: GlossaryEntry) {
    val c = Gridiron.colors
    val kid = Gridiron.skin.voice == Voice.KID
    var full by rememberSaveable(e.id) { mutableStateOf(false) }
    val learned = remember(e.id, state.save) { state.learnedIn(e.id) }
    val canQuiz = remember(e.id, state.save) { state.quizFor(e.id).isNotEmpty() }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(e.term, style = Gridiron.type.title, color = c.ink)
            Text(if (kid || !full) e.kid else e.full, style = Gridiron.type.body, color = c.ink)
            if (kid && full) Text(e.full, style = Gridiron.type.body, color = c.inkDim)
            if (e.full != e.kid) {
                TextButton(onClick = { full = !full }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(if (full) Res.string.glossary_less else Res.string.glossary_full), style = Gridiron.type.label, color = c.link)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (learned != null) {
                    val (lv, lesson) = learned
                    TextButton(onClick = { state.openLevel(lv) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(Res.string.glossary_learned, lv.number, lesson.title), style = Gridiron.type.label, color = c.link,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                } else Spacer(Modifier.weight(1f))
                if (canQuiz) {
                    TextButton(onClick = { state.startQuiz(e.id) }) {
                        Text(stringResource(Res.string.glossary_quiz), style = Gridiron.type.label, color = c.link)
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Gridiron.colors
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(text, style = Gridiron.type.label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.gold, selectedLabelColor = c.onGold, labelColor = c.ink),
    )
}
