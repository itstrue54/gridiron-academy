package io.github.itstrue54.gridironacademy.learner

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.content.ContentValidator
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

sealed interface Load {
    data object Loading : Load
    data class Ready(val app: LearnerAppState) : Load
    data object Failed : Load
}

/**
 * Holds the app state across rotations. Content and the save are read off the main
 * thread; a content failure becomes [Load.Failed] with a retry, never a crash (SPEC 3.3).
 */
class LearnerViewModel(app: Application) : AndroidViewModel(app) {
    private val _load = MutableStateFlow<Load>(Load.Loading)
    val load: StateFlow<Load> = _load

    init { start() }

    fun retry() { if (_load.value == Load.Failed) start() }

    private fun start() {
        _load.value = Load.Loading
        viewModelScope.launch {
            _load.value = try {
                withContext(Dispatchers.IO) {
                    // The content JSON ships inside :shared:core as Java resources.
                    val content = ContentLoader.load(ClasspathContentSource())
                    val problems = ContentValidator.validate(content)
                    check(problems.isEmpty()) { "content failed validation: ${problems.take(5)}" }
                    // Ids are the platform's job (SPEC 8.1): a random UUID per install and per profile.
                    val newId = { UUID.randomUUID().toString() }
                    val store = FileLearnerStore(getApplication<Application>().filesDir, newId)
                    // Debug builds mark steps no coach has signed off yet (SPEC 13.3).
                    val debuggable = getApplication<Application>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                    Load.Ready(LearnerAppState(content, store, nowMillis = System::currentTimeMillis, newId = newId, showReviewBadges = debuggable) { LocalDate.now().toEpochDay() })
                }
            } catch (e: Exception) {
                Log.e(TAG, "lessons failed to load", e)
                Load.Failed
            }
        }
    }

    private companion object {
        const val TAG = "LearnerViewModel"
    }
}
