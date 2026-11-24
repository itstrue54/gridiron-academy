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
        // Every other step in the course is signed, other levels' game items included.
        val every = levels.flatMap { lv -> content.unitsByLevel.getValue(lv.id).flatMap { u -> u.lessons.flatMap { l -> l.steps } } + lv.gate.gameItems.filter { it.id != item.id } }.map { it.id }
        val signed = every.map { Signoff(it, "Coach", "2026-10-21", 2026, "0123456789abcdef") }
        val withoutItem = Content(content.curriculum.copy(levels = levels), content.glossary, content.unitsByLevel, content.scenes, content.packs, ReviewLog(signoffs = signed))
        assertTrue(level.id !in withoutItem.reviewedOnly().curriculum.levels.map { it.id })
        val withItem = Content(content.curriculum.copy(levels = levels), content.glossary, content.unitsByLevel, content.scenes, content.packs, ReviewLog(signoffs = signed + Signoff("g-r", "Coach", "2026-10-21", 2026, "0123456789abcdef")))
        assertTrue(level.id in withItem.reviewedOnly().curriculum.levels.map { it.id })
    }

    private fun term(id: String, level: Int, vararg requires: String) =
        GlossaryEntry(id, id, "Kid text.", "Full text.", level, PackSide.RULES, requires.toList())
    private fun ask(id: String, concept: String) = MultipleChoice(id, "Pick one.", listOf("a", "b"), 0, "Because.", concept = concept)

    /** The shipped course plus fixture terms, and one fixture lesson added to each named level. */
    private fun errsWithLessons(
        terms: List<GlossaryEntry>,
        lessons: Map<String, List<Step>>,
        gameItems: Map<String, List<Step>> = emptyMap(),
    ): List<String> {
        val units = content.unitsByLevel.mapValues { (id, us) ->
            lessons[id]?.let { steps -> us + CourseUnit("fx-$id", "Fixture", listOf(Lesson("fx-$id-l", "Fixture", steps = steps))) } ?: us
        }
        val levels = content.curriculum.levels.map { lv ->
            gameItems[lv.id]?.let { lv.copy(gate = lv.gate.copy(gameItems = lv.gate.gameItems + it)) } ?: lv
        }
        val g = Glossary(content.glossary.entries + terms)
        return ContentValidator.validate(Content(content.curriculum.copy(levels = levels), g, units, content.scenes, content.packs, content.review))
    }

    @Test fun `prerequisiteMustBeGradedOnSameLadder`() {
        // fx-term is graded on both ladders; its prerequisite only on the adult one (the kid
        // lesson teaches it on a card, which isn't graded).
        val terms = listOf(term("fx-pre", 1), term("fx-term", 1, "fx-pre"))
        val errs = errsWithLessons(terms, mapOf(
            "kid-1" to listOf(Learn("fx-k1", "Card", "Body.", concept = "fx-pre"), ask("fx-k2", "fx-term")),
            "adult-1" to listOf(ask("fx-a1", "fx-pre"), ask("fx-a2", "fx-term")),
        )).filter { "fx-" in it }
        assertTrue(errs.any { "glossary fx-term" in it && "kid ladder" in it && "'fx-pre'" in it }, "$errs")
        assertFalse(errs.any { "adult ladder" in it }, "$errs")

        // Graded on the kid ladder too, but a level after the concept: still too late.
        val late = errsWithLessons(terms, mapOf(
            "kid-1" to listOf(Learn("fx-k1", "Card", "Body.", concept = "fx-pre"), ask("fx-k2", "fx-term")),
            "kid-2" to listOf(ask("fx-k3", "fx-pre")),
            "adult-1" to listOf(ask("fx-a1", "fx-pre"), ask("fx-a2", "fx-term")),
        )).filter { "fx-" in it }
        assertTrue(late.any { "glossary fx-term" in it && "kid ladder" in it && "Level 2" in it }, "$late")
    }

    @Test fun `a prerequisite graded at the same level as its concept is accepted`() {
        val terms = listOf(term("fx-pre", 1), term("fx-term", 1, "fx-pre"))
        val errs = errsWithLessons(terms, mapOf(
            "kid-1" to listOf(ask("fx-k1", "fx-term"), ask("fx-k2", "fx-pre")),
            "adult-1" to listOf(ask("fx-a1", "fx-term"), ask("fx-a2", "fx-pre")),
        )).filter { "glossary fx-" in it }
        assertEquals(emptyList(), errs)
    }

    @Test fun `a shared level's track-level prerequisites are graded in the track's own levels`() {
        // A Level 4 concept needs a Level 2 term. The adult track grades that term at Level 2;
        // the kid track only first grades it at Level 4 itself, so a kid reaching Level 4 has
        // never been asked it, though ADR-010 counts it as known.
        val terms = listOf(term("fx-pre", 2), term("fx-term", 4, "fx-pre"))
        val shared = "varsity-4" to listOf(ask("fx-v1", "fx-pre"), ask("fx-v2", "fx-term"))
        val errs = errsWithLessons(terms, mapOf(
            "kid-2" to listOf(Learn("fx-k1", "Card", "Body.", concept = "fx-pre")),
            "adult-2" to listOf(ask("fx-a1", "fx-pre")),
            shared,
        )).filter { "glossary fx-term" in it }
        assertTrue(errs.any { "kid ladder" in it && "'fx-pre'" in it }, "$errs")
        assertFalse(errs.any { "adult ladder" in it }, "$errs")

        val fixed = errsWithLessons(terms, mapOf(
            "kid-2" to listOf(ask("fx-k1", "fx-pre")),
            "adult-2" to listOf(ask("fx-a1", "fx-pre")),
            shared,
        )).filter { "glossary fx-term" in it }
        assertEquals(emptyList(), fixed)
    }

    @Test fun `architect 8 plays its game in order`() {
        assertTrue(level.gate.ordered)
        val ids = { seed: Long -> io.github.itstrue54.gridironacademy.engine.LevelTest.gamePool(content.unitsOf(level), level.gate, seed).map { it.id } }
        assertEquals(ids(1), ids(99))
        assertEquals(level.gate.gameQuestions, ids(1).size)
        // Every decision of the one game, through the tied final call with 25 seconds left.
        assertEquals("ar8-game-02", ids(1).first())
        assertEquals("ar8-game-12", ids(1).last())
    }

    @Test fun `an ordered gate must play its whole pool`() {
        // Cutting the story short would drop its ending.
        assertTrue(errsWith(emptyList(), q = level.gate.gameQuestions - 1).any { "level architect-8" in it && "ordered game" in it }, "short")
        assertFalse(errsWith(emptyList()).any { "ordered game" in it })
    }

    @Test fun `a game item's concept is graded in a lesson on every ladder through its level`() {
        // fx-late is graded only at Level 4: too late for an Adult 2 game item.
        val late = errsWithLessons(listOf(term("fx-late", 4)), mapOf("varsity-4" to listOf(ask("fx-v1", "fx-late"))),
            gameItems = mapOf("adult-2" to listOf(sit("fx-g1").copy(concept = "fx-late"))))
        assertTrue(late.any { "step fx-g1" in it && "adult ladder by Level 2" in it }, "$late")

        // A College 5 item on a concept only the kid track grades: fine for kids, not adults.
        val oneTrack = errsWithLessons(listOf(term("fx-kid", 3)), mapOf("kid-3" to listOf(ask("fx-k1", "fx-kid"))),
            gameItems = mapOf("college-5" to listOf(sit("fx-g2").copy(concept = "fx-kid"))))
            .filter { "step fx-g2" in it }
        assertTrue(oneTrack.any { "adult ladder by Level 5" in it }, "$oneTrack")
        assertFalse(oneTrack.any { "kid ladder" in it }, "$oneTrack")

        // Taught on both ladders in time: no complaint.
        val ok = errsWithLessons(listOf(term("fx-both", 2)),
            mapOf("kid-2" to listOf(ask("fx-k2", "fx-both")), "adult-2" to listOf(ask("fx-a2", "fx-both"))),
            gameItems = mapOf("college-5" to listOf(sit("fx-g3").copy(concept = "fx-both"))))
        assertFalse(ok.any { "step fx-g3" in it && "no graded lesson step" in it }, "$ok")
    }
}
