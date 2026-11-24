package io.github.itstrue54.gridironacademy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.LessonController
import io.github.itstrue54.gridironacademy.ui.games.SpeedRoundScreen
import io.github.itstrue54.gridironacademy.ui.lesson.LocalScreenReader
import io.github.itstrue54.gridironacademy.ui.lesson.MatchView
import io.github.itstrue54.gridironacademy.ui.lesson.StepPlayer
import io.github.itstrue54.gridironacademy.ui.theme.GridironTheme
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import kotlin.test.Test
import kotlin.test.assertTrue

/** What a TalkBack user hears and where focus goes on the step types (DESIGN 12). */
@OptIn(ExperimentalTestApi::class)
class StepViewsUiTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    private fun words(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.StateDescription).orEmpty()
    private fun hasWords(part: String) = SemanticsMatcher("state description has \"$part\"") { part in words(it) }
    private fun button(text: String) = hasText(text, substring = true) and hasClickAction()

    /** Taps by action, not by pixel: a tall diagram can leave the button below the test window. */
    private fun ComposeUiTest.tap(text: String) = onNode(button(text)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)

    private val diagram = Diagram(players = listOf(
        DiagramPlayer("a", "Zed", Side.OFFENSE, -3f, 0f),
        DiagramPlayer("b", "Yan", Side.OFFENSE, 0f, 0f),
        DiagramPlayer("c", "Xia", Side.OFFENSE, 3f, 0f),
    ))
    private val match = Match("m1", "Match them", listOf(MatchPair("Punt", "Kick away"), MatchPair("Sack", "Tackle the QB")), "Because.")

    private fun ComposeUiTest.play(step: Step, screenReader: Boolean = false): LessonController {
        val controller = LessonController(listOf(step))
        setContent {
            CompositionLocalProvider(LocalScreenReader provides { screenReader }) {
                GridironTheme(skin = Skin.Adult, dark = false) { StepPlayer(controller, "Close", {}, {}) }
            }
        }
        return controller
    }

    @Test fun listModeMissedTargetIsNotCorrect() = runComposeUiTest {
        val c = play(DiagramTap("t1", "Tap two", diagram, setOf("a", "b"), "Because."), screenReader = true)
        tap("Zed")
        onNode(button("Zed")).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        tap("Xia") // a decoy
        onNodeWithText("Check", ignoreCase = true).performClick()
        onNode(button("Zed")).assert(hasWords("correct"))
        onNode(button("Xia")).assert(hasWords("not quite"))
        // Yan was an answer nobody tapped: it must not read as "correct".
        onNode(button("Yan")).assert(hasWords("missed"))
        onNode(button("Yan")).assert(!hasWords("correct"))
        runOnIdle { assertTrue(c.grade != null) }
    }

    @Test fun matchPairedItemsSayPaired() = runComposeUiTest {
        play(match)
        onNode(button("Kick away")).performClick() // pairs with the first left, Punt
        onNode(button("Kick away")).assert(hasWords("paired with Punt"))
        onNode(button("Kick away")).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        onNode(button("Punt")).assert(hasWords("paired with Kick away"))
    }

    @Test fun matchRightFirstShowsHint() = runComposeUiTest {
        setContent {
            GridironTheme(skin = Skin.Adult, dark = false) {
                MatchView(match, listOf("Kick away", "Tackle the QB"), emptyMap(), selectedLeft = null, graded = false, onSelectLeft = {}, onPairs = {})
            }
        }
        onNodeWithText("Pick one on the left first, then its partner.").assertDoesNotExist()
        onNode(button("Kick away")).performClick()
        onNodeWithText("Pick one on the left first, then its partner.").assertExists()
    }

    @Test fun matchHintNamesTheLayoutItIsIn() = runComposeUiTest {
        fun show(fontScale: Float) = setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, fontScale)) {
                GridironTheme(skin = Skin.Adult, dark = false) {
                    MatchView(match, listOf("Kick away", "Tackle the QB"), emptyMap(), null, false, {}, {})
                }
            }
        }
        show(2f)
        onNodeWithText("Tap one in the first list, then its partner in the second.").assertExists()
    }

    @Test fun matchHintSaysLeftAndRightWhenSideBySide() = runComposeUiTest {
        setContent {
            GridironTheme(skin = Skin.Adult, dark = false) { MatchView(match, listOf("Kick away", "Tackle the QB"), emptyMap(), null, false, {}, {}) }
        }
        onNodeWithText("Tap one on the left, then its partner on the right.").assertExists()
    }

    @Test fun continueGetsFocusAfterCheck() = runComposeUiTest {
        val mc = MultipleChoice("q1", "Pick", listOf("One", "Two"), 0, "Because.")
        play(mc)
        onNode(button("One")).performClick()
        onNodeWithText("Check", ignoreCase = true).performClick()
        onNodeWithText("Continue", ignoreCase = true).assertIsFocused()
    }

    @Test fun orderAnnouncesTheNewPosition() = runComposeUiTest {
        play(Order("o1", "Order", listOf("First", "Second", "Third"), "Because."))
        // Whatever the shuffle, the first item that can move down says where it landed, in a live region.
        onAllNodes(hasContentDescription("down", substring = true) and hasClickAction() and isEnabled())[0].performClick()
        onNode(hasText("of 3", substring = true) and SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertExists()
    }

    private fun appState() = LearnerAppState(content, InMemoryLearnerStore()) { 800 }.also {
        it.createProfile("Sam", AgeBand.AGE_18_PLUS)
        it.openGlossary()
    }

    @Test fun searchFieldHasLabel() = runComposeUiTest {
        val state = appState()
        setContent { LearnerApp(state, dark = false) }
        // A placeholder vanishes when you type; a label stays for the screen reader.
        onNode(hasSetTextAction()).performTextInput("mesh")
        onNode(hasSetTextAction()).assert(SemanticsMatcher("labelled") { n ->
            n.config.getOrNull(SemanticsProperties.Text)?.any { "Search a word" in it.text } == true ||
                n.config.getOrNull(SemanticsProperties.ContentDescription)?.any { "Search a word" in it } == true
        })
    }

    @Test fun speedRoundShowsVerdictText() = runComposeUiTest {
        val state = LearnerAppState(content, InMemoryLearnerStore()) { 500 }.also { it.createProfile("Leo", AgeBand.AGE_8_9) }
        val level = state.levels.first()
        state.startBigGame(level)
        val round = state.speedRoundController(level.id)
        round.start(timed = false)
        setContent { GridironTheme(skin = state.skin, dark = false) { SpeedRoundScreen(state, level.id, round, clock = false) } }
        val q = round.current!!
        val wrong = when (q) { is MultipleChoice -> Answer.Choice((q.answer + 1) % q.choices.size); is TrueFalse -> Answer.Bool(!q.answer); else -> error("unexpected") }
        runOnIdle { round.answer(wrong) }
        onNodeWithText("Last answer: not quite").assertExists()
    }

    @Test fun aLongExplanationTellsTalkBackThereIsMore() = runComposeUiTest {
        val long = "The safety is the last line of the defense. ".repeat(30)
        setContent {
            GridironTheme(skin = Skin.Adult, dark = false) {
                io.github.itstrue54.gridironacademy.ui.components.FeedbackSheet(
                    io.github.itstrue54.gridironacademy.ui.components.Verdict.NOT_QUITE, "Not quite", long, "Continue", {},
                    maxHeight = androidx.compose.ui.unit.Dp(200f))
            }
        }
        onNode(hasWords("more text below")).assertExists()
        onNode(hasWords("more text below")).assert(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.CustomActions))
    }
}
