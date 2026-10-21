package io.github.itstrue54.gridironacademy.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.engine.Grade
import io.github.itstrue54.gridironacademy.engine.LessonSession
import io.github.itstrue54.gridironacademy.model.DiagramTap
import io.github.itstrue54.gridironacademy.model.Learn
import io.github.itstrue54.gridironacademy.model.Match
import io.github.itstrue54.gridironacademy.model.MultipleChoice
import io.github.itstrue54.gridironacademy.model.Order
import io.github.itstrue54.gridironacademy.model.Quality
import io.github.itstrue54.gridironacademy.model.ReadDefense
import io.github.itstrue54.gridironacademy.model.Situation
import io.github.itstrue54.gridironacademy.model.Step
import io.github.itstrue54.gridironacademy.model.TrueFalse
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.LessonController
import io.github.itstrue54.gridironacademy.ui.components.DriveBar
import io.github.itstrue54.gridironacademy.ui.components.FeedbackSheet
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.components.Verdict
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.best_adult
import io.github.itstrue54.gridironacademy.ui.resources.best_kid
import io.github.itstrue54.gridironacademy.ui.resources.best_was
import io.github.itstrue54.gridironacademy.ui.resources.check
import io.github.itstrue54.gridironacademy.ui.resources.close_checkpoint
import io.github.itstrue54.gridironacademy.ui.resources.close_game
import io.github.itstrue54.gridironacademy.ui.resources.close_lesson
import io.github.itstrue54.gridironacademy.ui.resources.continue_
import io.github.itstrue54.gridironacademy.ui.resources.next
import io.github.itstrue54.gridironacademy.ui.resources.not_quite_adult
import io.github.itstrue54.gridironacademy.ui.resources.not_quite_again_adult
import io.github.itstrue54.gridironacademy.ui.resources.not_quite_again_kid
import io.github.itstrue54.gridironacademy.ui.resources.not_quite_kid
import io.github.itstrue54.gridironacademy.ui.resources.ok_adult
import io.github.itstrue54.gridironacademy.ui.resources.ok_kid
import io.github.itstrue54.gridironacademy.ui.resources.partial_adult
import io.github.itstrue54.gridironacademy.ui.resources.partial_kid
import io.github.itstrue54.gridironacademy.ui.resources.poor_adult
import io.github.itstrue54.gridironacademy.ui.resources.poor_kid
import io.github.itstrue54.gridironacademy.ui.resources.right_adult_1
import io.github.itstrue54.gridironacademy.ui.resources.right_adult_2
import io.github.itstrue54.gridironacademy.ui.resources.right_kid_1
import io.github.itstrue54.gridironacademy.ui.resources.right_kid_2
import io.github.itstrue54.gridironacademy.ui.resources.right_kid_3
import io.github.itstrue54.gridironacademy.ui.resources.review_badge
import io.github.itstrue54.gridironacademy.ui.resources.step_of
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import io.github.itstrue54.gridironacademy.ui.theme.Voice
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import io.github.itstrue54.gridironacademy.ui.resources.close_practice
import io.github.itstrue54.gridironacademy.ui.resources.close_quiz

/**
 * The drill player (DESIGN 9.3): DriveBar on top, one step, one primary at the bottom.
 * After Check the feedback sheet rises with the why; a first-try miss comes back once
 * at the end. The controller comes from [state] so it outlives a rebuilt screen; a test
 * can pass its own to show any step.
 */
@Composable
fun LessonScreen(
    state: LearnerAppState,
    levelId: String,
    lessonId: String,
    controller: LessonController? = state.lessonControllerOrNull(levelId, lessonId),
) {
    if (controller == null) return // a quit lesson fading out
    StepPlayer(
        controller = controller,
        closeLabel = stringResource(Res.string.close_lesson),
        onClose = { state.requestBack() },
        onFinished = { state.finishLesson(levelId, lessonId, it) },
    )
}

/** The level test (DESIGN 9.2): the same player, each question asked once. */
@Composable
fun CheckpointScreen(state: LearnerAppState, levelId: String, testOut: Boolean) {
    StepPlayer(
        controller = state.checkpointControllerOrNull(levelId, testOut) ?: return,
        closeLabel = stringResource(Res.string.close_checkpoint),
        onClose = { state.requestBack() },
        onFinished = { state.finishCheckpoint(levelId, testOut, it) },
    )
}

