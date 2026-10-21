package io.github.itstrue54.gridironacademy.content

/**
 * The course a release build ships once review gating is on (SPEC 13.3): only levels whose
 * every step a coach has signed off. A half-reviewed level would ship with holes, so it is
 * dropped whole, and so is any level that needs a dropped one: the course ends where
 * review ends. Packs that use a dropped lesson go too, so a coach can't assign a lesson
 * the learner app doesn't have.
 */
fun Content.reviewedOnly(): Content {
    val kept = curriculum.levels.filter { lv ->
        unitsOf(lv).all { u -> u.lessons.all { l -> l.steps.all { signedOff(it.id) } } }
    }.map { it.id }.toMutableSet()
    do {
        val before = kept.size
        curriculum.levels.filter { it.id in kept }.forEach { lv ->
            val open = lv.requires.all { it in kept } && (lv.requiresAny.isEmpty() || lv.requiresAny.any { it in kept })
            if (!open) kept -= lv.id
        }
    } while (kept.size != before)

    val levels = curriculum.levels.filter { it.id in kept }
    val lessonIds = levels.flatMap { unitsOf(it) }.flatMap { it.lessons }.map { it.id }.toSet()
    return Content(
        curriculum = curriculum.copy(levels = levels),
        glossary = glossary,
        unitsByLevel = unitsByLevel.filterKeys { it in kept },
        scenes = scenes,
        packs = packs.filter { p -> p.lessons.all { it in lessonIds } },
        review = review,
    )
}
