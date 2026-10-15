package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.ui.app.Gate
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.ReviewItem
import io.github.itstrue54.gridironacademy.ui.app.Screen
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.components.SecondaryButton
import io.github.itstrue54.gridironacademy.ui.components.StarRow
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.answer_false
import io.github.itstrue54.gridironacademy.ui.resources.answer_true
import io.github.itstrue54.gridironacademy.ui.resources.back_to_level
import io.github.itstrue54.gridironacademy.ui.resources.big_game
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint_not_yet_adult
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint_not_yet_kid
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint_passed_adult
import io.github.itstrue54.gridironacademy.ui.resources.checkpoint_passed_kid
import io.github.itstrue54.gridironacademy.ui.resources.game_not_yet_adult
import io.github.itstrue54.gridironacademy.ui.resources.game_not_yet_kid
import io.github.itstrue54.gridironacademy.ui.resources.game_won_adult
import io.github.itstrue54.gridironacademy.ui.resources.game_won_kid
import io.github.itstrue54.gridironacademy.ui.resources.level_done_adult
import io.github.itstrue54.gridironacademy.ui.resources.level_done_body_adult
import io.github.itstrue54.gridironacademy.ui.resources.level_done_body_kid
import io.github.itstrue54.gridironacademy.ui.resources.level_done_kid
import io.github.itstrue54.gridironacademy.ui.resources.level_done_next_adult
import io.github.itstrue54.gridironacademy.ui.resources.level_done_next_kid
import io.github.itstrue54.gridironacademy.ui.resources.play_again
import io.github.itstrue54.gridironacademy.ui.resources.play_big_game
import io.github.itstrue54.gridironacademy.ui.resources.review_answer
import io.github.itstrue54.gridironacademy.ui.resources.review_title
import io.github.itstrue54.gridironacademy.ui.resources.speed_summary
import io.github.itstrue54.gridironacademy.ui.resources.stars_label
import io.github.itstrue54.gridironacademy.ui.resources.testout_passed_adult
import io.github.itstrue54.gridironacademy.ui.resources.testout_passed_kid
import io.github.itstrue54.gridironacademy.ui.resources.try_again
import io.github.itstrue54.gridironacademy.ui.resources.your_score
import io.github.itstrue54.gridironacademy.engine.LessonSession
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import org.jetbrains.compose.resources.stringResource

/**
 * After a Checkpoint or Big Game (DESIGN 9.7): the score, what it means, what to look at
 * again, and one primary for what comes next. When this result passed the level, it is
 * the level-complete moment.
 */
@Composable
fun GateDoneScreen(state: LearnerAppState, done: Screen.GateDone) {
    val c = Gridiron.colors
    val kid = Gridiron.skin.voice == Voice.KID
    val level = state.content.level(done.levelId)
    val next = state.levels.firstOrNull { it.number == level.number + 1 }
    val headline = when {
        done.levelPassed -> stringResource(if (kid) Res.string.level_done_kid else Res.string.level_done_adult, level.number)
        done.gate == Gate.CHECKPOINT -> stringResource(Res.string.checkpoint)
        else -> stringResource(Res.string.big_game)
    }
    val message = when {
        done.levelPassed && next != null ->
            stringResource(if (kid) Res.string.level_done_next_kid else Res.string.level_done_next_adult, level.title, next.number, next.title)
        done.levelPassed -> stringResource(if (kid) Res.string.level_done_body_kid else Res.string.level_done_body_adult, level.title)
        done.gate == Gate.CHECKPOINT && done.passed && done.testOut ->
            stringResource(if (kid) Res.string.testout_passed_kid else Res.string.testout_passed_adult)
        done.gate == Gate.CHECKPOINT && done.passed -> stringResource(if (kid) Res.string.checkpoint_passed_kid else Res.string.checkpoint_passed_adult)
        done.gate == Gate.CHECKPOINT -> stringResource(if (kid) Res.string.checkpoint_not_yet_kid else Res.string.checkpoint_not_yet_adult, done.passPct)
        done.passed -> stringResource(if (kid) Res.string.game_won_kid else Res.string.game_won_adult)
        else -> stringResource(if (kid) Res.string.game_not_yet_kid else Res.string.game_not_yet_adult, done.passPct)
    }
    val stars = LessonSession.stars(done.scorePct)
    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding().padding(Gridiron.skin.gutter)) {
        Column(
            // Full width, or "centred" means centred on the widest line, not the screen.
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.size(24.dp))
            Text(headline, style = Gridiron.type.displayLarge, color = if (Gridiron.skin.primeTime) c.gold else c.ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            StarRow(stars, stringResource(Res.string.stars_label, stars), size = 44.dp)
            Text(stringResource(Res.string.your_score, done.scorePct), style = Gridiron.type.scoreboard, color = c.ink)
            if (done.points > 0 || done.bestStreak > 0) {
                Text(stringResource(Res.string.speed_summary, done.points, done.bestStreak), style = Gridiron.type.label, color = c.inkDim)
            }
            Text(message, style = Gridiron.type.body, color = c.ink, textAlign = TextAlign.Center)
            if (done.review.isNotEmpty()) {
                Text(
                    stringResource(Res.string.review_title),
                    style = Gridiron.type.title, color = c.ink,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { heading() },
                )
                done.review.forEach { ReviewCard(it) }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                done.gate == Gate.CHECKPOINT && done.passed && !done.levelPassed -> {
                    PrimaryButton(stringResource(Res.string.play_big_game), onClick = { state.back(); state.startBigGame(level) })
                    SecondaryButton(stringResource(Res.string.back_to_level), onClick = { state.back() })
                }
                !done.passed -> {
                    PrimaryButton(stringResource(Res.string.back_to_level), onClick = { state.back() })
                    SecondaryButton(
                        stringResource(if (done.gate == Gate.CHECKPOINT) Res.string.try_again else Res.string.play_again),
                        onClick = {
                            state.back()
                            if (done.gate == Gate.CHECKPOINT) state.startCheckpoint(level, done.testOut) else state.startBigGame(level)
                        },
                    )
                }
                else -> PrimaryButton(stringResource(Res.string.back_to_level), onClick = { state.back() })
            }
        }
    }
}

@Composable
private fun ReviewCard(item: ReviewItem) {
    val c = Gridiron.colors
    val answer = item.answerText ?: item.answerTrue?.let { stringResource(if (it) Res.string.answer_true else Res.string.answer_false) }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (item.question.isNotEmpty()) Text(item.question, style = Gridiron.type.body, color = c.ink)
            if (answer != null) Text(stringResource(Res.string.review_answer, answer), style = Gridiron.type.answer, color = c.correct)
            Text(item.why, style = Gridiron.type.label, color = c.inkDim)
        }
    }
}
