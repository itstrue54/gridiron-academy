package io.github.itstrue54.gridironacademy

import io.github.itstrue54.gridironacademy.content.*
import io.github.itstrue54.gridironacademy.model.ReviewLog
import io.github.itstrue54.gridironacademy.model.Signoff
import kotlinx.serialization.json.*
import java.security.MessageDigest
import kotlin.test.*

/** Football sign-offs (SPEC 13.3): a signed step must still be the step that was read. */
class ReviewLogTest {
    private val source = ClasspathContentSource()
    private val content = ContentLoader.load(source)

    /** Same as tools/review.py: SHA-256 of the step's canonical JSON, first 16 hex digits. */
    private fun fingerprint(step: JsonElement): String {
        fun canon(e: JsonElement): String = when (e) {
            is JsonObject -> e.entries.sortedBy { it.key }.joinToString(",", "{", "}") { (k, v) -> "${JsonPrimitive(k)}:${canon(v)}" }
            is JsonArray -> e.joinToString(",", "[", "]") { canon(it) }
            is JsonPrimitive -> e.toString()
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(canon(step).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    /** Every shipped step's raw JSON, by id. */
    private val rawSteps: Map<String, JsonElement> by lazy {
        content.curriculum.levels.flatMap { it.unitFiles }.flatMap { f ->
            Json.parseToJsonElement(source.read(f)!!).jsonObject.getValue("lessons").jsonArray
                .flatMap { it.jsonObject.getValue("steps").jsonArray }
        }.associateBy { it.jsonObject.getValue("id").jsonPrimitive.content }
    }

    @Test fun `the fingerprint matches the review tool's`() {
        // `python3 tools/review.py selftest` prints the same value for the same object.
        val sample = Json.parseToJsonElement("""{"b":[1,-3.2,true,null],"a":"Quote \" and \\ and é","c":{"z":0,"y":"line\nbreak"}}""")
        assertEquals("51dd50af7c538f9b", fingerprint(sample))
    }

    @Test fun `no signed step has changed since it was signed`() {
        val stale = content.review.signoffs.filter { s -> rawSteps[s.step]?.let(::fingerprint) != s.hash }.map { it.step }
        assertEquals(emptyList(), stale, "edited after sign-off: review again with tools/review.py, or drop the sign-off")
    }

    @Test fun `the validator checks sign-offs`() {
        val bad = ReviewLog(signoffs = listOf(
            Signoff("no-such-step", "Coach", "2026-10-21", 2026, "0123456789abcdef"),
            Signoff("k1-field-01", "", "21/10/2026", 2026, "xyz"),
            Signoff("k1-field-01", "Coach", "2026-10-21", 2026, "0123456789abcdef"),
        ))
        val errs = ContentValidator.validate(Content(content.curriculum, content.glossary, content.unitsByLevel, content.scenes, content.packs, bad))
        listOf("no such step", "no reviewer", "is not YYYY-MM-DD", "is not 16 hex digits", "signed off twice").forEach { msg ->
            assertTrue(errs.any { msg in it }, "expected '$msg' in $errs")
        }
    }

    @Test fun `a signed step reads as signed`() {
        val id = rawSteps.keys.first()
        val log = ReviewLog(signoffs = listOf(Signoff(id, "Coach", "2026-10-21", 2026, fingerprint(rawSteps.getValue(id)))))
        val signed = Content(content.curriculum, content.glossary, content.unitsByLevel, content.scenes, content.packs, log)
        assertTrue(signed.signedOff(id))
        assertFalse(content.signedOff(id) && content.review.signoffs.none { it.step == id })
    }
}
