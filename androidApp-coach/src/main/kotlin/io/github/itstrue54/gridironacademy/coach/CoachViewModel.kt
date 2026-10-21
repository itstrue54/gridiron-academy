package io.github.itstrue54.gridironacademy.coach

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.content.ContentValidator
import io.github.itstrue54.gridironacademy.content.reviewedOnly
import io.github.itstrue54.gridironacademy.data.DemoCoachRepository
import io.github.itstrue54.gridironacademy.data.supabase.SupabaseConfig
import io.github.itstrue54.gridironacademy.data.supabase.supabaseCoach
import io.github.itstrue54.gridironacademy.ui.coach.CoachAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface Load {
    data object Loading : Load
    data class Ready(val app: CoachAppState) : Load
    data object Failed : Load
}

/**
 * Holds the coach app across rotations. Content loads off the main thread; a failure is
 * a retry screen, never a crash (SPEC 3.3). With a Supabase project configured the app
 * signs in to it; without one it runs on the demo account (ADR-009).
 */
class CoachViewModel(app: Application) : AndroidViewModel(app) {
    private val _load = MutableStateFlow<Load>(Load.Loading)
    val load: StateFlow<Load> = _load

    /** What the coach sent on the demo account, kept across launches. */
    private val store = SentAssignmentsStore(app.filesDir)

    init { start() }

    fun retry() { if (_load.value == Load.Failed) start() }

    private fun start() {
        _load.value = Load.Loading
        viewModelScope.launch {
            _load.value = try {
                val today = LocalDate.now().toEpochDay()
                val content = withContext(Dispatchers.IO) {
                    val content = ContentLoader.load(ClasspathContentSource())
                    val problems = ContentValidator.validate(content)
                    check(problems.isEmpty()) { "content failed validation: ${problems.take(5)}" }
                    // Same course as the learner app, so nothing unreviewed can be assigned.
                    if (BuildConfig.REVIEWED_ONLY) content.reviewedOnly() else content
                }
                val config = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
                if (config.url.isNotBlank() && config.anonKey.isNotBlank()) {
                    // A real account (ADR-004): sign-in, then the dashboards over synced data.
                    val repo = supabaseCoach(config, content, { LocalDate.now().toEpochDay() }, viewModelScope)
                    Load.Ready(CoachAppState(content, repo, today, viewModelScope, auth = repo, setup = repo))
                } else {
                    val sent = withContext(Dispatchers.IO) { store.read() }
                    val repo = DemoCoachRepository(content, today, sent = sent, onSent = { all ->
                        viewModelScope.launch(Dispatchers.IO) { store.write(all) }
                    })
                    Load.Ready(CoachAppState(content, repo, today, viewModelScope))
                }
            } catch (e: Exception) {
                Log.e(TAG, "coach app failed to load", e)
                Load.Failed
            }
        }
    }

    private companion object {
        const val TAG = "CoachViewModel"
    }
}
