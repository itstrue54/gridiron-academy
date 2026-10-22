package io.github.itstrue54.gridironacademy.engine

import io.github.itstrue54.gridironacademy.model.Diagram
import io.github.itstrue54.gridironacademy.model.DiagramPlayer
import io.github.itstrue54.gridironacademy.model.DiagramRoute
import io.github.itstrue54.gridironacademy.model.PathKind
import io.github.itstrue54.gridironacademy.model.Side
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A diagram as plain sentences for screen readers (SPEC 11.2, DESIGN 7.6): who is where,
 * who goes where, and the shaded areas. Deterministic, so a description never changes
 * between runs and can be tested word for word.
 *
 * Offense moves up the field. "Left" and "right" are as the picture is drawn. Yards are
 * rounded to the nearest whole yard, which is how a coach would say it.
 *
 * English only for now; a translated build supplies its own [Words].
 */
object DiagramDescriber {
    /** Every word the describer says, so a locale can replace them all. */
    data class Words(
        val offense: String = "Offense",
        val defense: String = "Defense",
        val onTheLine: String = "on the line",
        val yardsBehind: (Int) -> String = { if (it == 1) "1 yard behind the line" else "$it yards behind the line" },
        val yardsOff: (Int) -> String = { if (it == 1) "1 yard off the ball" else "$it yards off the ball" },
        val middle: String = "in the middle",
        val left: String = "left of the middle",
        val right: String = "right of the middle",
        val wideLeft: String = "wide left",
        val wideRight: String = "wide right",
        val upfield: (Int) -> String = { "$it up the field" },
        val back: (Int) -> String = { "$it back" },
        val towardLeft: (Int) -> String = { "$it to the left" },
        val towardRight: (Int) -> String = { "$it to the right" },
        val then: String = ", then ",
        val kind: (PathKind) -> String = {
            when (it) {
                PathKind.PASS -> "runs a route"
                PathKind.RUN -> "carries the ball"
                PathKind.MOTION -> "goes in motion"
                PathKind.BLOCK -> "blocks"
                PathKind.BLITZ -> "rushes"
                PathKind.ROTATE -> "rotates"
                PathKind.DROP -> "drops"
            }
        },
        val named: (String) -> String = { " ($it)" },
        val zone: (label: String, from: Int, to: Int) -> String = { label, from, to ->
            "Shaded area $label, from ${yardWords(from)} to ${yardWords(to)}."
        },
    )

    /** Within this many yards of x = 0 is "the middle"; past [WIDE] is "wide". */
    private const val MIDDLE = 3f
    private const val WIDE = 12f
    /** Closer than this to the line of scrimmage counts as on it. */
    private const val ON_LINE = 0.5f

    fun describe(d: Diagram, w: Words = Words()): String {
        val parts = mutableListOf<String>()
        for (side in listOf(Side.OFFENSE, Side.DEFENSE)) {
            val players = d.players.filter { it.side == side }
            if (players.isEmpty()) continue
            // Left to right, as a sighted reader scans the picture.
            val list = players.sortedWith(compareBy({ it.x }, { it.y })).joinToString("; ") { "${it.label}, ${where(it, w)}" }
            parts += "${if (side == Side.OFFENSE) w.offense else w.defense}: $list."
        }
        d.routes.forEach { r ->
            val p = d.players.firstOrNull { it.id == r.playerId } ?: return@forEach
            parts += route(p, r, w)
        }
        d.zones.forEach { z ->
            parts += w.zone(z.label, z.y.roundToInt(), (z.y + z.h).roundToInt())
        }
        return parts.joinToString(" ")
    }

    /**
     * Everything a learner could tap, as list rows (DESIGN 7.6, list mode): players in the
     * same order [describe] reads them, offense first, then the shaded areas. Each row is
     * (id, words); the id is what the grader checks, so list and picture grade alike.
     */
    fun tapTargets(d: Diagram, w: Words = Words()): List<Pair<String, String>> {
        val players = listOf(Side.OFFENSE, Side.DEFENSE).flatMap { side ->
            d.players.filter { it.side == side }.sortedWith(compareBy({ it.x }, { it.y })).map { p ->
                p.id to "${p.label}, ${if (side == Side.OFFENSE) w.offense else w.defense}, ${where(p, w)}"
            }
        }
        val zones = d.zones.map { z -> z.id to w.zone(z.label, z.y.roundToInt(), (z.y + z.h).roundToInt()).removeSuffix(".") }
        return players + zones
    }

    private fun where(p: DiagramPlayer, w: Words): String {
        val across = when {
            abs(p.x) <= MIDDLE -> w.middle
            p.x < -WIDE -> w.wideLeft
            p.x > WIDE -> w.wideRight
            p.x < 0 -> w.left
            else -> w.right
        }
        val depth = abs(p.y).roundToInt()
        val deep = when {
            abs(p.y) < ON_LINE || depth == 0 -> w.onTheLine
            p.side == Side.OFFENSE -> if (p.y < 0) w.yardsBehind(depth) else w.yardsOff(depth)
            else -> if (p.y > 0) w.yardsOff(depth) else w.yardsBehind(depth)
        }
        return "$across, $deep"
    }

    /** Each leg as its main direction and length: "10 up the field, then 6 to the left". */
    private fun route(p: DiagramPlayer, r: DiagramRoute, w: Words): String {
        var fx = p.x
        var fy = p.y
        val legs = r.points.mapNotNull { pt ->
            val dx = pt.x - fx
            val dy = pt.y - fy
            fx = pt.x; fy = pt.y
            val ax = abs(dx).roundToInt()
            val ay = abs(dy).roundToInt()
            when {
                ax == 0 && ay == 0 -> null
                // A diagonal leg names both parts, the bigger first.
                ay >= ax -> listOfNotNull(
                    if (dy > 0) w.upfield(ay) else w.back(ay),
                    if (ax > 0) (if (dx < 0) w.towardLeft(ax) else w.towardRight(ax)) else null,
                ).joinToString(" and ")
                else -> listOfNotNull(
                    if (dx < 0) w.towardLeft(ax) else w.towardRight(ax),
                    if (ay > 0) (if (dy > 0) w.upfield(ay) else w.back(ay)) else null,
                ).joinToString(" and ")
            }
        }
        val name = if (r.label.isNotEmpty()) w.named(r.label) else ""
        return "${p.label} ${w.kind(r.kind)}$name: ${legs.joinToString(w.then)}."
    }

    private fun yardWords(y: Int): String = when {
        y == 0 -> "the line of scrimmage"
        y > 0 -> "$y yards up the field"
        else -> "${-y} yards behind the line"
    }
}
