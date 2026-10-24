package io.github.itstrue54.gridironacademy.learner

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import io.github.itstrue54.gridironacademy.ui.app.Cue
import io.github.itstrue54.gridironacademy.ui.app.LearnerPlatform

/**
 * Backups through the system file pickers (Storage Access Framework): the parent picks
 * where the file goes and which file comes back. No storage permission, no cloud of our
 * own (SPEC 5.4). Must be created before the activity is started (launchers register then).
 */
class AndroidLearnerPlatform(private val activity: ComponentActivity) : LearnerPlatform {
    private var pendingText: String? = null
    private var onExported: ((Boolean) -> Unit)? = null
    private var onPicked: ((String?) -> Unit)? = null

    /** A system file picker is up; the app leaving the screen for it isn't the child leaving. */
    val pickerOpen: Boolean get() = onExported != null || onPicked != null

    private val createDoc = activity.registerForActivityResult(ActivityResultContracts.CreateDocument(MIME)) { uri ->
        val text = pendingText
        val ok = uri != null && text != null && write(activity, uri, text)
        pendingText = null
        onExported?.invoke(ok)
        onExported = null
    }

    private val openDoc = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val done = onPicked
        onPicked = null
        if (uri == null) { done?.invoke(null); return@registerForActivityResult }
        // The file can be large and on a slow provider: read it off the main thread.
        Thread({
            val text = read(activity, uri)
            activity.runOnUiThread { done?.invoke(text) }
        }, "backup-read").start()
    }

    override fun exportBackup(text: String, fileName: String, done: (Boolean) -> Unit) {
        pendingText = text
        onExported = done
        createDoc.launch(fileName)
    }

    override fun pickBackup(done: (String?) -> Unit) {
        onPicked = done
        openDoc.launch(arrayOf(MIME, "text/plain", "application/octet-stream"))
    }

    // Cue sounds (DESIGN 10): loaded once, played as sonification so they follow the
    // system volume. A sound still loading is skipped rather than played late.
    private val sounds: SoundPool by lazy {
        SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).build()
    }
    private val soundIds: Map<Cue, Int> by lazy {
        mapOf(
            Cue.SELECT to R.raw.cue_select, Cue.CORRECT to R.raw.cue_correct,
            Cue.RETRY to R.raw.cue_retry, Cue.PASS to R.raw.cue_pass,
        ).mapValues { (_, res) -> sounds.load(activity, res, 1) }
    }

    override fun playSound(cue: Cue) {
        val id = soundIds[cue] ?: return
        sounds.play(id, 1f, 1f, 1, 0, 1f)
    }

    /** Loads the sounds ahead of the first answer. */
    fun preloadSounds() { soundIds }

    fun release() = sounds.release()

    private val prefs by lazy { activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override var soundSetting: Boolean?
        get() = if (prefs.contains(KEY_SOUND)) prefs.getBoolean(KEY_SOUND, false) else null
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY_SOUND) else putBoolean(KEY_SOUND, value) }.apply()
        }

    override val appVersion: String =
        runCatching { activity.packageManager.getPackageInfo(activity.packageName, 0).versionName }.getOrNull() ?: "?"

    private fun write(context: Context, uri: Uri, text: String): Boolean = try {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.encodeToByteArray()) } != null
    } catch (e: Exception) {
        Log.e(TAG, "backup write failed", e)
        false
    }

    private fun read(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            // A save is small; a huge file isn't one of ours.
            val bytes = input.readNBytesCompat(MAX_BYTES + 1)
            if (bytes.size > MAX_BYTES) null else bytes.decodeToString()
        }
    } catch (e: Exception) {
        Log.e(TAG, "backup read failed", e)
        null
    }

    private fun java.io.InputStream.readNBytesCompat(n: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (total < n) {
            val r = read(buf, 0, minOf(buf.size, n - total))
            if (r < 0) break
            out.write(buf, 0, r); total += r
        }
        return out.toByteArray()
    }

    private companion object {
        const val TAG = "LearnerBackup"
        const val MIME = "application/json"
        /** 8 MB: the attempt log is capped near 5 MB, so anything larger isn't one of ours. */
        const val MAX_BYTES = 8 * 1024 * 1024
        const val PREFS = "settings"
        const val KEY_SOUND = "sound"
    }
}
