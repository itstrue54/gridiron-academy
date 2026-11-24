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

        repeat(2) { n ->
            state.tapLockedLevel(locked)
            waitForIdle()
            onNodeWithText("Finish Level 1 first.").assertExists("after tap ${n + 1}").assertIsDisplayed()
            val live = onAllNodes(SemanticsProperties.LiveRegion.let { key -> SemanticsMatcher.keyIsDefined(key) })
                .fetchSemanticsNodes().filter { it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text.startsWith("Finish Level") } == true }
            assertEquals(1, live.size, "exactly one live region carries the note")
        }
    }

    @Test fun kidSkinExposesTheNoteAfterEachTap() = check(AgeBand.AGE_8_9)
    @Test fun primeTimeExposesTheNoteAfterEachTap() = check(AgeBand.AGE_18_PLUS)
}
