package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class TrackSwitchTest {
    private val levels = ContentLoader.load(ClasspathContentSource()).curriculum.levels

    @Test fun `an adult through Level 4 moving to the kid track keeps Level 4 and up, starts kid 1 to 3`() {
        val p = Progress(levelsPassed = setOf("adult-1", "adult-2", "adult-3", "varsity-4"))
        val c = TrackSwitch.preview(levels, p, Track.ADULT, Track.KID)
        assertEquals(3, c.earlyPassedOld)
        assertEquals(0, c.earlyPassedNew)
        assertEquals(3, c.earlyLevelsNew)
        assertEquals(1, c.sharedPassed)
        assertTrue(c.sharedOpen)
        // And switching really doesn't close Level 4: its requirement is either Level 3.
        assertTrue(Gating.levelUnlocked(levels.first { it.id == "varsity-4" }, p))
    }

    @Test fun `a beginner switching loses nothing`() {
        val c = TrackSwitch.preview(levels, Progress(), Track.KID, Track.ADULT)
        assertEquals(0, c.earlyPassedOld)
        assertFalse(c.sharedOpen)
    }
}
