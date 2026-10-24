package io.github.itstrue54.gridironacademy.ui.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.itstrue54.gridironacademy.ui.components.GoldSweep
import io.github.itstrue54.gridironacademy.ui.components.Celebration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.components.SecondaryButton
import io.github.itstrue54.gridironacademy.ui.components.StarRow
import io.github.itstrue54.gridironacademy.ui.games.SpeedRoundScreen
import io.github.itstrue54.gridironacademy.ui.lesson.CheckpointScreen
import io.github.itstrue54.gridironacademy.ui.lesson.LessonScreen
import io.github.itstrue54.gridironacademy.ui.lesson.PracticeScreen
import io.github.itstrue54.gridironacademy.ui.lesson.StepGameScreen
import io.github.itstrue54.gridironacademy.ui.screens.GateDoneScreen
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.back_to_level
import io.github.itstrue54.gridironacademy.ui.resources.done_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_not_passed_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_not_passed_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_passed_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_passed_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_review
import io.github.itstrue54.gridironacademy.ui.resources.done_score
import io.github.itstrue54.gridironacademy.ui.resources.done_try_again_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_try_again_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_passed_checkpoint_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_passed_checkpoint_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_try_again_checkpoint_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_try_again_checkpoint_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_passed_again_kid
import io.github.itstrue54.gridironacademy.ui.resources.done_passed_again_adult
import io.github.itstrue54.gridironacademy.ui.resources.done_best_still_counts
import io.github.itstrue54.gridironacademy.ui.resources.done_xp
import io.github.itstrue54.gridironacademy.ui.resources.stars_label
import io.github.itstrue54.gridironacademy.ui.resources.try_again
import io.github.itstrue54.gridironacademy.engine.LessonSession
import io.github.itstrue54.gridironacademy.ui.screens.HomeScreen
import io.github.itstrue54.gridironacademy.ui.screens.LevelScreen
import io.github.itstrue54.gridironacademy.ui.screens.OnboardingScreen
import io.github.itstrue54.gridironacademy.ui.screens.WelcomeScreen
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import io.github.itstrue54.gridironacademy.ui.theme.GridironTheme
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import org.jetbrains.compose.resources.stringResource
import io.github.itstrue54.gridironacademy.ui.resources.practice_done_kid
import io.github.itstrue54.gridironacademy.ui.resources.practice_done_adult
import io.github.itstrue54.gridironacademy.ui.resources.practice_done_body
import io.github.itstrue54.gridironacademy.ui.resources.back_home
import io.github.itstrue54.gridironacademy.ui.resources.back_to_glossary
import io.github.itstrue54.gridironacademy.ui.lesson.GlossaryQuizScreen
import io.github.itstrue54.gridironacademy.ui.screens.GlossaryScreen
import io.github.itstrue54.gridironacademy.ui.lesson.LocalReviewBadge
import io.github.itstrue54.gridironacademy.ui.lesson.LocalScreenReader
import io.github.itstrue54.gridironacademy.ui.screens.PlaybookMapScreen
import io.github.itstrue54.gridironacademy.ui.screens.GrownUpGateScreen
import io.github.itstrue54.gridironacademy.ui.screens.GrownUpZoneScreen
import androidx.compose.runtime.remember
import io.github.itstrue54.gridironacademy.ui.resources.leave_title
import io.github.itstrue54.gridironacademy.ui.resources.leave_body
import io.github.itstrue54.gridironacademy.ui.resources.leave_gate_title
import io.github.itstrue54.gridironacademy.ui.resources.leave_gate_body
import io.github.itstrue54.gridironacademy.ui.resources.keep_going
import io.github.itstrue54.gridironacademy.ui.resources.leave

/**
 * The learner app, shared by every platform. The shell supplies the content, the store,
 * the clock and the theme choice; back handling calls [LearnerAppState.back].
 */
