package io.github.itstrue54.gridironacademy.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import kotlin.test.Test
import kotlin.test.assertEquals

/** A tap on a locked level is heard, not only seen: the note is a live region, and a repeat tap keeps it. */
@OptIn(ExperimentalTestApi::class)
class LockedLevelNoteUiTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    private fun check(band: AgeBand) = runComposeUiTest {
        val state = LearnerAppState(content, InMemoryLearnerStore()) { 800 }.also { it.createProfile("Sam", band) }
        val locked = state.levels.first { it.number == 2 }
        setContent { LearnerApp(state, dark = false) }
        onAllNodesWithText("Finish Level", substring = true).assertCountEquals(0)
        assertEquals(0, reachableLiveRegions().size, "an empty note box is no TalkBack stop")

        repeat(2) { n ->
            state.tapLockedLevel(locked)
            waitForIdle()
            onNodeWithText("Finish Level 1 first.").assertExists("after tap ${n + 1}").assertIsDisplayed()
            val live = reachableLiveRegions()
            assertEquals(1, live.size, "only the tapped card's note is a TalkBack stop")
            assertEquals(true, live.single().config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text.startsWith("Finish Level") },
                "and it carries the note")
        }
    }

    /** Live regions TalkBack can land on: the note boxes not hidden from accessibility. */
    private fun ComposeUiTest.reachableLiveRegions() =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and
            SemanticsMatcher.keyNotDefined(SemanticsProperties.HideFromAccessibility))
            .fetchSemanticsNodes()
            .filter { it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text.startsWith("Finish") } != false }

    @Test fun kidSkinExposesTheNoteAfterEachTap() = check(AgeBand.AGE_8_9)
    @Test fun primeTimeExposesTheNoteAfterEachTap() = check(AgeBand.AGE_18_PLUS)
}
