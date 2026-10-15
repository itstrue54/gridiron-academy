package io.github.itstrue54.gridironacademy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.engine.Answer
import io.github.itstrue54.gridironacademy.model.*
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.InMemoryLearnerStore
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.LessonController
import io.github.itstrue54.gridironacademy.ui.lesson.LessonScreen
import io.github.itstrue54.gridironacademy.ui.theme.GridironTheme
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.*

/** Every learner screen rendered at phone size into build/screen-renders/ for review. */
class ScreenRenderTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val out = File("build/screen-renders").apply { mkdirs() }
    private val allSteps = content.curriculum.levels.flatMap { l -> content.unitsOf(l).flatMap { u -> u.lessons.map { l to it } } }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun shot(name: String, ui: @Composable () -> Unit) {
        val bytes = ImageComposeScene(width = 720, height = 1520, density = Density(2f)) { ui() }.use { scene ->
            scene.render(0)
            scene.render(1_000_000_000L) // let effects and transitions settle
            scene.render(2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
        }
        File(out, "$name.png").writeBytes(bytes)
        assertTrue(bytes.size > 10_000, "$name looks empty")
    }

    private fun appState(band: AgeBand?): LearnerAppState =
        LearnerAppState(content, InMemoryLearnerStore()) { 500 }.also { if (band != null) it.createProfile(if (band.child) "Leo" else "Sam", band) }

    private fun lessonShot(name: String, band: AgeBand, dark: Boolean, pick: (Step) -> Boolean, answer: ((Step) -> Answer)? = null) {
        val state = appState(band)
        // Prefer the profile's own track; fall back to the other if only it has this step type.
        val (level, lesson) = allSteps.firstOrNull { (l, les) -> l.track == band.track && les.steps.any(pick) }
            ?: allSteps.first { (_, les) -> les.steps.any(pick) }
        val step = lesson.steps.first(pick)
        val controller = LessonController(listOf(step))
        if (answer != null) controller.submit(answer(step))
        shot(name) {
            GridironTheme(skin = state.skin, dark = dark) {
                LessonScreen(state, level.id, lesson.id, controller)
            }
        }
    }

    @Test fun welcomeAndOnboarding() {
        shot("01-welcome-light") { LearnerApp(appState(null), dark = false) }
        val s = appState(null).also { it.startOnboarding() }
        shot("02-onboarding-dark") { LearnerApp(s, dark = true) }
    }

    @Test fun homeAndLevelBothSkins() {
        val kid = appState(AgeBand.AGE_8_9)
        shot("03-home-kid-light") { LearnerApp(kid, dark = false) }
        val adult = appState(AgeBand.AGE_18_PLUS)
        shot("04-home-adult-dark") { LearnerApp(adult, dark = true) }
        kid.openLevel(kid.levels.first())
        shot("05-level-kid-light") { LearnerApp(kid, dark = false) }
    }

    @Test fun lessonSteps() {
        lessonShot("06-learn-kid", AgeBand.AGE_8_9, dark = false, pick = { it is Learn })
        lessonShot("07-mc-missed-kid", AgeBand.AGE_8_9, dark = false, pick = { it is MultipleChoice }) {
            val mc = it as MultipleChoice; Answer.Choice((mc.answer + 1) % mc.choices.size)
        }
        lessonShot("08-tf-right-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it is TrueFalse }) { Answer.Bool((it as TrueFalse).answer) }
        lessonShot("09-tap-kid", AgeBand.AGE_8_9, dark = false, pick = { it is DiagramTap && it.diagram.players.size > 3 })
        lessonShot("10-tap-graded-adult", AgeBand.AGE_18_PLUS, dark = false, pick = { it is DiagramTap }) {
            val t = it as DiagramTap
            Answer.Taps(setOf(t.diagram.players.first { p -> p.id !in t.targets }.id))
        }
        lessonShot("11-match-adult-dark", AgeBand.AGE_18_PLUS, dark = true, pick = { it is Match })
        lessonShot("12-match-graded-kid", AgeBand.AGE_8_9, dark = false, pick = { it is Match }) {
            val m = it as Match
            val rights = m.pairs.map { p -> p.right }
            Answer.Matches(m.pairs.mapIndexed { i, p -> p.left to rights[if (i < 2) 1 - i else i] }.toMap())
        }
        lessonShot("13-order-adult", AgeBand.AGE_18_PLUS, dark = false, pick = { it is Order })
        lessonShot("14-situation-graded-adult", AgeBand.AGE_18_PLUS, dark = false, pick = { it is Situation }) {
            Answer.Choice((it as Situation).options.indexOfFirst { o -> o.quality == Quality.POOR })
        }
    }

    @Test fun lessonDone() {
        val s = appState(AgeBand.AGE_8_9)
        val (level, lesson) = s.nextLesson()!!
        s.openLevel(level)
        s.startLesson(level, lesson)
        val c = LessonController(lesson.steps)
        while (!c.finished) { c.submit(Answer.Seen.takeIf { c.step is Learn } ?: Answer.Choice(-1)); if (!c.finished && c.grade != null) c.next() }
        s.finishLesson(level.id, lesson.id, c.result)
        shot("15-done-kid-retry") { LearnerApp(s, dark = false) }
    }
}
