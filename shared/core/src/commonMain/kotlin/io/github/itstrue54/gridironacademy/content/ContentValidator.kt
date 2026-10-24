package io.github.itstrue54.gridironacademy.content

import io.github.itstrue54.gridironacademy.engine.LevelTest
import io.github.itstrue54.gridironacademy.model.*
import kotlin.math.abs

/**
 * Every content mistake that would otherwise surface as a wrong answer key or a
 * crash on a child's phone is caught here, and CI runs it over all shipped content.
 */
object ContentValidator {
    /** Kid-track text limits (words). Read-aloud is on, but short is still kinder. */
    const val KID_BODY_MAX_WORDS = 45
    const val KID_PROMPT_MAX_WORDS = 28
    /** Levels with fewer true/false items than this aren't checked for balance. */
    const val TF_BALANCE_MIN_ITEMS = 6

    fun validate(c: Content): List<String> {
        val errs = mutableListOf<String>()
        fun err(where: String, msg: String) { errs += "$where: $msg" }

        val levelIds = c.curriculum.levels.map { it.id }
        dupes(levelIds).forEach { err("curriculum", "duplicate level id $it") }
        dupes(c.curriculum.levels.map { it.number + 1000 * it.track.ordinal }).forEach {
            err("curriculum", "duplicate level number within a track: ${it % 1000}")
        }
        val glossaryIds = c.glossary.entries.map { it.id }.toSet()
        dupes(c.glossary.entries.map { it.id }).forEach { err("glossary", "duplicate id $it") }
        c.glossary.entries.forEach {
            if (it.kid.isBlank() || it.full.isBlank() || it.term.isBlank()) err("glossary ${it.id}", "blank field")
        }

        diagram(c.scenes.welcome.diagram, "app/scenes welcome", ::err)
        if (c.scenes.welcome.description.isBlank()) err("app/scenes welcome", "missing description")

        val allStepIds = mutableListOf<String>()
        val allLessonIds = mutableListOf<String>()

        for (lv in c.curriculum.levels) {
            (lv.requires + lv.requiresAny).forEach { if (it !in levelIds) err("level ${lv.id}", "requires unknown level $it") }
            val units = c.unitsByLevel[lv.id].orEmpty()
            if (units.isEmpty()) err("level ${lv.id}", "has no units")
            if (!LevelTest.gateIsPlayable(units, lv.gate)) {
                err("level ${lv.id}", "not enough ${lv.gate.game} content to run its gate")
            }
            for (u in units) {
                errs += validateUnit(u, lv.track == Track.KID, glossaryIds)
                u.lessons.forEach { l -> allLessonIds += l.id; l.steps.forEach { allStepIds += it.id } }
            }
            // "Always True" must not pass a level: each answer is at least a third of its
            // true/false items (it was four in five before the launch review).
            val tf = units.flatMap { u -> u.lessons.flatMap { it.steps } }.filterIsInstance<TrueFalse>()
            if (tf.size >= TF_BALANCE_MIN_ITEMS) {
                val falses = tf.count { !it.answer }
                val minority = minOf(falses, tf.size - falses)
                if (minority * 3 < tf.size) err("level ${lv.id}", "true/false answers lean one way: ${tf.size - falses} true, $falses false")
            }
        }
        dupes(allStepIds).forEach { err("content", "duplicate step id $it") }
        dupes(allLessonIds).forEach { err("content", "duplicate lesson id $it") }

        // The concept graph (SPEC 4.6): real ids, no cycles, a prerequisite never taught
        // after the concept that needs it, and every term taught where the glossary says.
        for (e in c.glossary.entries) {
            val where = "glossary ${e.id}"
            e.requires.forEach { r ->
                if (r !in glossaryIds) err(where, "requires unknown concept '$r'")
                if (r == e.id) err(where, "requires itself")
            }
            val taught = c.firstTaughtLevel(e.id)
            if (taught == null) err(where, "no lesson teaches it")
            else {
                if (taught != e.level) err(where, "level says ${e.level} but it's first taught in Level $taught")
                e.requires.forEach { r ->
                    val rl = c.firstTaughtLevel(r)
                    if (rl != null && rl > taught) err(where, "needs '$r', which isn't taught until Level $rl")
                }
            }
        }
        // Sign-offs (SPEC 13.3): real steps, one each, a named reviewer and a real date.
        val stepIds = c.lessons.flatMap { l -> l.steps.map { it.id } }.toSet()
        dupes(c.review.signoffs.map { it.step }).forEach { err("review", "signed off twice: $it") }
        c.review.signoffs.forEach {
            if (it.step !in stepIds) err("review ${it.step}", "no such step")
            if (it.reviewer.isBlank()) err("review ${it.step}", "no reviewer")
            if (!ISO_DATE.matches(it.date)) err("review ${it.step}", "date '${it.date}' is not YYYY-MM-DD")
            if (!HASH.matches(it.hash)) err("review ${it.step}", "hash '${it.hash}' is not 16 hex digits")
        }
        c.graph.cycles().forEach { err("glossary", "concept cycle: ${it.joinToString(" -> ")}") }
        // Direct parents only (SPEC 4.6): a line the map would draw twice.
        if (c.graph.cycles().isEmpty()) c.graph.redundant().forEach { (concept, r, via) ->
            err("glossary $concept", "requires '$r', which '$via' already needs")
        }

        dupes(c.packs.map { it.id }).forEach { err("packs", "duplicate pack id $it") }
        for (p in c.packs) {
            val where = "pack ${p.id}"
            if (p.title.isBlank() || p.blurb.isBlank()) err(where, "blank title or blurb")
            if (p.lessons.isEmpty()) err(where, "has no lessons")
            p.lessons.forEach { if (c.lesson(it) == null) err(where, "unknown lesson '$it'") }
            if (p.check.passPct !in 1..100) err(where, "check passPct must be 1-100")
            val graded = p.lessons.mapNotNull { c.lesson(it) }.sumOf { l -> l.steps.count { it !is Learn } }
            if (graded < p.check.questions) err(where, "check needs ${p.check.questions} questions, lessons have $graded")
        }
        return errs
    }

