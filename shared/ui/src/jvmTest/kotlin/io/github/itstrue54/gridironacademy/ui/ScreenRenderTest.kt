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

    /** A learner who missed a question yesterday: practice leads Home. */
    private fun withPracticeDue(band: AgeBand): LearnerAppState {
        var day = 500L
        val s = LearnerAppState(content, InMemoryLearnerStore()) { day }
        s.createProfile(if (band.child) "Leo" else "Sam", band)
        val (level, lesson) = s.nextLesson()!!
        s.startLesson(level, lesson)
        val c = s.lessonController(level.id, lesson.id)
        var missed = false
        while (!c.finished) {
            val step = c.step!!
            val a = if (!missed && step is MultipleChoice) { missed = true; Answer.Choice((step.answer + 1) % step.choices.size) }
                else when (step) {
                    is Learn -> Answer.Seen
                    is MultipleChoice -> Answer.Choice(step.answer)
                    is TrueFalse -> Answer.Bool(step.answer)
                    is Match -> Answer.Matches(step.pairs.associate { it.left to it.right })
                    is Order -> Answer.Ordering(step.items)
                    is DiagramTap -> Answer.Taps(step.targets)
                    is ReadDefense -> Answer.Choice(step.answer)
                    is Situation -> Answer.Choice(step.options.indexOfFirst { it.quality == Quality.BEST })
                }
            c.submit(a); if (!c.finished && c.grade != null) c.next()
        }
        s.finishLesson(level.id, lesson.id, c.result); s.back()
        day = 501
        return s
    }

    @Test fun glossary() {
        val kid = appState(AgeBand.AGE_8_9).also { it.openGlossary() }
        shot("40-glossary-kid") { LearnerApp(kid, dark = false) }
        val adult = appState(AgeBand.AGE_18_PLUS).also { it.openGlossary() }
        shot("41-glossary-adult") { LearnerApp(adult, dark = true) }
    }

    @Test fun homeWithPracticeDue() {
        shot("03b-home-practice-kid") { LearnerApp(withPracticeDue(AgeBand.AGE_8_9), dark = false) }
        shot("04b-home-practice-adult") { LearnerApp(withPracticeDue(AgeBand.AGE_18_PLUS), dark = true) }
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

    @Test fun levelTwo() {
        lessonShot("25-formation-question-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a2-formations-06" })
        lessonShot("26-ineligible-tap-graded-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a2-eligibility-04" }) {
            Answer.Taps(setOf("lt", "lg", "c", "te"))
        }
        lessonShot("27-oline-tap-kid", AgeBand.AGE_8_9, dark = false, pick = { it.id == "k2-oline-03" })
        lessonShot("28-gameday-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a2-gameday-09" }) {
            Answer.Choice(0)
        }
    }

    @Test fun levelThree() {
        lessonShot("29-route-tap-kid", AgeBand.AGE_10_12, dark = false, pick = { it.id == "k3-moreroutes-05" })
        lessonShot("30-zone-tap-kid", AgeBand.AGE_10_12, dark = false, pick = { it.id == "k3-zone-04" })
        lessonShot("31-front-34-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a3-fronts-04" })
        lessonShot("32-sweep-pullers-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a3-blocking-03" })
        lessonShot("33-leverage-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a3-leverage-02" })
        lessonShot("34-highlow-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a3-pairs-03" })
        lessonShot("35-counter-kid", AgeBand.AGE_10_12, dark = false, pick = { it.id == "k3-runs-06" })
        lessonShot("36-tree-tap-adult", AgeBand.AGE_18_PLUS, dark = true, pick = { it.id == "a3-tree-05" })
    }

    private fun sharedShot(name: String, band: AgeBand, id: String, levelId: String = "varsity-4", answer: ((Step) -> Answer)? = null) {
        val state = appState(band)
        val level = content.level(levelId)
        val lesson = content.unitsOf(level).flatMap { it.lessons }.first { l -> l.steps.any { it.id == id } }
        val controller = LessonController(listOf(lesson.steps.first { it.id == id }))
        if (answer != null) controller.submit(answer(controller.step!!))
        shot(name) { GridironTheme(skin = state.skin, dark = true) { LessonScreen(state, level.id, lesson.id, controller) } }
    }

    @Test fun levelFour() {
        sharedShot("37-cover3-presnap-adult", AgeBand.AGE_18_PLUS, "v4-cover3-04")
        sharedShot("38-cover3-reveal-adult", AgeBand.AGE_18_PLUS, "v4-cover3-04", answer = { Answer.Choice((it as ReadDefense).answer) })
        sharedShot("39-cover2-reveal-kid", AgeBand.AGE_10_12, "v4-cover2-04", answer = { Answer.Choice((it as ReadDefense).answer) })
        sharedShot("40-cover4-presnap-adult", AgeBand.AGE_18_PLUS, "v4-cover4-04")
        sharedShot("41-front-bear-adult", AgeBand.AGE_18_PLUS, "v4-fronts-05")
        sharedShot("42-power-tap-adult", AgeBand.AGE_18_PLUS, "v4-gap-05")
        sharedShot("43-mesh-adult", AgeBand.AGE_18_PLUS, "v4-dropback-05")
        sharedShot("44-gaps-tap-kid", AgeBand.AGE_10_12, "v4-gaps-04")
    }

    @Test fun levelFive() {
        val c = "college-5"
        sharedShot("45-zone-read-tap", AgeBand.AGE_18_PLUS, "c5-option-05", c)
        sharedShot("46-rpo-tap", AgeBand.AGE_18_PLUS, "c5-rpo-03", c)
        sharedShot("47-hot-tap", AgeBand.AGE_18_PLUS, "c5-protect-05", c)
        sharedShot("48-crossdog-reveal", AgeBand.AGE_18_PLUS, "c5-pressure-05", c, answer = { Answer.Choice((it as ReadDefense).answer) })
        sharedShot("49-firezone-reveal", AgeBand.AGE_18_PLUS, "c5-pressure-06", c, answer = { Answer.Choice((it as ReadDefense).answer) })
        sharedShot("50-orbit", AgeBand.AGE_18_PLUS, "c5-motion-04", c)
        sharedShot("51-nickel-tap", AgeBand.AGE_10_12, "c5-subs-04", c)
        sharedShot("52-gameday", AgeBand.AGE_18_PLUS, "c5-gameday-06", c, answer = { Answer.Choice(0) })
    }

    @Test fun levelSix() {
        val p = "pro-6"
        val reveal: (Step) -> Answer = { Answer.Choice((it as ReadDefense).answer) }
        sharedShot("53-robber", AgeBand.AGE_18_PLUS, "p6-cover1-03", p, reveal)
        sharedShot("54-2man", AgeBand.AGE_18_PLUS, "p6-cover1-04", p, reveal)
        sharedShot("55-tampa2", AgeBand.AGE_18_PLUS, "p6-tampa-03", p, reveal)
        sharedShot("56-buzz", AgeBand.AGE_18_PLUS, "p6-cover3-05", p, reveal)
        sharedShot("57-cloud", AgeBand.AGE_18_PLUS, "p6-cover3-06", p, reveal)
        sharedShot("58-cover6", AgeBand.AGE_18_PLUS, "p6-quarters-05", p, reveal)
        sharedShot("59-box6", AgeBand.AGE_18_PLUS, "p6-presnap-03", p)
        sharedShot("60-box8", AgeBand.AGE_18_PLUS, "p6-presnap-04", p)
    }

    @Test fun levelSeven() {
        val c = "coordinator-7"
        val reveal: (Step) -> Answer = { Answer.Choice((it as ReadDefense).answer) }
        listOf("co7-rotate-04", "co7-rotate-05", "co7-rotate-06", "co7-late-03", "co7-late-04", "co7-pressure-03", "co7-pressure-04", "co7-coverage-04", "co7-coverage-05", "co7-coverage-06")
            .forEachIndexed { i, id -> sharedShot("${61 + i}-$id", AgeBand.AGE_18_PLUS, id, c, reveal) }
        sharedShot("71-tap-the-tell", AgeBand.AGE_18_PLUS, "co7-rotate-03", c)
    }

    @Test fun postMotionBothPictures() {
        sharedShot("72-post-motion-presnap", AgeBand.AGE_18_PLUS, "co7-late-04", "coordinator-7")
    }

    @Test fun levelEight() {
        val a = "architect-8"
        sharedShot("73-widezone-system", AgeBand.AGE_18_PLUS, "ar8-offense-05", a)
        sharedShot("74-game-opening", AgeBand.AGE_18_PLUS, "ar8-game-02", a, answer = { Answer.Choice(0) })
        sharedShot("75-game-weather", AgeBand.AGE_18_PLUS, "ar8-game-09", a)
        sharedShot("76-success-rate", AgeBand.AGE_10_12, "ar8-analytics-05", a)
    }
}
