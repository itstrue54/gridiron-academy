package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class MasteryTest {
    private var seq = 0L
    private fun a(day: Long, pct: Float, ctx: AttemptContext = AttemptContext.LESSON, firstTry: Boolean = true) =
        AttemptEvent("l1", "d1", seq++, "s", ctx, pct, firstTry, epochDay = day, at = day * 86_400_000 + seq)

    private fun state(vararg xs: AttemptEvent, today: Long = 100) = Mastery.of("c", xs.toList(), today).state

    @Test fun `no attempts is new`() {
        assertEquals(MasteryState.NEW, state())
    }

    @Test fun `three right on one day is not yet mastered`() {
        assertEquals(MasteryState.PRACTICED, state(a(100, 1f), a(100, 1f), a(100, 1f)))
    }

    @Test fun `three right over two days with the last right is mastered`() {
        assertEquals(MasteryState.MASTERED, state(a(99, 1f), a(99, 1f), a(100, 1f)))
    }

    @Test fun `a wrong last attempt is not mastered`() {
        assertNotEquals(MasteryState.MASTERED, state(a(98, 1f), a(99, 1f), a(99, 1f), a(100, 0f)))
    }

    @Test fun `retries do not count`() {
        assertEquals(MasteryState.LEARNING, state(a(99, 0f), a(99, 1f, firstTry = false), a(100, 0f)))
    }

    @Test fun `mastered then untouched for thirty days is rusty`() {
        val xs = arrayOf(a(60, 1f), a(61, 1f), a(62, 1f))
        assertEquals(MasteryState.MASTERED, state(*xs, today = 91))
        assertEquals(MasteryState.RUSTY, state(*xs, today = 92))
    }

    @Test fun `a review miss after mastery is rusty, a lesson miss is not`() {
        val base = arrayOf(a(90, 1f), a(91, 1f), a(92, 1f))
        assertEquals(MasteryState.RUSTY, state(*base, a(95, 0f, AttemptContext.REVIEW)))
        assertNotEquals(MasteryState.RUSTY, state(*base, a(95, 0f, AttemptContext.LESSON)))
    }

    @Test fun `newer attempts weigh more`() {
        val oldMiss = Mastery.of("c", listOf(a(90, 0f), a(91, 1f), a(92, 1f)), 100).score
        val newMiss = Mastery.of("c", listOf(a(90, 1f), a(91, 1f), a(92, 0f)), 100).score
        assertTrue(oldMiss > newMiss, "$oldMiss vs $newMiss")
    }

    @Test fun `order of the input does not matter`() {
        val xs = listOf(a(90, 1f), a(91, 0f), a(92, 1f), a(93, 1f))
        assertEquals(Mastery.of("c", xs, 100), Mastery.of("c", xs.reversed(), 100))
    }

    @Test fun `forLearner folds steps into concepts and lists untouched ones as new`() {
        val xs = listOf(a(99, 1f).copy(stepId = "s1"), a(100, 1f).copy(stepId = "s2"))
        val m = Mastery.forLearner(xs, { mapOf("s1" to "c1", "s2" to "c1")[it] }, 100, listOf("c2"))
        assertEquals(2, m.getValue("c1").attempts)
        assertEquals(MasteryState.NEW, m.getValue("c2").state)
    }
}

class EventPayloadTest {
    @Test fun `attempts round-trip through the wire format`() {
        val a = AttemptEvent("l", "d", 3, "s1", AttemptContext.REVIEW, 0.5f, answer = "Cover 3", epochDay = 9, at = 10)
        val text = EventPayload.json.encodeToString(EventPayload.serializer(), EventPayload.Attempt(a))
        assertTrue(text.startsWith("{\"type\":\"attempt\""), text)
        assertEquals(EventPayload.Attempt(a), EventPayload.json.decodeFromString(EventPayload.serializer(), text))
    }

    @Test fun `a newer app's extra fields don't break an older reader`() {
        val text = """{"type":"attempt","event":{"learnerId":"l","deviceId":"d","seq":1,"stepId":"s","context":"lesson","pct":1.0,"epochDay":1,"at":2,"newField":7}}"""
        assertIs<EventPayload.Attempt>(EventPayload.json.decodeFromString(EventPayload.serializer(), text))
    }
}
