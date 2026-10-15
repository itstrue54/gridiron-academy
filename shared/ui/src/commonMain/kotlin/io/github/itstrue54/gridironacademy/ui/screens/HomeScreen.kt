package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import io.github.itstrue54.gridironacademy.ui.resources.xp_total
import io.github.itstrue54.gridironacademy.ui.resources.season_progress
import io.github.itstrue54.gridironacademy.ui.components.glass
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.model.Level
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.NextUp
import io.github.itstrue54.gridironacademy.ui.resources.big_game
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.all_done_adult
import io.github.itstrue54.gridironacademy.ui.resources.all_done_kid
import io.github.itstrue54.gridironacademy.ui.resources.continue_lesson
import io.github.itstrue54.gridironacademy.ui.resources.home_title
import io.github.itstrue54.gridironacademy.ui.resources.level_n
import io.github.itstrue54.gridironacademy.ui.resources.level_progress
import io.github.itstrue54.gridironacademy.ui.resources.level_state_locked
import io.github.itstrue54.gridironacademy.ui.resources.level_state_open
import io.github.itstrue54.gridironacademy.ui.resources.level_state_passed
import io.github.itstrue54.gridironacademy.ui.resources.streak_days
import io.github.itstrue54.gridironacademy.ui.resources.streak_start_adult
import io.github.itstrue54.gridironacademy.ui.resources.streak_start_kid
import io.github.itstrue54.gridironacademy.ui.theme.DiagramColors
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import org.jetbrains.compose.resources.stringResource

/**
 * Home: the drive map (DESIGN 9.1). Levels are nodes on a strip of turf, the highest at
 * the top, so progress reads as driving up the field. The one primary is Continue.
 */
@Composable
fun HomeScreen(state: LearnerAppState) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    val kid = skin.voice == Voice.KID
    val next = state.nextUp()
    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding().padding(skin.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.profile?.nickname.orEmpty(), style = Gridiron.type.title, color = c.ink, modifier = Modifier.weight(1f))
            StreakChip(state.save.progress.streakDays, kid)
        }
        Spacer(Modifier.size(12.dp))
        if (skin.primeTime) {
            SeasonHeader(state.save.progress.xp)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.levels.forEach { SeasonCard(state, it) }
            }
        } else {
            Text(stringResource(Res.string.home_title), style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                DriveMap(state)
            }
        }
        Spacer(Modifier.size(16.dp))
        if (next != null) {
            val title = when (next) {
                is NextUp.Drill -> next.lesson.title
                is NextUp.Checkpoint -> stringResource(Res.string.checkpoint)
                is NextUp.BigGame -> stringResource(Res.string.big_game)
            }
            PrimaryButton(stringResource(Res.string.continue_lesson, title), onClick = {
                when (next) {
                    is NextUp.Drill -> state.startLesson(next.level, next.lesson)
                    is NextUp.Checkpoint -> { state.openLevel(next.level); state.startCheckpoint(next.level) }
                    is NextUp.BigGame -> { state.openLevel(next.level); state.startBigGame(next.level) }
                }
            })
        } else {
            Text(
                stringResource(if (kid) Res.string.all_done_kid else Res.string.all_done_adult),
                style = Gridiron.type.body,
                color = c.inkDim,
            )
        }
    }
}

@Composable
private fun StreakChip(days: Int, kid: Boolean) {
    val c = Gridiron.colors
    val label = if (days > 0) stringResource(Res.string.streak_days, days)
    else stringResource(if (kid) Res.string.streak_start_kid else Res.string.streak_start_adult)
    Surface(shape = Gridiron.skin.chipShape, color = c.surfaceRaised, contentColor = c.ink, border = BorderStroke(1.dp, c.line)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(Glyph.FLAME, if (days > 0) c.gold else c.line, size = 20.dp)
            Spacer(Modifier.width(6.dp))
            Text(label, style = Gridiron.type.label)
        }
    }
}

