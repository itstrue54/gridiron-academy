package io.github.itstrue54.gridironacademy.learner

import android.util.AtomicFile
import android.util.Log
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.progress.LearnerSave
import io.github.itstrue54.gridironacademy.ui.app.LearnerStore
import java.io.File
import java.util.concurrent.Executors

/**
 * The save as one JSON file in app-private storage. Writes are atomic (a crash mid-write
 * keeps the previous save) and run on one background thread, in order, so the UI never
 * waits on disk. Format changes bump [LearnerSave.schemaVersion] with a migration (SPEC 9).
 */
class FileLearnerStore(dir: File) : LearnerStore {
    private val file = AtomicFile(File(dir, FILE_NAME))
    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "learner-save").apply { isDaemon = true } }

    override fun load(): LearnerSave? = try {
        if (!file.baseFile.exists()) null
        else ContentLoader.json.decodeFromString(LearnerSave.serializer(), file.readFully().decodeToString())
    } catch (e: Exception) {
        // A save we can't read must not crash the app or be silently overwritten.
        Log.e(TAG, "unreadable save; keeping it aside", e)
        file.baseFile.renameTo(File(file.baseFile.parentFile, "$FILE_NAME.unreadable-${System.currentTimeMillis()}"))
        null
    }

    override fun save(save: LearnerSave) {
        val text = ContentLoader.json.encodeToString(LearnerSave.serializer(), save)
        writer.execute {
            val out = file.startWrite()
            try {
                out.write(text.encodeToByteArray())
                file.finishWrite(out)
            } catch (e: Exception) {
                file.failWrite(out)
                Log.e(TAG, "save failed", e)
            }
        }
    }

    private companion object {
        const val FILE_NAME = "learner-save.json"
        const val TAG = "LearnerStore"
    }
}
