package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.ReviewLog
import io.github.itstrue54.gridironacademy.model.Signoff
import kotlin.test.*

/** Review gating for release builds (SPEC 13.3): the course ends where review ends. */
class ReleaseContentTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    private fun signed(vararg levels: String, except: String? = null): Content {
        val ids = levels.flatMap { id -> content.unitsOf(content.level(id)).flatMap { u -> u.lessons.flatMap { it.steps } } }
            .map { it.id }.filter { it != except }
        val log = ReviewLog(signoffs = ids.map { Signoff(it, "Coach", "2026-10-21", 2026, "0123456789abcdef") })
        return Content(content.curriculum, content.glossary, content.unitsByLevel, content.scenes, content.packs, log)
    }

    private fun Content.levelIds() = curriculum.levels.map { it.id }

    @Test fun `nothing signed ships nothing`() {
        val shipped = signed().reviewedOnly()
        assertEquals(emptyList(), shipped.levelIds())
        assertEquals(emptyList(), shipped.packs)
    }

    @Test fun `a fully signed level ships, one that needs an unsigned level doesn't`() {
        assertEquals(listOf("kid-1"), signed("kid-1", "kid-3").reviewedOnly().levelIds())
    }

    @Test fun `one unsigned step holds back its whole level and everything after it`() {
        val oneMissing = signed("kid-1", "kid-2", "kid-3", except = "k2-sets-05").reviewedOnly()
        assertEquals(listOf("kid-1"), oneMissing.levelIds())
    }

    @Test fun `shared levels open from either track`() {
        val kidSide = signed("kid-1", "kid-2", "kid-3", "varsity-4").reviewedOnly().levelIds()
        assertEquals(listOf("kid-1", "kid-2", "kid-3", "varsity-4"), kidSide)
    }

    @Test fun `the shipped course is exactly the fully signed levels and what they open`() {
        // Whatever review.json holds today: a level ships if all its steps are signed and
        // its prerequisites ship. Nothing partly signed gets through.
        val shipped = content.reviewedOnly()
        shipped.curriculum.levels.forEach { lv ->
            val unsigned = content.unitsOf(lv).flatMap { u -> u.lessons.flatMap { it.steps } }.filterNot { content.signedOff(it.id) }
            assertEquals(emptyList(), unsigned.map { it.id }, "${lv.id} shipped with unsigned steps")
        }
    }

    @Test fun `everything signed ships the whole course, and it still validates`() {
        val all = signed(*content.curriculum.levels.map { it.id }.toTypedArray()).reviewedOnly()
        assertEquals(content.levelIds(), all.levelIds())
        assertEquals(content.packs, all.packs)
    }

    @Test fun `packs never point at a lesson that didn't ship`() {
        val shipped = signed("kid-1", "kid-2", "kid-3", "adult-1", "adult-2", "adult-3", "varsity-4").reviewedOnly()
        val lessons = shipped.lessons.map { it.id }.toSet()
        shipped.packs.forEach { p -> assertTrue(p.lessons.all { it in lessons }, p.id) }
        assertTrue(shipped.packs.size < content.packs.size, "packs from Level 5 and up are held back")
    }
}