/** The levels on turf, top to bottom from the highest, with the goal line at the bottom. */
@Composable
private fun DriveMap(state: LearnerAppState) {
    val skin = Gridiron.skin
    Box(
        Modifier
            .fillMaxWidth()
            .clip(skin.cardShape)
            .background(DiagramColors.turfB),
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Five-yard stripes and chalk lines, as on the field diagrams.
            val band = 56.dp.toPx()
            var y = 0f
            var k = 0
            while (y < size.height) {
                if (k % 2 == 0) drawRect(DiagramColors.turfA, Offset(0f, y), Size(size.width, band))
                drawLine(DiagramColors.chalk.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += band; k++
            }
            drawLine(DiagramColors.chalk, Offset(0f, size.height - 2.dp.toPx()), Offset(size.width, size.height - 2.dp.toPx()), 3.dp.toPx())
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            state.levels.sortedByDescending { it.number }.forEach { level -> LevelRow(state, level) }
        }
    }
}

private enum class NodeState { LOCKED, OPEN, PASSED }

@Composable
private fun LevelRow(state: LearnerAppState, level: Level) {
    val c = Gridiron.colors
    val unlocked = state.levelUnlocked(level)
    val passedCount = state.lessonsPassed(level)
    val total = state.lessonCount(level)
    val nodeState = when {
        level.id in state.save.progress.levelsPassed -> NodeState.PASSED
        unlocked -> NodeState.OPEN
        else -> NodeState.LOCKED
    }
    val stateWord = stringResource(
        when (nodeState) {
            NodeState.LOCKED -> Res.string.level_state_locked
            NodeState.OPEN -> Res.string.level_state_open
            NodeState.PASSED -> Res.string.level_state_passed
        },
    )
    val levelLabel = stringResource(Res.string.level_n, level.number)
    val progressLabel = stringResource(Res.string.level_progress, passedCount, total)
    Surface(
        onClick = { state.openLevel(level) },
        enabled = unlocked,
        shape = Gridiron.skin.cardShape,
        color = c.surfaceRaised.copy(alpha = 0.94f),
        contentColor = c.ink,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "$levelLabel, ${level.title}. $stateWord. $progressLabel"
        },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelNode(level.number, nodeState, if (total == 0) 0f else passedCount.toFloat() / total)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(levelLabel, style = Gridiron.type.label, color = c.inkDim)
                Text(level.title, style = Gridiron.type.title, color = c.ink)
                Text(level.tagline, style = Gridiron.type.label, color = c.inkDim)
                Spacer(Modifier.size(4.dp))
                Text(progressLabel, style = Gridiron.type.label, color = c.ink)
            }
        }
    }
}

