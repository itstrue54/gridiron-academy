package io.github.itstrue54.gridironacademy.ui

import androidx.compose.ui.test.*
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.RestoreResult
import kotlin.test.Test
import kotlin.test.assertEquals

/** The grown-up zone driven as a parent would: typing, tapping, reading the screen. */
@OptIn(ExperimentalTestApi::class)
class GrownUpZoneUiTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    private fun zone(band: AgeBand = AgeBand.AGE_18_PLUS) =
        LearnerAppState(content, InMemoryLearnerStore()) { 800 }.also {
            it.createProfile("Sam", band)
            it.openGrownUpGate()
            it.enterGrownUpZone()
        }

    @Test
    fun theNameBoxFollowsARestore() = runComposeUiTest {
        val state = zone()
        val backup = state.backupText()
        setContent { LearnerApp(state, dark = false) }

        onNode(hasSetTextAction() and hasText("Sam")).performTextReplacement("Sam2")
        onNodeWithText("Save name", ignoreCase = true).performClick()
        runOnIdle { assertEquals("Sam2", state.profile!!.nickname) }

        runOnIdle { assertEquals(RestoreResult.Restored, state.restoreBackup(backup)) }
        // Found on the phone: the box kept "Sam2" and still offered Save name.
        onNode(hasSetTextAction() and hasText("Sam")).assertExists()
        onAllNodesWithText("Save name", ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun typingANameIsKeptUntilSaved() = runComposeUiTest {
        val state = zone()
        setContent { LearnerApp(state, dark = false) }
        onNode(hasSetTextAction() and hasText("Sam")).performTextReplacement("Samantha")
        onNodeWithText("Save name", ignoreCase = true).assertExists()
        runOnIdle { assertEquals("Sam", state.profile!!.nickname, "nothing changes until Save") }
    }
}

@OptIn(ExperimentalTestApi::class)
class AgeChangeUiTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    @Test
    fun anAgeOnTheOtherTrackAsksFirstAndSaysWhatChanges() = runComposeUiTest {
        val state = LearnerAppState(content, InMemoryLearnerStore()) { 800 }.also {
            it.createProfile("Sam", AgeBand.AGE_18_PLUS); it.openGrownUpGate(); it.enterGrownUpZone()
        }
        setContent { LearnerApp(state, dark = false) }

        onNodeWithText("8-9").performClick()
        onNodeWithText("Change Sam's lessons?").assertExists()
        onNodeWithText("Level 4 and up", substring = true).assertExists()
        onNodeWithText("Cancel").performClick()
        runOnIdle { assertEquals(AgeBand.AGE_18_PLUS, state.profile!!.ageBand, "cancel changes nothing") }

        onNodeWithText("8-9").performClick()
        onNodeWithText("Change lessons").performClick()
        runOnIdle { assertEquals(AgeBand.AGE_8_9, state.profile!!.ageBand) }

        // Same track: no question.
        onNodeWithText("10-12").performClick()
        onAllNodesWithText("Change Sam's lessons?").assertCountEquals(0)
        runOnIdle { assertEquals(AgeBand.AGE_10_12, state.profile!!.ageBand) }
    }
}