/** Today's practice (SPEC 6.4): the same player, each question asked once. */
@Composable
fun PracticeScreen(state: LearnerAppState) {
    StepPlayer(
        controller = state.practiceControllerOrNull() ?: return,
        closeLabel = stringResource(Res.string.close_practice),
        onClose = { state.requestBack() },
        onFinished = { state.finishPractice(it) },
    )
}

/** A glossary term's Quiz me (DESIGN 9.10): a short drill from that term's questions. */
@Composable
fun GlossaryQuizScreen(state: LearnerAppState, concept: String) {
    StepPlayer(
        controller = state.quizControllerOrNull(concept) ?: return,
        closeLabel = stringResource(Res.string.close_quiz),
        onClose = { state.requestBack() },
        onFinished = { state.finishQuiz(concept, it) },
    )
}

/** A level game that isn't a Speed round (Game day, Read the defense, Diagram dash). */
@Composable
fun StepGameScreen(state: LearnerAppState, levelId: String) {
    StepPlayer(
        controller = state.gameControllerOrNull(levelId) ?: return,
        closeLabel = stringResource(Res.string.close_game),
        onClose = { state.requestBack() },
        onFinished = { state.finishBigGame(levelId, it) },
    )
}

/**
 * Plays any list of steps: lessons, the Checkpoint and step-based games. [onFinished]
 * runs once the last step is done; the caller records the result.
 */