/** DESIGN 8: locked, open (gold ring, progress arc), passed (solid gold with a check). */
@Composable
private fun LevelNode(number: Int, state: NodeState, progress: Float) {
    val c = Gridiron.colors
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            when (state) {
                NodeState.PASSED -> drawCircle(c.gold)
                NodeState.LOCKED -> drawCircle(c.line)
                NodeState.OPEN -> {
                    drawCircle(c.surface)
                    drawArc(c.line, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    drawArc(c.gold, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        when (state) {
            NodeState.PASSED -> GlyphIcon(Glyph.CHECK, c.onGold, size = 28.dp)
            NodeState.LOCKED -> GlyphIcon(Glyph.LOCK, c.inkDim, size = 24.dp)
            NodeState.OPEN -> Text(number.toString(), style = Gridiron.type.headline, color = c.ink)
        }
    }
}

// ---- Prime Time (DESIGN 2.2): the season ladder ----

/** "SEASON PROGRESS" between two rules, and the learner's XP in cyan. */
@Composable
private fun SeasonHeader(xp: Int) {
    val c = Gridiron.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(c.gold.copy(alpha = 0.5f)))
        Text(
            stringResource(Res.string.season_progress).uppercase(),
            style = Gridiron.type.eyebrow,
            color = c.gold,
            modifier = Modifier.padding(horizontal = 12.dp).semantics { heading() },
        )
        Box(Modifier.weight(1f).height(1.dp).background(c.gold.copy(alpha = 0.5f)))
    }
    Spacer(Modifier.size(8.dp))
    Text(stringResource(Res.string.xp_total, xp), style = Gridiron.type.scoreboard, color = c.xp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

/**
 * A level as a season card: a shield rank badge, the level name, and an XP-style bar of
 * drills passed. Passed is volt with a check; the open level is magenta; locked is grey.
 */
@Composable
private fun SeasonCard(state: LearnerAppState, level: Level) {
    val c = Gridiron.colors
    val unlocked = state.levelUnlocked(level)
    val passed = level.id in state.save.progress.levelsPassed
    val done = state.lessonsPassed(level)
    val total = state.lessonCount(level)
    val nodeState = when {
        passed -> NodeState.PASSED
        unlocked -> NodeState.OPEN
        else -> NodeState.LOCKED
    }
    val stateWord = stringResource(
        when (nodeState) {
            NodeState.LOCKED -> Res.string.level_state_locked
            NodeState.OPEN -> Res.string.level_state_open
            NodeState.PASSED -> Res.string.level_state_passed
        },
    )
    val levelLabel = stringResource(Res.string.level_n, level.number)
    val progressLabel = stringResource(Res.string.level_progress, done, total)
    val accent = when (nodeState) { NodeState.PASSED -> c.gold; NodeState.OPEN -> c.rank; NodeState.LOCKED -> c.line }
    Surface(
        onClick = { state.openLevel(level) },
        enabled = unlocked,
        shape = Gridiron.skin.cardShape,
        color = glass(),
        contentColor = c.ink,
        border = BorderStroke(if (nodeState == NodeState.OPEN) 1.5.dp else 1.dp, if (nodeState == NodeState.OPEN) c.rank else c.line),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = "$levelLabel, ${level.title}. $stateWord. $progressLabel"
        },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ShieldBadge(level.number, nodeState, accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(levelLabel.uppercase(), style = Gridiron.type.eyebrow, color = c.inkDim)
                Text(level.title.uppercase(), style = Gridiron.type.headline, color = if (unlocked) c.ink else c.inkDim)
                Spacer(Modifier.size(8.dp))
                XpBar(if (total == 0) 0f else done.toFloat() / total, if (passed) c.gold else c.xp)
                Spacer(Modifier.size(4.dp))
                Text(progressLabel, style = Gridiron.type.label, color = c.inkDim)
            }
        }
    }
}

/** Season Mode's rank badge: a shield with the level number. */
@Composable
private fun ShieldBadge(number: Int, state: NodeState, accent: Color) {
    val c = Gridiron.colors
    Box(Modifier.size(width = 56.dp, height = 64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val shield = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w, h * 0.14f)
                lineTo(w, h * 0.52f)
                cubicTo(w, h * 0.80f, w * 0.72f, h * 0.92f, w * 0.5f, h)
                cubicTo(w * 0.28f, h * 0.92f, 0f, h * 0.80f, 0f, h * 0.52f)
                lineTo(0f, h * 0.14f)
                close()
            }
            drawPath(shield, accent.copy(alpha = if (state == NodeState.LOCKED) 0.35f else 0.22f))
            drawPath(shield, accent, style = Stroke(2.5.dp.toPx()))
        }
        when (state) {
            NodeState.PASSED -> GlyphIcon(Glyph.CHECK, c.gold, size = 26.dp)
            NodeState.LOCKED -> GlyphIcon(Glyph.LOCK, c.inkDim, size = 22.dp)
            NodeState.OPEN -> Text("$number", style = Gridiron.type.headline, color = c.ink)
        }
    }
}

/** A rounded progress bar on a dim track (DESIGN 2.2). */
@Composable
private fun XpBar(fraction: Float, color: Color) {
    val c = Gridiron.colors
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
    }
}
