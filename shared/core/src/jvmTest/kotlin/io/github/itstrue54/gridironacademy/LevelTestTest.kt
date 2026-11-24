package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.engine.LevelTest
import io.github.itstrue54.gridironacademy.model.*
import kotlin.test.*

/** Ordered gates and game-only items (SPEC 4.4). */
class LevelTestTest {
    private fun sit(id: String) = Situation(
        id, GameState(1, 600, 1, 10, 25, 0), "Call it.",
        listOf(SituationOption("a", Quality.BEST, "w"), SituationOption("b", Quality.POOR, "w")), "e",
    )

    private fun units(n: Int) = listOf(CourseUnit("u", "u", listOf(Lesson("l", "l", steps = (1..n).map { sit("l$it") }))))
    private fun gate(items: List<Step> = emptyList(), q: Int = 10, ordered: Boolean = false) =
        LevelGate(game = GameType.GAME_DAY, gameQuestions = q, gameItems = items, ordered = ordered)

    @Test fun `orderedGamePlaysAuthoredOrder`() {
        val g = gate(ordered = true)
        val expected = (1..10).map { "l$it" }
        for (seed in listOf(1L, 7L, 12345L)) {
            assertEquals(expected, LevelTest.gamePool(units(15), g, seed).map { it.id })
        }
    }

    @Test fun `ordered game puts game items first then lesson items in order`() {
        val g = gate(listOf(sit("g1"), sit("g2")), q = 5, ordered = true)
        assertEquals(listOf("g1", "g2", "l1", "l2", "l3"), LevelTest.gamePool(units(15), g, 3).map { it.id })
    }

    @Test fun `gameItemsDrawnFirst`() {
        val items = (1..8).map { sit("g$it") }
        val g = gate(items, q = 10)
        for (seed in 1L..5L) {
            val pool = LevelTest.gamePool(units(20), g, seed)
            assertEquals(10, pool.size)
            assertEquals(items.map { it.id }.toSet(), pool.map { it.id }.filter { it.startsWith("g") }.toSet())
        }
    }

    @Test fun `more game items than questions are sampled by seed`() {
        val items = (1..12).map { sit("g$it") }
        val g = gate(items, q = 5)
        val a = LevelTest.gamePool(units(3), g, 1).map { it.id }
        assertEquals(5, a.size)
        assertTrue(a.all { it.startsWith("g") })
        assertEquals(a, LevelTest.gamePool(units(3), g, 1).map { it.id })
        assertTrue((1L..20L).map { LevelTest.gamePool(units(3), g, it).map { s -> s.id } }.toSet().size > 1)
    }

    @Test fun `a gate with no game items draws exactly as before`() {
        val g = gate(q = 6)
        assertEquals(
            io.github.itstrue54.gridironacademy.engine.Rng(4L xor 0x5DEECE66DL).sample(units(15)[0].lessons[0].steps, 6).map { it.id },
            LevelTest.gamePool(units(15), g, 4).map { it.id },
        )
    }

    @Test fun `game items alone can make a gate playable`() {
        // 12 lesson items for the test, 8 game items + 2 lesson items fill 10 questions.
        assertTrue(LevelTest.gateIsPlayable(units(12), gate((1..8).map { sit("g$it") }, q = 10)))
        // Too few of either: 3 game items + 4 lesson items is 7 < 10.
        assertFalse(LevelTest.gateIsPlayable(units(4), gate((1..3).map { sit("g$it") }, q = 10).copy(testQuestions = 4)))
    }
}
