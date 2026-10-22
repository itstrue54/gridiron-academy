package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.*
import kotlin.test.*

class ContentTest {
    private val content = ContentLoader.load(ClasspathContentSource())

    @Test fun `all shipped content validates`() {
        val errs = ContentValidator.validate(content)
        assertTrue(errs.isEmpty(), errs.joinToString("\n"))
    }

    @Test fun `preview units for later levels validate`() {
        val glossary = content.glossary.entries.map { it.id }.toSet()
        for (f in listOf("preview/l6-personnel.json", "preview/l7-disguise.json")) {
            val unit = ContentLoader.json.decodeFromString<CourseUnit>(ClasspathContentSource().read(f)!!)
            val errs = ContentValidator.validateUnit(unit, kid = false, glossaryIds = glossary)
            assertTrue(errs.isEmpty(), "$f\n" + errs.joinToString("\n"))
        }
    }

    @Test fun `both tracks have a level one`() {
        assertEquals(setOf(Track.KID, Track.ADULT), content.curriculum.levels.filter { it.number == 1 }.map { it.track }.toSet())
    }

    @Test fun `kid level uses every game and step type it needs`() {
        val steps = content.unitsOf(content.level("kid-1")).flatMap { u -> u.lessons.flatMap { it.steps } }
        assertTrue(steps.any { it is Match } && steps.any { it is Order } && steps.any { it is DiagramTap } && steps.any { it is Situation })
    }

    @Test fun `validator catches a bad answer key and a bad target`() {
        val glossary = emptySet<String>()
        val bad = CourseUnit("u", "u", listOf(Lesson("l", "l", steps = listOf(
            MultipleChoice("a", "p", listOf("x", "y"), 5, "e"),
            DiagramTap("b", "p", Diagram(players = listOf(DiagramPlayer("qb", "QB", Side.OFFENSE, 0f, 0f))), setOf("zzz"), "e"),
            Situation("c", GameState(1, 100, 1, 10, 50, 0), "p",
                listOf(SituationOption("a", Quality.OK, "w"), SituationOption("b", Quality.OK, "w")), "e"),
        ))))
        val errs = ContentValidator.validateUnit(bad, false, glossary)
        assertTrue(errs.any { "out of range" in it })
        assertTrue(errs.any { "not on the diagram" in it })
        assertTrue(errs.any { "exactly one BEST" in it })
    }

    @Test fun `validator rejects an explanation that gives its own verdict`() {
        // Found in review: "Not quite. Here's why." followed by "Yes! Get the ball..."
        val unit = CourseUnit("u", "u", listOf(Lesson("l", "l", steps = listOf(
            TrueFalse("a", "s", true, "Yes! The end zone is where you score."),
            TrueFalse("b", "s", false, "Not quite. It's short."),
            TrueFalse("c", "s", true, "No deep safety means Cover 0."),
        ))))
        val errs = ContentValidator.validateUnit(unit, false, emptySet())
        assertEquals(2, errs.count { "opens with a verdict" in it }, errs.joinToString())
    }

    @Test fun `validator catches an illegal formation`() {
        fun offense(onLine: Int) = (0 until 11).map { i ->
            DiagramPlayer("o$i", "O", Side.OFFENSE, i * 2f - 10f, if (i < onLine) 0f else -1f)
        }
        fun errs(onLine: Int) = ContentValidator.validateUnit(CourseUnit("u", "u", listOf(Lesson("l", "l", steps = listOf(
            Learn("a", "t", "b", diagram = Diagram(players = offense(onLine))))))), false, emptySet())
        assertTrue(errs(6).any { "illegal formation" in it }, errs(6).joinToString("\n"))
        assertTrue(errs(7).none { "illegal formation" in it }, errs(7).joinToString("\n"))
    }

    @Test fun `kid text limits are enforced`() {
        val long = (1..60).joinToString(" ") { "word" }
        val u = CourseUnit("u", "u", listOf(Lesson("l", "l", steps = listOf(
            Learn("a", "t", long), TrueFalse("b", "s", true, "e")))))
        assertTrue(ContentValidator.validateUnit(u, kid = true, glossaryIds = emptySet()).any { "kid card" in it })
        assertTrue(ContentValidator.validateUnit(u, kid = false, glossaryIds = emptySet()).isEmpty())
    }

    @Test fun `roundtrip json is stable`() {
        val u = content.unitsOf(content.level("kid-1")).first()
        val again = ContentLoader.json.decodeFromString<CourseUnit>(ContentLoader.json.encodeToString(CourseUnit.serializer(), u))
        assertEquals(u, again)
    }
}