@Composable
fun StepPlayer(
    controller: LessonController,
    closeLabel: String,
    onClose: () -> Unit,
    onFinished: (LessonSession) -> Unit,
) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    LaunchedEffect(controller.finished) {
        if (controller.finished) onFinished(controller.result)
    }
    val step = controller.step ?: return
    val stepKey = "${step.id}#${controller.done}"
    val pending = controller.draft

    BoxWithConstraints(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        val sheetMax = maxHeight * SHEET_MAX_FRACTION
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 4.dp, end = skin.gutter), verticalAlignment = Alignment.CenterVertically) {
                IconAction(Glyph.CLOSE, closeLabel, onClick = onClose)
                DriveBar(controller.done, controller.total, stringResource(Res.string.step_of, controller.done + 1, controller.total), Modifier.weight(1f))
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = skin.gutter, vertical = 12.dp),
            ) {
                if (LocalReviewBadge.current(step.id)) ReviewBadge(step.id)
                key(stepKey) {
                    StepBody(step, controller, pending, controller.grade != null, controller.answer) { controller.draft = it }
                }
                // Room so the feedback sheet never covers the end of the step.
                if (controller.grade != null) Spacer(Modifier.height(sheetMax))
            }
            if (controller.grade == null) {
                Box(Modifier.padding(skin.gutter)) {
                    if (step is Learn) {
                        PrimaryButton(stringResource(Res.string.next), onClick = { controller.submit(Answer.Seen) })
                    } else {
                        PrimaryButton(stringResource(Res.string.check), onClick = { pending?.let(controller::submit) }, enabled = pending != null)
                    }
                }
            }
        }
        controller.grade?.let { grade ->
            val fb = feedbackFor(step, grade, controller.answer, controller.isRetry, controller.done)
            FeedbackSheet(
                verdict = fb.verdict,
                headline = stringResource(fb.headline),
                detail = fb.detail,
                body = fb.body,
                continueLabel = stringResource(Res.string.continue_),
                onContinue = { controller.next() },
                maxHeight = sheetMax,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private const val SHEET_MAX_FRACTION = 0.45f

/** True for a step that should wear the ReviewBadge; provided by LearnerApp in debug builds. */
val LocalReviewBadge = staticCompositionLocalOf<(String) -> Boolean> { { false } }

/** DESIGN 8, ReviewBadge: debug builds only, "[review]" and the id to quote back. */
@Composable
private fun ReviewBadge(stepId: String) {
    Text(stringResource(Res.string.review_badge, stepId), style = Gridiron.type.label, color = Gridiron.colors.retry,
        modifier = Modifier.padding(bottom = 6.dp))
}

/** Shows [step]; [submitted] is the checked answer once graded, else [pending] drives it. */
@Composable
private fun StepBody(step: Step, controller: LessonController, pending: Answer?, graded: Boolean, submitted: Answer?, onAnswer: (Answer?) -> Unit) {
    val shown = if (graded) submitted else pending
    when (step) {
        is Learn -> LearnView(step)
        is MultipleChoice -> MultipleChoiceView(
            step.id, step.prompt, step.diagram, step.choices, step.answer,
            selected = (shown as? Answer.Choice)?.index, graded = graded,
        ) { onAnswer(Answer.Choice(it)) }
        is TrueFalse -> TrueFalseView(step.statement, step.answer, (shown as? Answer.Bool)?.value, graded) { onAnswer(Answer.Bool(it)) }
        is DiagramTap -> DiagramTapView(step, (shown as? Answer.Taps)?.ids.orEmpty(), graded) { ids ->
            onAnswer(if (ids.isEmpty()) null else Answer.Taps(ids))
        }
        is Match -> {
            val rightOrder = remember(step.id) { stepShuffle(step.id, step.pairs.map { it.right }) }
            var selectedLeft by remember(step.id) { mutableStateOf<String?>(step.pairs.first().left) }
            // Pairs in progress live on the controller; the answer exists once all are paired.
            val pairs = if (graded) (submitted as? Answer.Matches)?.pairs.orEmpty() else controller.matchDraft
            MatchView(step, rightOrder, pairs, selectedLeft, graded, onSelectLeft = { selectedLeft = it }) { next ->
                controller.matchDraft = next
                onAnswer(Answer.Matches(next).takeIf { next.size == step.pairs.size })
            }
        }
        is Order -> {
            val start = remember(step.id) { stepShuffle(step.id, step.items, avoid = step.items) }
            val order = (shown as? Answer.Ordering)?.items ?: start
            // An order step always has an answer: the current order.
            LaunchedEffect(step.id) { if (pending == null && !graded) onAnswer(Answer.Ordering(start)) }
            OrderView(step, order, graded) { onAnswer(Answer.Ordering(it)) }
        }
        is Situation -> SituationView(step, (shown as? Answer.Choice)?.index, graded) { onAnswer(Answer.Choice(it)) }
        is ReadDefense -> ReadDefenseView(step, (shown as? Answer.Choice)?.index, graded) { onAnswer(Answer.Choice(it)) }
    }
}

private class Feedback(val verdict: Verdict, val headline: StringResource, val detail: String, val body: String)

/** The verdict and its words (DESIGN 11): never "wrong"; gentler the second time. */
@Composable
private fun feedbackFor(step: Step, grade: Grade, answer: Answer?, isRetry: Boolean, done: Int): Feedback {
    val kid = Gridiron.skin.voice == Voice.KID
    if (step is Situation) {
        val chosen = (answer as? Answer.Choice)?.index?.let { step.options.getOrNull(it) }
        val best = step.options.first { it.quality == Quality.BEST }
        val (verdict, headline) = when (chosen?.quality) {
            Quality.BEST -> Verdict.RIGHT to if (kid) Res.string.best_kid else Res.string.best_adult
            Quality.OK -> Verdict.PARTIAL to if (kid) Res.string.ok_kid else Res.string.ok_adult
            else -> Verdict.NOT_QUITE to if (kid) Res.string.poor_kid else Res.string.poor_adult
        }
        val detail = listOfNotNull(chosen?.why, if (chosen != best) stringResource(Res.string.best_was, best.text) else null).joinToString("\n\n")
        return Feedback(verdict, headline, detail, step.explanation)
    }
    val (verdict, headline) = when {
        grade.correct -> Verdict.RIGHT to rightLine(kid, done)
        grade.credit > 0f -> Verdict.PARTIAL to if (kid) Res.string.partial_kid else Res.string.partial_adult
        isRetry -> Verdict.NOT_QUITE to if (kid) Res.string.not_quite_again_kid else Res.string.not_quite_again_adult
        else -> Verdict.NOT_QUITE to if (kid) Res.string.not_quite_kid else Res.string.not_quite_adult
    }
    return Feedback(verdict, headline, detail = "", body = grade.feedback)
}

private fun rightLine(kid: Boolean, done: Int): StringResource {
    val lines = if (kid) listOf(Res.string.right_kid_1, Res.string.right_kid_2, Res.string.right_kid_3)
    else listOf(Res.string.right_adult_1, Res.string.right_adult_2)
    return lines[done % lines.size]
}