    /** Validates one unit on its own: used for shipped levels and for preview units. */
    fun validateUnit(u: CourseUnit, kid: Boolean, glossaryIds: Set<String>): List<String> {
        val errs = mutableListOf<String>()
        fun err(where: String, msg: String) { errs += "$where: $msg" }
        for (l in u.lessons) {
            if (l.steps.isEmpty()) err("lesson ${l.id}", "no steps")
            if (l.steps.none { it !is Learn }) err("lesson ${l.id}", "has no graded step")
            dupes(l.steps.map { it.id }).forEach { err("lesson ${l.id}", "duplicate step id $it") }
            for (s in l.steps) {
                val where = "step ${s.id}"
                s.concept?.let { if (it !in glossaryIds) err(where, "unknown concept '$it'") }
                checkStep(s, where, kid, ::err)
            }
        }
        return errs
    }

    private fun checkStep(s: Step, w: String, kid: Boolean, err: (String, String) -> Unit) {
        fun words(t: String) = t.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        fun prompt(t: String) {
            if (t.isBlank()) err(w, "blank prompt")
            if (kid && words(t) > KID_PROMPT_MAX_WORDS) err(w, "kid prompt over $KID_PROMPT_MAX_WORDS words")
        }
        fun explain(t: String) {
            if (t.isBlank()) err(w, "missing explanation")
            // The same text follows a right and a wrong answer, under the app's own verdict,
            // so it must not give one: "Not quite. Here's why." then "Yes! ..." contradicts.
            VERDICT_OPENER.find(t)?.let { err(w, "explanation opens with a verdict ('${it.value.trim()}'); the headline gives it") }
        }
        fun choices(list: List<String>, ans: Int) {
            if (list.size < 2) err(w, "needs at least 2 choices")
            if (list.toSet().size != list.size) err(w, "duplicate choices")
            if (ans !in list.indices) err(w, "answer index $ans out of range")
            if (list.any { it.isBlank() }) err(w, "blank choice")
        }
        when (s) {
            is Learn -> {
                if (s.title.isBlank() || s.body.isBlank()) err(w, "blank learn card")
                if (kid && words(s.body) > KID_BODY_MAX_WORDS) err(w, "kid card over $KID_BODY_MAX_WORDS words")
                s.diagram?.let { diagram(it, w, err) }
            }
            is MultipleChoice -> { prompt(s.prompt); explain(s.explanation); choices(s.choices, s.answer); s.diagram?.let { diagram(it, w, err) } }
            is TrueFalse -> { prompt(s.statement); explain(s.explanation) }
            is Match -> {
                prompt(s.prompt); explain(s.explanation)
                if (s.pairs.size < 2) err(w, "needs at least 2 pairs")
                if (s.pairs.map { it.left }.toSet().size != s.pairs.size) err(w, "duplicate left side")
                if (s.pairs.map { it.right }.toSet().size != s.pairs.size) err(w, "duplicate right side")
            }
            is Order -> {
                prompt(s.prompt); explain(s.explanation)
                if (s.items.size < 2) err(w, "needs at least 2 items")
                if (s.items.toSet().size != s.items.size) err(w, "duplicate items")
            }
            is DiagramTap -> {
                prompt(s.prompt); explain(s.explanation); diagram(s.diagram, w, err)
                val ids = s.diagram.players.map { it.id } + s.diagram.zones.map { it.id }
                if (s.targets.isEmpty()) err(w, "no targets")
                s.targets.forEach { if (it !in ids) err(w, "target '$it' is not on the diagram") }
            }
            is ReadDefense -> {
                prompt(s.prompt); explain(s.explanation)
                choices(s.choices, s.answer)
                diagram(s.preSnap, w, err); diagram(s.reveal, w, err)
                if (s.preSnap.players.map { it.id }.toSet() != s.reveal.players.map { it.id }.toSet()) {
                    err(w, "preSnap and reveal must show the same players")
                }
                if (s.disguised && s.reveal.routes.none { it.kind == PathKind.ROTATE || it.kind == PathKind.BLITZ || it.kind == PathKind.DROP }) {
                    err(w, "disguised look needs a reveal with rotate/blitz/drop paths")
                }
            }
            is Situation -> {
                explain(s.explanation); if (s.prompt.isBlank()) err(w, "blank prompt")
                if (s.options.size < 2) err(w, "needs at least 2 options")
                if (s.options.count { it.quality == Quality.BEST } != 1) err(w, "needs exactly one BEST option")
                if (s.options.any { it.why.isBlank() }) err(w, "every option needs a why")
                val g = s.state
                if (g.down !in 1..4 || g.quarter !in 1..5 || g.yardLine !in 1..99 || g.toGo < 1 || g.clock !in 0..900) {
                    err(w, "implausible game state")
                }
            }
        }
    }

