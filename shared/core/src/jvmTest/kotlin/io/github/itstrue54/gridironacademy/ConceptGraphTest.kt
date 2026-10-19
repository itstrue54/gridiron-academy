package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.Glossary
import io.github.itstrue54.gridironacademy.model.GlossaryEntry
import kotlin.test.*

class ConceptGraphTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val graph = content.graph

    @Test fun `the shipped graph is acyclic and covers every term`() {
        assertEquals(emptyList(), graph.cycles())
        assertEquals(content.glossary.entries.map { it.id }.toSet(), graph.concepts)
    }

    @Test fun `prerequisites are nearest first and never include the start`() {
        val pre = graph.prerequisites(listOf("cover-3"))
        assertEquals(listOf("zone-coverage", "safety-position"), pre.take(2))
        assertTrue("defense-positions" in pre && "defense" in pre)
        assertFalse("cover-3" in pre)
        assertTrue(pre.indexOf("defense") > pre.indexOf("defense-positions"), "farther ancestors come later")
    }

    @Test fun `disguise builds on reading the shell`() {
        assertTrue("safety-position" in graph.prerequisites(listOf("disguise")))
        assertTrue("disguise" in graph.unlocks("mofc"))
    }

    private fun validateWith(change: (GlossaryEntry) -> GlossaryEntry): List<String> {
        val g = Glossary(content.glossary.entries.map(change))
        return ContentValidator.validate(Content(content.curriculum, g, content.unitsByLevel, content.scenes, content.packs))
    }

    @Test fun `the validator catches a cycle`() {
        val errs = validateWith { if (it.id == "down") it.copy(requires = it.requires + "fourth-down") else it }
        assertTrue(errs.any { "concept cycle" in it }, errs.joinToString())
    }

    @Test fun `the validator catches an unknown or self prerequisite`() {
        val errs = validateWith {
            when (it.id) {
                "touchdown" -> it.copy(requires = listOf("ghost"))
                "punt" -> it.copy(requires = listOf("punt"))
                else -> it
            }
        }
        assertTrue(errs.any { "unknown concept 'ghost'" in it })
        assertTrue(errs.any { "requires itself" in it })
    }

    @Test fun `the validator catches a prerequisite taught too late and a wrong level`() {
        val errs = validateWith {
            when (it.id) {
                "touchdown" -> it.copy(requires = listOf("cover-3"))
                "field" -> it.copy(level = 5)
                else -> it
            }
        }
        assertTrue(errs.any { "needs 'cover-3', which isn't taught until Level 4" in it }, errs.joinToString())
        assertTrue(errs.any { "glossary field" in it && "first taught in Level 1" in it })
    }
}
