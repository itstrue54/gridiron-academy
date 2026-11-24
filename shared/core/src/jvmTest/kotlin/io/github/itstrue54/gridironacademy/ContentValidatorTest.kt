package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.*
import kotlin.test.*

class ContentValidatorTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val level = content.level("architect-8")

    private fun sit(id: String, explanation: String = "Because.") = Situation(
        id, GameState(1, 600, 1, 10, 25, 0), "Call it.",
        listOf(SituationOption("a", Quality.BEST, "w"), SituationOption("b", Quality.POOR, "w")), explanation,
    )

    private fun errsWith(items: List<Step>, q: Int = level.gate.gameQuestions): List<String> {
        val gate = level.gate.copy(gameItems = items, gameQuestions = q)
        val levels = content.curriculum.levels.map { if (it.id == level.id) it.copy(gate = gate) else it }
        return ContentValidator.validate(Content(content.curriculum.copy(levels = levels), content.glossary, content.unitsByLevel, content.scenes, content.packs, content.review))
    }

    @Test fun `gameItemsValidatedLikeSteps`() {
        val errs = errsWith(listOf(sit("g-bad", explanation = "")))
        assertTrue(errs.any { "g-bad" in it && "missing explanation" in it }, "$errs")
    }

    @Test fun `game item ids must be unique across the course`() {
        val taken = content.unitsOf(level).first().lessons.first().steps.first().id
        assertTrue(errsWith(listOf(sit(taken))).any { "duplicate step id $taken" in it })
        assertTrue(errsWith(listOf(sit("g1"), sit("g1"))).any { "duplicate step id g1" in it })
    }

    @Test fun `a game item of the wrong kind for the game is rejected`() {
        val tf = TrueFalse("g-tf", "Statement", true, "Because.")
        assertTrue(errsWith(listOf(tf)).any { "g-tf" in it && "GAME_DAY" in it })
    }

    @Test fun `gateUnplayableWithTooFewItemsRejected`() {
        // Game items alone, and few lesson items of the game's kind: still short of the questions.
        val units = content.unitsByLevel.mapValues { (id, us) ->
            if (id != level.id) us else us.map { u -> u.copy(lessons = u.lessons.map { l -> l.copy(steps = l.steps.filterNot { it is Situation }) }) }
        }
        val gate = level.gate.copy(gameItems = (1..3).map { sit("g$it") })
        val levels = content.curriculum.levels.map { if (it.id == level.id) it.copy(gate = gate) else it }
        val errs = ContentValidator.validate(Content(content.curriculum.copy(levels = levels), content.glossary, units, content.scenes, content.packs, content.review))
        assertTrue(errs.any { "level architect-8" in it && "not enough" in it }, "$errs")
    }

    @Test fun `game items count for concept lookup and release gating`() {
        val item = Situation("g-c", GameState(1, 600, 1, 10, 25, 0), "p",
            listOf(SituationOption("a", Quality.BEST, "w"), SituationOption("b", Quality.POOR, "w")), "e", concept = content.glossary.entries.first().id)
        val gate = level.gate.copy(gameItems = listOf(item))
        val levels = content.curriculum.levels.map { if (it.id == level.id) it.copy(gate = gate) else it }
        val c = Content(content.curriculum.copy(levels = levels), content.glossary, content.unitsByLevel, content.scenes, content.packs, content.review)
        assertEquals(item.concept, c.conceptOfStep("g-c"))
        assertEquals(listOf("g-c"), c.gameItemsOf(c.level(level.id)).map { it.id })
    }

    @Test fun `an unsigned game item holds its level back`() {
        val item = sit("g-r")
        val gate = level.gate.copy(gameItems = listOf(item))
        val levels = content.curriculum.levels.map { if (it.id == level.id) it.copy(gate = gate) else it }
        val every = levels.flatMap { lv -> content.unitsByLevel.getValue(lv.id).flatMap { u -> u.lessons.flatMap { l -> l.steps } } }.map { it.id }
        val signed = every.map { Signoff(it, "Coach", "2026-10-21", 2026, "0123456789abcdef") }
        val withoutItem = Content(content.curriculum.copy(levels = levels), content.glossary, content.unitsByLevel, content.scenes, content.packs, ReviewLog(signoffs = signed))
        assertTrue(level.id !in withoutItem.reviewedOnly().curriculum.levels.map { it.id })
        val withItem = Content(content.curriculum.copy(levels = levels), content.glossary, content.unitsByLevel, content.scenes, content.packs, ReviewLog(signoffs = signed + Signoff("g-r", "Coach", "2026-10-21", 2026, "0123456789abcdef")))
        assertTrue(level.id in withItem.reviewedOnly().curriculum.levels.map { it.id })
    }

    @Test fun `architect 8 plays its game in order`() {
        assertTrue(level.gate.ordered)
        val ids = { seed: Long -> io.github.itstrue54.gridironacademy.engine.LevelTest.gamePool(content.unitsOf(level), level.gate, seed).map { it.id } }
        assertEquals(ids(1), ids(99))
        assertEquals(level.gate.gameQuestions, ids(1).size)
    }
}
