package io.github.itstrue54.gridironacademy.learner

import android.app.Application
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
                    val store = FileLearnerStore(getApplication<Application>().filesDir)
                    Load.Ready(LearnerAppState(content, store, today = { LocalDate.now().toEpochDay() }))
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