@Composable
fun LearnerApp(state: LearnerAppState, dark: Boolean, reducedMotion: Boolean = false, platform: LearnerPlatform = NoPlatform) {
    // Before a profile exists there is no track; onboarding uses the Adult skin.
    val skin = if (state.profile == null) Skin.Adult else state.skin
    val badge: (String) -> Boolean = remember(state) {
        if (state.showReviewBadges) { id -> !state.content.signedOff(id) } else { _ -> false }
    }
    val screenReader: () -> Boolean = remember(state) { { state.screenReaderActive() } }
    val sound = remember(platform) { SoundSetting(platform) }
    val haptics = LocalHapticFeedback.current
    val soundOn = sound.on(skin)
    val cues = remember(haptics, soundOn, platform) {
        Cues { cue ->
            haptics.performHapticFeedback(cue.haptic)
            if (soundOn) platform.playSound(cue)
        }
    }
    CompositionLocalProvider(
        LocalReviewBadge provides badge, LocalScreenReader provides screenReader,
        LocalCues provides cues, LocalSoundSetting provides sound,
    ) {
    GridironTheme(skin = skin, dark = dark, reducedMotion = reducedMotion) {
        // One readable column, centred: a landscape phone or a tablet gets margins of
        // surface colour instead of screens stretched edge to edge.
        Box(Modifier.fillMaxSize().screenBackground(), contentAlignment = Alignment.TopCenter) {
        AnimatedContent(
            targetState = state.screen,
            modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxSize(),
            transitionSpec = {
                val ms = if (reducedMotion) CROSSFADE_MS else (STANDARD_MS * skin.motionScale).toInt()
                fadeIn(tween(ms)) togetherWith fadeOut(tween(ms))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Welcome -> WelcomeScreen(state.content.scenes.welcome, onStart = state::startOnboarding)
                Screen.Onboarding -> OnboardingScreen(onDone = state::createProfile)
                Screen.Home -> HomeScreen(state)
                is Screen.LevelDetail -> LevelScreen(state, state.content.level(screen.levelId))
                is Screen.LessonPlay -> LessonScreen(state, screen.levelId, screen.lessonId)
                is Screen.LessonDone -> LessonDoneScreen(state, screen)
                is Screen.CheckpointPlay -> CheckpointScreen(state, screen.levelId, screen.testOut)
                is Screen.BigGamePlay ->
                    if (state.bigGameIsSpeedRound(screen.levelId)) SpeedRoundScreen(state, screen.levelId)
                    else StepGameScreen(state, screen.levelId)
                is Screen.GateDone -> GateDoneScreen(state, screen)
                Screen.PracticePlay -> PracticeScreen(state)
                is Screen.PracticeDone -> PracticeDoneScreen(state, screen)
                Screen.Glossary -> GlossaryScreen(state)
                Screen.PlaybookMap -> PlaybookMapScreen(state)
                // For the grown-up whatever the profile: the Coach app's look (DESIGN 9.9).
                Screen.GrownUpGate -> GridironTheme(skin = Skin.Coach, dark = dark, reducedMotion = reducedMotion) {
                    GrownUpGateScreen(state, seed = remember { state.clockMillis })
                }
                Screen.GrownUpZone -> GridironTheme(skin = Skin.Coach, dark = dark, reducedMotion = reducedMotion) {
                    GrownUpZoneScreen(state, platform)
                }
                is Screen.GlossaryQuizPlay -> GlossaryQuizScreen(state, screen.concept)
                is Screen.QuizDone -> QuizDoneScreen(state, screen)
            }
        }
        }
        LeaveDialog(state)
    }
    }
}

/**
 * Leaving a player part-way (DESIGN 9.3): once something's been answered, ✕ and Back ask
 * first, and say what's lost. Before the first answer they just leave.
 */
