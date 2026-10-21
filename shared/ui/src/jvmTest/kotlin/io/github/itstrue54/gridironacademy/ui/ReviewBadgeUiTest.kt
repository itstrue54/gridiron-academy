package io.github.itstrue54.gridironacademy.ui

import androidx.compose.ui.test.*
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.model.ReviewLog
import io.github.itstrue54.gridironacademy.model.Signoff
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import kotlin.test.Test

/** DESIGN 8, ReviewBadge: debug builds mark unsigned steps; signed ones and release builds don't. */
@OptIn(ExperimentalTestApi::class)
class ReviewBadgeUiTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    private fun firstLesson(c: Content, badges: Boolean) = LearnerAppState(c, InMemoryLearnerStore(), showReviewBadges = badges) { 800 }.also {
        it.createProfile("Leo", AgeBand.AGE_8_9)
        val (level, lesson) = it.nextLesson()!!
        it.startLesson(level, lesson)
    }

    @Test fun anUnsignedStepWearsTheBadgeInDebug() = runComposeUiTest {
        val state = firstLesson(content, badges = true)
        setContent { LearnerApp(state, dark = false) }
        onNodeWithText("[review] k1-field-01").assertExists()
    }

    @Test fun releaseBuildsNeverShowIt() = runComposeUiTest {
        val state = firstLesson(content, badges = false)
        setContent { LearnerApp(state, dark = false) }
        onAllNodesWithText("[review]", substring = true).assertCountEquals(0)
    }

    @Test fun aSignedStepDoesNot() = runComposeUiTest {
        val log = ReviewLog(signoffs = listOf(Signoff("k1-field-01", "Coach", "2026-10-21", 2026, "0123456789abcdef")))
        val signed = Content(content.curriculum, content.glossary, content.unitsByLevel, content.scenes, content.packs, log)
        val state = firstLesson(signed, badges = true)
        setContent { LearnerApp(state, dark = false) }
        onAllNodesWithText("[review]", substring = true).assertCountEquals(0)
    }
}
