package io.github.itstrue54.gridironacademy.content

import io.github.itstrue54.gridironacademy.model.GlossaryEntry

/**
 * The concept graph (SPEC 4.6, 7.4): each glossary term and the terms to know first.
 * Content, not code: the edges are `requires` in glossary.json. Drives assignment
 * warm-ups and the Playbook map.
 */
class ConceptGraph(entries: List<GlossaryEntry>) {
    private val parents: Map<String, List<String>> = entries.associate { it.id to it.requires }

    val concepts: Set<String> get() = parents.keys

    fun requires(concept: String): List<String> = parents[concept].orEmpty()

    /** Everything to know before [concepts], nearest first; [concepts] themselves excluded. */
    fun prerequisites(concepts: Collection<String>): List<String> {
        val start = concepts.toSet()
        val seen = mutableSetOf<String>()
        val out = mutableListOf<String>()
        var frontier = start.flatMap { requires(it) }.distinct()
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<String>()
            frontier.forEach { c ->
                if (c !in start && seen.add(c)) { out += c; next += requires(c) }
            }
            frontier = next.distinct()
        }
        return out
    }

    /** Concepts that list [concept] as a prerequisite. */
    fun unlocks(concept: String): List<String> = parents.filterValues { concept in it }.keys.sorted()

    /** Any cycle, as the path that closes it; empty when the graph is acyclic. */
    fun cycles(): List<List<String>> {
        val state = mutableMapOf<String, Int>()
        val found = mutableListOf<List<String>>()
        fun visit(n: String, path: List<String>) {
            when (state[n]) {
                VISITING -> { found += path.dropWhile { it != n } + n; return }
                DONE -> return
            }
            state[n] = VISITING
            requires(n).forEach { visit(it, path + n) }
            state[n] = DONE
        }
        parents.keys.sorted().forEach { visit(it, emptyList()) }
        return found
    }

    private companion object {
        const val VISITING = 1
        const val DONE = 2
    }
}