@Composable
private fun LeaveDialog(state: LearnerAppState) {
    if (!state.confirmingLeave) return
    val gate = state.screen is Screen.CheckpointPlay || state.screen is Screen.BigGamePlay
    androidx.compose.material3.AlertDialog(
        onDismissRequest = state::keepGoing,
        title = { Text(stringResource(if (gate) Res.string.leave_gate_title else Res.string.leave_title)) },
        text = { Text(stringResource(if (gate) Res.string.leave_gate_body else Res.string.leave_body)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = state::keepGoing) { Text(stringResource(Res.string.keep_going)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = state::confirmLeave) { Text(stringResource(Res.string.leave)) } },
    )
}

private const val STANDARD_MS = 220
private val MAX_CONTENT_WIDTH = 600.dp
private const val CROSSFADE_MS = 150

/** Practice done: the score, the XP, and back home. Kind about misses: they come back. */
@Composable
fun PracticeDoneScreen(state: LearnerAppState, done: Screen.PracticeDone) {
    val c = Gridiron.colors
    val kid = Gridiron.skin.voice == Voice.KID
    Column(
        Modifier.fillMaxSize().screenBackground().safeDrawingPadding().padding(Gridiron.skin.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(if (kid) Res.string.practice_done_kid else Res.string.practice_done_adult),
            style = Gridiron.type.displayLarge,
            color = if (Gridiron.skin.primeTime) c.gold else c.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(Res.string.done_score, done.scorePct), style = Gridiron.type.title, color = c.ink)
        if (done.xpGained > 0) Text(stringResource(Res.string.done_xp, done.xpGained), style = Gridiron.type.answer, color = c.xp)
        Text(stringResource(Res.string.practice_done_body), style = Gridiron.type.body, color = c.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        PrimaryButton(stringResource(Res.string.back_home), onClick = { state.back() })
    }
}

/** Quiz me finished: the score on that term, then back to the glossary. */
@Composable
fun QuizDoneScreen(state: LearnerAppState, done: Screen.QuizDone) {
    val c = Gridiron.colors
    val term = state.content.glossary.entries.firstOrNull { it.id == done.concept }?.term ?: done.concept
    Column(
        Modifier.fillMaxSize().screenBackground().safeDrawingPadding().padding(Gridiron.skin.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(term, style = Gridiron.type.displayLarge, color = if (Gridiron.skin.primeTime) c.gold else c.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.done_score, done.scorePct), style = Gridiron.type.title, color = c.ink)
        Text(stringResource(Res.string.practice_done_body), style = Gridiron.type.body, color = c.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        PrimaryButton(stringResource(Res.string.back_to_glossary), onClick = { state.back() })
    }
}

/** Lesson complete (DESIGN 9.7): the verdict, stars, score and XP, then back to the level. */
@Composable
fun LessonDoneScreen(state: LearnerAppState, done: Screen.LessonDone) {
    val c = Gridiron.colors
    val kid = Gridiron.skin.voice == Voice.KID
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().screenBackground().safeDrawingPadding().padding(Gridiron.skin.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(
                when {
                    done.passed -> if (kid) Res.string.done_kid else Res.string.done_adult
                    else -> if (kid) Res.string.done_not_passed_kid else Res.string.done_not_passed_adult
                },
            ),
            style = Gridiron.type.displayLarge,
            color = if (Gridiron.skin.primeTime) c.gold else c.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (done.passed) GoldSweep(Modifier.widthIn(max = 220.dp))
        StarRow(done.stars, stringResource(Res.string.stars_label, done.stars), size = 44.dp)
        Text(stringResource(Res.string.done_score, done.scorePct), style = Gridiron.type.title, color = c.ink)
        Text(stringResource(Res.string.done_xp, done.xpGained), style = Gridiron.type.answer, color = c.xp)
        val pass = LessonSession.LESSON_PASS_PCT
        Text(
            when (done.opens) {
                Opens.NOTHING ->
                    if (done.passed) stringResource(if (kid) Res.string.done_passed_again_kid else Res.string.done_passed_again_adult)
                    else stringResource(Res.string.done_best_still_counts)
                Opens.CHECKPOINT ->
                    if (done.passed) stringResource(if (kid) Res.string.done_passed_checkpoint_kid else Res.string.done_passed_checkpoint_adult)
                    else stringResource(if (kid) Res.string.done_try_again_checkpoint_kid else Res.string.done_try_again_checkpoint_adult, pass)
                Opens.NEXT_DRILL ->
                    if (done.passed) stringResource(if (kid) Res.string.done_passed_kid else Res.string.done_passed_adult)
                    else stringResource(if (kid) Res.string.done_try_again_kid else Res.string.done_try_again_adult, pass)
            },
            style = Gridiron.type.body,
            color = c.ink,
        )
        if (done.missedCount > 0) Text(stringResource(Res.string.done_review), style = Gridiron.type.label, color = c.inkDim)
        Spacer(Modifier.size(8.dp))
        PrimaryButton(stringResource(Res.string.back_to_level), onClick = { state.back() })
        if (!done.passed) {
            SecondaryButton(stringResource(Res.string.try_again), onClick = {
                state.back()
                state.startLesson(state.content.level(done.levelId), state.lesson(done.levelId, done.lessonId))
            })
        }
    }
    if (done.passed) Celebration()
    }
}
