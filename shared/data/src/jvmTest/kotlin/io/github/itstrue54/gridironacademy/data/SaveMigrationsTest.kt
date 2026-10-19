package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.data.migration.SaveMigrations
import io.github.itstrue54.gridironacademy.data.migration.SaveTooNew
import io.github.itstrue54.gridironacademy.progress.*
import kotlin.test.*

class SaveMigrationsTest {
    private fun fixture(v: Int) = checkNotNull(javaClass.getResource("/saves/v$v.json")).readText()
    private var n = 0
    private val ids = { "id-${n++}" }

    @Test fun `a version 1 save keeps all its progress and gains an empty log`() {
        val s = SaveMigrations.decode(fixture(1), ids)
        assertEquals(LearnerSave.CURRENT_SCHEMA, s.schemaVersion)
        assertEquals(Profile("Leo", AgeBand.AGE_8_9), s.profile)
        assertEquals(185, s.progress.xp)
        assertEquals(4, s.progress.streakDays)
        assertEquals(setOf("kid-1"), s.progress.levelsPassed)
        assertEquals(LessonResult(75, 2), s.progress.lessons["k1-game"])
        assertEquals(20003, s.progress.review.getValue("k1-game-04").dueDay)
        assertEquals(emptyList(), s.attempts)
        assertEquals(0, s.nextSeq)
        assertEquals("id-0", s.deviceId)
        assertEquals("id-1", s.learnerId)
    }

    @Test fun `a real version 1 save, which never wrote its version, migrates`() {
        val text = checkNotNull(javaClass.getResource("/saves/v1-as-written.json")).readText()
        assertFalse("schemaVersion" in text, "v1 left the version out")
        val s = SaveMigrations.decode(text, ids)
        assertEquals(LearnerSave.CURRENT_SCHEMA, s.schemaVersion)
        assertEquals("Sam", s.profile?.nickname)
        assertTrue(s.progress.lessons.size > 20 && "adult-1" in s.progress.levelsPassed)
    }

    @Test fun `every save from now on states its version`() {
        val text = SaveMigrations.encode(SaveMigrations.withIds(LearnerSave(), ids))
        assertTrue("\"schemaVersion\":${LearnerSave.CURRENT_SCHEMA}" in text, text)
    }

    @Test fun `ids are made once and then kept`() {
        val first = SaveMigrations.decode(fixture(1), ids)
        val again = SaveMigrations.decode(SaveMigrations.encode(first), ids)
        assertEquals(first, again)
    }

    @Test fun `a save from a newer app is refused, not misread`() {
        val text = fixture(1).replace("\"schemaVersion\":1", "\"schemaVersion\":99")
        assertEquals(99, assertFailsWith<SaveTooNew> { SaveMigrations.decode(text, ids) }.version)
    }

    @Test fun `the attempt log round-trips`() {
        val a = AttemptEvent("l", "d", 0, "k1-field-03", AttemptContext.LESSON, 1f, answer = "100", msSpent = 4200, epochDay = 20004, at = 1)
        val s = SaveMigrations.withIds(LearnerSave(attempts = listOf(a), nextSeq = 1), ids)
        assertEquals(s, SaveMigrations.decode(SaveMigrations.encode(s), ids))
    }
}
