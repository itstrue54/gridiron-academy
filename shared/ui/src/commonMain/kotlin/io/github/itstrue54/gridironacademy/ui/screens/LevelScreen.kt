package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.model.Level
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.components.StarRow
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.back
import io.github.itstrue54.gridironacademy.ui.resources.big_game
import io.github.itstrue54.gridironacademy.ui.resources.big_game_blurb
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint_blurb
import io.github.itstrue54.gridironacademy.ui.resources.coming_soon
import io.github.itstrue54.gridironacademy.ui.resources.drill_locked
import io.github.itstrue54.gridironacademy.ui.resources.level_n
import io.github.itstrue54.gridironacademy.ui.resources.level_progress
import io.github.itstrue54.gridironacademy.ui.resources.stars_label
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.stringResource

/**
 * A level (DESIGN 9.2): its units as sections of drill cards, in order, then the
 * Checkpoint and the Big Game. Locked cards say what opens them.
 */
@Composable
fun LevelScreen(state: LearnerAppState, level: Level) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    val units = state.content.unitsOf(level)
    val lessons = units.flatMap { it.lessons }
    Column(Modifier.fillMaxSize().background(c.surface).safeDrawingPadding()) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(stringResource(Res.string.level_n, level.number), style = Gridiron.type.label, color = c.inkDim)
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = skin.gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(level.title, style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
                    Text(level.tagline, style = Gridiron.type.body, color = c.inkDim)
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(Res.string.level_progress, state.lessonsPassed(level), lessons.size), style = Gridiron.type.label, color = c.ink)
                }
            }
            units.forEach { unit ->
                item(key = unit.id) {
                    Text(unit.title, style = Gridiron.type.title, color = c.ink, modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                }
                unit.lessons.forEach { lesson ->
                    item(key = lesson.id) {
                        val i = lessons.indexOf(lesson)
                        val open = state.lessonUnlocked(level, lesson.id)
                        val result = state.save.progress.lessons[lesson.id]
                        val passed = state.save.progress.lessonPassed(lesson.id)
                        Card(
                            edge = if (passed) c.correct else if (open) c.gold else c.line,
                            onClick = if (open) ({ state.startLesson(level, lesson) }) else null,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lesson.title, style = Gridiron.type.title, color = if (open) c.ink else c.inkDim, modifier = Modifier.weight(1f))
                                    if (!open) GlyphIcon(Glyph.LOCK, c.inkDim, size = 20.dp)
                                }
                                if (lesson.blurb.isNotEmpty()) Text(lesson.blurb, style = Gridiron.type.label, color = c.inkDim)
                                Spacer(Modifier.size(6.dp))
                                when {
                                    !open && i > 0 -> Text(stringResource(Res.string.drill_locked, lessons[i - 1].title), style = Gridiron.type.label, color = c.inkDim)
                                    else -> StarRow(result?.stars ?: 0, stringResource(Res.string.stars_label, result?.stars ?: 0))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.size(8.dp))
                GateCard(stringResource(Res.string.checkpoint), stringResource(Res.string.checkpoint_blurb, level.gate.testQuestions, level.gate.testPassPct))
            }
            item {
                GateCard(stringResource(Res.string.big_game), stringResource(Res.string.big_game_blurb, level.gate.gamePassPct))
            }
        }
    }
}

/** The level test and level game: shown so the path is visible, opened in a later step of M1. */
@Composable
private fun GateCard(title: String, blurb: String) {
    val c = Gridiron.colors
    Card(edge = c.line) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Gridiron.type.title, color = c.inkDim, modifier = Modifier.weight(1f))
                GlyphIcon(Glyph.LOCK, c.inkDim, size = 20.dp)
            }
            Text(blurb, style = Gridiron.type.label, color = c.inkDim)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.coming_soon), style = Gridiron.type.label, color = c.ink)
        }
    }
}