    private fun diagram(d: Diagram, w: String, err: (String, String) -> Unit) {
        val ids = d.players.map { it.id }
        dupes(ids).forEach { err(w, "diagram duplicate player id $it") }
        dupes(d.zones.map { it.id }).forEach { err(w, "diagram duplicate zone id $it") }
        d.routes.forEach { r ->
            if (r.playerId !in ids) err(w, "route for unknown player '${r.playerId}'")
            if (r.points.isEmpty()) err(w, "empty route for '${r.playerId}'")
        }
        d.ball.forEach { b ->
            if (b.playerId == null && (b.x == null || b.y == null)) err(w, "ball spot needs a player or x and y")
            if (b.playerId != null && b.playerId !in ids) err(w, "ball with unknown player '${b.playerId}'")
            if (b.at !in 0f..1f) err(w, "ball reaches '${b.playerId}' at ${b.at}, outside 0..1")
        }
        if (d.ball.zipWithNext().any { (a, b) -> b.at <= a.at }) err(w, "ball spots out of time order")
        d.players.forEach { if (it.x !in -27f..27f) err(w, "player ${it.id} off the field (x=${it.x})") }
        // Football legality: never more than 11 a side, and a full offense has seven on the
        // line at the snap. A partial offense (a whiteboard concept drawing) is fine.
        for (side in Side.entries) {
            val n = d.players.count { it.side == side }
            if (n > 11) err(w, "$n ${side.name.lowercase()} players on the diagram (11 is the limit)")
        }
        val offense = d.players.filter { it.side == Side.OFFENSE }
        if (offense.size == 11) {
            val onLine = offense.count { abs(it.y) < ON_THE_LINE }
            if (onLine < 7) err(w, "illegal formation: $onLine offensive players on the line, 7 required")
        }
    }

    /** How far from y = 0 a player still counts as on the line (diagrams nudge some to 0.2). */
    private const val ON_THE_LINE = 0.5f
    private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    private val HASH = Regex("[0-9a-f]{16}")
    private val VERDICT_OPENER = Regex("""^(Yes|Right|Correct|Not quite|No|Exactly|Wrong|Nope)[.!,:]\s""")

    private fun <T> dupes(l: List<T>): Set<T> = l.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
