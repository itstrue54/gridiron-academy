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
import io.github.itstrue54.gridironacademy.ui.games.SpeedRoundScreen
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
    private fun shot(name: String, height: Int = 1520, ui: @Composable () -> Unit) {
        val bytes = ImageComposeScene(width = 720, height = height, density = Density(2f)) { ui() }.use { scene ->
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

    private fun perfect(step: Step): Answer = when (step) {
        is Learn -> Answer.Seen
        is MultipleChoice -> Answer.Choice(step.answer)
        is ReadDefense -> Answer.Choice(step.answer)
        is TrueFalse -> Answer.Bool(step.answer)
        is Match -> Answer.Matches(step.pairs.associate { it.left to it.right })
        is Order -> Answer.Ordering(step.items)
        is DiagramTap -> Answer.Taps(step.targets)
        is Situation -> Answer.Choice(step.options.indexOfFirst { it.quality == Quality.BEST })
    }

    private fun drillsDone(band: AgeBand): LearnerAppState {
        val s = appState(band)
        val level = s.levels.first()
        s.openLevel(level)
        content.unitsOf(level).flatMap { it.lessons }.forEach { lesson ->
            s.startLesson(level, lesson)
            val c = s.lessonController(level.id, lesson.id)
            while (!c.finished) { c.submit(perfect(c.step!!)); if (!c.finished && c.grade != null) c.next() }
            s.finishLesson(level.id, lesson.id, c.result)
            s.back()
        }
        return s
    }

    @Test fun gates() {
        val adult = appState(AgeBand.AGE_18_PLUS).also { it.openLevel(it.levels.first()) }
        shot("16-level-adult-testout", height = 3400) { LearnerApp(adult, dark = false) }

        val kid = drillsDone(AgeBand.AGE_8_9)
        shot("17-level-kid-checkpoint-open", height = 3800) { LearnerApp(kid, dark = false) }

        val level = kid.levels.first()
        kid.startCheckpoint(level)
        val test = kid.checkpointController(level.id, false)
        while (!test.finished) { test.submit(perfect(test.step!!)); if (!test.finished) test.next() }
        kid.finishCheckpoint(level.id, false, test.result)
        shot("18-checkpoint-passed-kid") { LearnerApp(kid, dark = false) }

        kid.back()
        kid.startBigGame(level)
        val round = kid.speedRoundController(level.id)
        repeat(3) { round.tick(4_000L * (it + 1)); round.answer(perfect(round.current!!)) }
        round.tick(51_500)
        shot("19-speed-round-kid") {
            GridironTheme(skin = kid.skin, dark = false) { SpeedRoundScreen(kid, level.id, round, clock = false) }
        }

        // Miss two, then let the clock run out: a loss with a review list.
        round.answer(Answer.Bool(round.current.let { (it as? TrueFalse)?.answer?.not() ?: false }).takeIf { round.current is TrueFalse } ?: Answer.Choice(-1))
        round.answer(Answer.Choice(-1).takeIf { round.current is MultipleChoice } ?: Answer.Bool(!(round.current as TrueFalse).answer))
        round.tick(60_000)
        kid.finishSpeedRound(level.id, round)
        shot("20-big-game-lost-kid") { LearnerApp(kid, dark = false) }

        kid.back()
        kid.startBigGame(level)
        val win = kid.speedRoundController(level.id)
        var t = 0L
        while (!win.over) { t += 1_000; win.tick(t); win.current?.let { win.answer(perfect(it)) } }
        kid.finishSpeedRound(level.id, win)
        shot("21-level-complete-kid") { LearnerApp(kid, dark = false) }
        shot("22-home-level-passed-adult-dark") {
            LearnerApp(drillsDone(AgeBand.AGE_18_PLUS).also { it.back() }, dark = true)
        }
    }

    @Test fun readTheDefense() {
        val unit = ContentLoader.json.decodeFromString(CourseUnit.serializer(), ClasspathContentSource().read("preview/l7-disguise.json")!!)
        val step = unit.lessons.flatMap { it.steps }.filterIsInstance<ReadDefense>().first()
        val state = appState(AgeBand.AGE_18_PLUS)
        val level = state.levels.first()
        val pre = LessonController(listOf(step))
        shot("23-read-defense-presnap-adult") {
            GridironTheme(skin = state.skin, dark = true) { LessonScreen(state, level.id, "preview", pre) }
        }
        val graded = LessonController(listOf(step)).also { it.submit(Answer.Choice(step.answer)) }
        shot("24-read-defense-reveal-adult") {
            GridironTheme(skin = state.skin, dark = true) { LessonScreen(state, level.id, "preview", graded) }
        }
    }
}
