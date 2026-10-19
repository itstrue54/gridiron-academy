package io.github.itstrue54.gridironacademy.data.migration

import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.progress.LearnerSave
import kotlinx.serialization.json.*

/** A save written by a newer app: never read past it, never overwrite it (SPEC 9). */
class SaveTooNew(val version: Int) : Exception("save is version $version; this app reads up to ${LearnerSave.CURRENT_SCHEMA}")

/**
 * Reads a learner save of any version this app knows, migrating step by step to the
 * current one (SPEC 9). Every step has a test that migrates a fixture of the old format.
 */
object SaveMigrations {

    /**
     * [newId] makes an id when the save has none (the device id on first run, the
     * profile's local id). It comes from the platform: ids are never the engine's job.
     */
    fun decode(text: String, newId: () -> String): LearnerSave {
        var o = ContentLoader.json.parseToJsonElement(text).jsonObject
        val version = o["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1
        if (version > LearnerSave.CURRENT_SCHEMA) throw SaveTooNew(version)
        if (version < 2) o = v1to2(o)
        return withIds(ContentLoader.json.decodeFromJsonElement(LearnerSave.serializer(), o), newId)
    }

    fun encode(save: LearnerSave): String = ContentLoader.json.encodeToString(LearnerSave.serializer(), save)

    /** A save (new or migrated) gets its device and learner ids once, then keeps them. */
    fun withIds(save: LearnerSave, newId: () -> String): LearnerSave = save.copy(
        deviceId = save.deviceId.ifBlank { newId() },
        learnerId = save.learnerId.ifBlank { newId() },
    )

    /** v1 -> v2: the attempt log arrives. Old progress is kept as is; the log starts empty. */
    private fun v1to2(o: JsonObject): JsonObject = JsonObject(o + ("schemaVersion" to JsonPrimitive(2)))
}
