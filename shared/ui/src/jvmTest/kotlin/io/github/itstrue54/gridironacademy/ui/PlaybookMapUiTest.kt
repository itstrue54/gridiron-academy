package io.github.itstrue54.gridironacademy.ui

import androidx.compose.ui.test.*
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.model.Glossary
import io.github.itstrue54.gridironacademy.model.GlossaryEntry
import io.github.itstrue54.gridironacademy.model.PackSide
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Playbook map driven as a learner would, and as a TalkBack user would. */
@OptIn(ExperimentalTestApi::class)
class PlaybookMapUiTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    private fun map() = LearnerAppState(content, InMemoryLearnerStore()) { 800 }.also {
        it.createProfile("Sam", AgeBand.AGE_18_PLUS)
        it.openGlossary()
    }

    @Test
    fun theGlossaryOpensTheMapAndANodeOpensItsCard() = runComposeUiTest {
        val state = map()
        setContent { LearnerApp(state, dark = false) }
        onNodeWithContentDescription("Playbook map").performClick()
        runOnIdle { assertEquals(Screen.PlaybookMap, state.screen) }

        onNodeWithContentDescription("First down, New").performClick()
        onNodeWithText("Learn first").assertExists()
        onNodeWithText("Down and distance").assertExists()
        onNodeWithText("Quiz me").performScrollTo().performClick()
        runOnIdle { assertEquals(Screen.GlossaryQuizPlay("first-down"), state.screen) }
    }

    @Test
    fun aPrerequisiteOnTheOtherSideIsOneTapAway() = runComposeUiTest {
        val state = map().also { it.openPlaybookMap() }
        setContent { LearnerApp(state, dark = false) }
        onNode(hasSetTextAction()).performTextInput("mesh")
        onNodeWithContentDescription("Mesh, New").performClick()
        // Mesh is an offense concept that beats man coverage, a defense one.
        onNodeWithText("From Defense").assertExists()
        onNodeWithText("Man coverage").performScrollTo().performClick()
        onNodeWithContentDescription("Cover 0, New").assertExists()
    }

    @Test
    fun aConceptThisLadderNeverTeachesIsMarkedNotCounted() = runComposeUiTest {
        // Shipped content tags every term on both ladders (GlossaryQuizTest), so add one no step teaches.
        val extra = GlossaryEntry("wildcat", "Wildcat", "A trick set.", "A direct snap to a running back.", level = 1, side = PackSide.OFFENSE)
        val withExtra = Content(content.curriculum, Glossary(content.glossary.entries + extra), content.unitsByLevel, content.scenes, content.packs)
        val state = LearnerAppState(withExtra, InMemoryLearnerStore()) { 800 }.also {
            it.createProfile("Sam", AgeBand.AGE_18_PLUS); it.openGlossary(); it.openPlaybookMap()
        }
        setContent { LearnerApp(state, dark = false) }
        onNodeWithContentDescription("Wildcat, Not on your course yet").assertExists()
        val offense = content.glossary.entries.count { it.side == PackSide.OFFENSE }
        onNodeWithText("0 of $offense mastered").assertExists()
    }

    @Test
    fun listModeReadsTheTreeAsRows() = runComposeUiTest {
        val state = map().also { it.openPlaybookMap() }
        setContent { LearnerApp(state, dark = false) }
        onNodeWithText("List").performClick()
        onNode(hasSetTextAction()).performTextInput("cover 3")
        // Not on Offense: the search moves to the tab that has it.
        onNodeWithText("Learn first: Zone coverage, Safeties").assertExists()
    }
}
