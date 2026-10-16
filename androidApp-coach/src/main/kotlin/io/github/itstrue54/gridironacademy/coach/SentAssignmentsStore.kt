package io.github.itstrue54.gridironacademy.coach

import android.util.Log
import io.github.itstrue54.gridironacademy.progress.Assignment
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The demo account's sent assignments, one small JSON file in app-private storage, so an
 * assignment doesn't vanish when the app closes. The backend replaces this (ADR-009).
 */
class SentAssignmentsStore(dir: File) {
    private val file = File(dir, "demo-sent-assignments.json")
    private val serializer = ListSerializer(Assignment.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun read(): List<Assignment> = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (e: Exception) {
        // A bad file costs the demo's sent list, never the app.
        Log.w(TAG, "ignoring unreadable ${file.name}", e)
        emptyList()
    }

    /** Write-then-rename so a crash mid-write never leaves half a file. */
    fun write(all: List<Assignment>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(serializer, all))
        tmp.renameTo(file)
    }

    private companion object {
        const val TAG = "SentAssignmentsStore"
    }
}
