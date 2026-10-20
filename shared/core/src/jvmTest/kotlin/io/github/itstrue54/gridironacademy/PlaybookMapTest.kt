package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.PackSide
import kotlin.test.*

class PlaybookMapTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val entries = content.glossary.entries
    private val maps = PackSide.entries.map { PlaybookMap.layout(entries, it) }

    @Test fun `every term is on exactly one tab`() {
        assertEquals(entries.map { it.id }.sorted(), maps.flatMap { m -> m.nodes.map { it.entry.id } }.sorted())
    }

    @Test fun `every line runs downward to a deeper concept`() {
        maps.forEach { m ->
            m.edges.forEach { e ->
                val from = m.byId.getValue(e.from)
                val to = m.byId.getValue(e.to)
                assertTrue(from.depth < to.depth && from.row < to.row, "${e.from} -> ${e.to} on ${m.side}")
            }
        }
    }

    @Test fun `same-side prerequisites are all drawn, other sides are not`() {
        val offense = maps.first { it.side == PackSide.OFFENSE }
        assertTrue(MapEdge("route-combination", "mesh") in offense.edges)
        // Mesh beats man coverage: that prerequisite is on the Defense tab, named in the sheet.
        assertFalse(offense.edges.any { it.from == "man-coverage" })
        val mesh = entries.first { it.id == "mesh" }
        assertEquals(listOf("man-coverage"), PlaybookMap.otherSide(mesh, entries).map { it.id })
    }

    @Test fun `rows fit a phone and sit centred`() {
        maps.forEach { m ->
            m.nodes.groupBy { it.row }.forEach { (row, ns) ->
                assertTrue(ns.size <= PlaybookMap.MAX_COLS, "${m.side} row $row has ${ns.size}")
                val mid = ns.map { it.x }.average()
                assertEquals((PlaybookMap.MAX_COLS - 1) / 2.0, mid, 1e-9)
            }
            assertEquals(m.nodes.size, m.nodes.map { it.row to it.x }.toSet().size, "no two nodes overlap")
        }
    }

    @Test fun `the basics head each tab`() {
        val defense = maps.first { it.side == PackSide.DEFENSE }
        assertEquals(0, defense.byId.getValue("defense").row)
        assertTrue(defense.byId.getValue("cover-3").row > defense.byId.getValue("zone-coverage").row)
    }

    @Test fun `the layout is the same every time`() {
        assertEquals(maps, PackSide.entries.map { PlaybookMap.layout(entries, it) })
    }
}
