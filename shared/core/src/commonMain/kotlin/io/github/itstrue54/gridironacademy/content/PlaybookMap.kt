package io.github.itstrue54.gridironacademy.content

import io.github.itstrue54.gridironacademy.model.GlossaryEntry
import io.github.itstrue54.gridironacademy.model.PackSide

/**
 * A concept's place on one tab of the Playbook map: [row] from the top, [x] in column
 * widths from the left (a short row is centred, so [x] can be a half step).
 */
data class MapNode(val entry: GlossaryEntry, val row: Int, val x: Double, val depth: Int)

/** A chalk line from a prerequisite down to the concept that needs it. */
data class MapEdge(val from: String, val to: String)

data class MapLayout(
    val side: PackSide,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>,
    val rows: Int,
) {
    val byId: Map<String, MapNode> by lazy { nodes.associateBy { it.entry.id } }
}

/**
 * The Playbook map's layout (DESIGN 9.14): the concept graph as a vertical skill tree, one
 * per side. A concept's depth is one more than its deepest prerequisite on the same side,
 * so every line runs downward. Within a depth, concepts sit under their prerequisites
 * (barycentre order) and a wide depth wraps into rows of [MAX_COLS] for a phone screen.
 * Prerequisites from another side get no line; the node sheet names them instead.
 */
object PlaybookMap {
    const val MAX_COLS = 4

    fun layout(entries: List<GlossaryEntry>, side: PackSide): MapLayout {
        val mine = entries.filter { it.side == side }
        val byId = mine.associateBy { it.id }
        fun sameSide(e: GlossaryEntry) = e.requires.filter { it in byId }

        val depth = mutableMapOf<String, Int>()
        fun depthOf(id: String): Int = depth.getOrPut(id) {
            sameSide(byId.getValue(id)).maxOfOrNull { depthOf(it) + 1 } ?: 0
        }
        mine.forEach { depthOf(it.id) }

        // Place depth by depth: each concept's key is the mean slot of its prerequisites,
        // so children gather under their parents. Ties keep curriculum order.
        val slot = mutableMapOf<String, Double>()
        val nodes = mutableListOf<MapNode>()
        var row = 0
        val maxDepth = depth.values.maxOrNull() ?: -1
        for (d in 0..maxDepth) {
            val band = mine.filter { depth[it.id] == d }
                .sortedWith(compareBy<GlossaryEntry>(
                    { e -> sameSide(e).map { slot.getValue(it) }.average().takeUnless { it.isNaN() } ?: 0.0 },
                    { it.level },
                    { it.term.lowercase() },
                ))
            band.chunked(MAX_COLS).forEach { line ->
                val offset = (MAX_COLS - line.size) / 2.0
                line.forEachIndexed { i, e ->
                    slot[e.id] = offset + i
                    nodes += MapNode(e, row, offset + i, d)
                }
                row++
            }
        }
        val edges = mine.flatMap { e -> sameSide(e).map { MapEdge(it, e.id) } }
        return MapLayout(side, nodes, edges, rows = row)
    }

    /** Prerequisites that live on another tab, for the node sheet. */
    fun otherSide(entry: GlossaryEntry, entries: List<GlossaryEntry>): List<GlossaryEntry> {
        val byId = entries.associateBy { it.id }
        return entry.requires.mapNotNull { byId[it] }.filter { it.side != entry.side }
    }
}
