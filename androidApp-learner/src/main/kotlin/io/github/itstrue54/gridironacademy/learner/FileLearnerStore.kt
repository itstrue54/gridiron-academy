package io.github.itstrue54.gridironacademy.learner

import android.util.AtomicFile
import android.util.Log
import io.github.itstrue54.gridironacademy.data.migration.SaveMigrations
import io.github.itstrue54.gridironacademy.data.migration.SaveTooNew
import io.github.itstrue54.gridironacademy.progress.LearnerSave
import io.github.itstrue54.gridironacademy.ui.app.LearnerStore
import java.io.File
import java.util.concurrent.Executors

/**
 * The save as one JSON file in app-private storage. Writes are atomic (a crash mid-write
 * keeps the previous save) and run on one background thread, in order, so the UI never
 * waits on disk. Every load goes through [SaveMigrations], so an older save is upgraded
 * and a newer app's save is set aside, never misread or overwritten (SPEC 9).
 */
class FileLearnerStore(dir: File, private val newId: () -> String) : LearnerStore {
    private val file = AtomicFile(File(dir, FILE_NAME))
    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "learner-save").apply { isDaemon = true } }

    override fun load(): LearnerSave? = try {
        if (!file.baseFile.exists()) null
        else SaveMigrations.decode(file.readFully().decodeToString(), newId)
    } catch (e: SaveTooNew) {
        // Written by a newer version of the app (a downgrade). Keep it whole for when the
        // newer app returns; start fresh meanwhile.
        Log.w(TAG, e.message ?: "save from a newer app; keeping it aside")
        file.baseFile.renameTo(File(file.baseFile.parentFile, "$FILE_NAME.v${e.version}-${System.currentTimeMillis()}"))
        null
    } catch (e: Exception) {
        // A save we can't read must not crash the app or be silently overwritten.
        Log.e(TAG, "unreadable save; keeping it aside", e)
        file.baseFile.renameTo(File(file.baseFile.parentFile, "$FILE_NAME.unreadable-${System.currentTimeMillis()}"))
        null
    }

    override fun save(save: LearnerSave) {
        // The save is immutable, so encoding it on the writer thread is safe and keeps a
        // long attempt log from stalling the UI.
        writer.execute {
            val text = SaveMigrations.encode(save)
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
