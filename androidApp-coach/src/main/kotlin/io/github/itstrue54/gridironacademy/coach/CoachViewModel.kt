package io.github.itstrue54.gridironacademy.coach

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.content.ContentValidator
import io.github.itstrue54.gridironacademy.data.DemoCoachRepository
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
 * a retry screen, never a crash (SPEC 3.3). The repository is the demo until the
 * backend lands (SPEC 8.2).
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
                val (content, repo) = withContext(Dispatchers.IO) {
                    val content = ContentLoader.load(ClasspathContentSource())
                    val problems = ContentValidator.validate(content)
                    check(problems.isEmpty()) { "content failed validation: ${problems.take(5)}" }
                    content to DemoCoachRepository(content, today, sent = store.read(), onSent = { all -> viewModelScope.launch(Dispatchers.IO) { store.write(all) } })
                }
                Load.Ready(CoachAppState(content, repo, today, viewModelScope))
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
