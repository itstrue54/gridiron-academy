package io.github.itstrue54.gridironacademy.coach

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import io.github.itstrue54.gridironacademy.ui.coach.CoachApp
import io.github.itstrue54.gridironacademy.ui.screens.LoadErrorScreen

class MainActivity : ComponentActivity() {
    private val model: CoachViewModel by viewModels()

    /** System "Remove animations", kept live while the app is open (DESIGN 10). */
    private val reducedMotion = mutableStateOf(false)
    private val animationScaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { readReducedMotion() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val load by model.load.collectAsState()
            val dark = isSystemInDarkTheme()
            when (val l = load) {
                Load.Loading -> Unit
                Load.Failed -> LoadErrorScreen(dark, onRetry = model::retry)
                is Load.Ready -> {
                    // Back walks the coach app's own stack; at the root it leaves the app.
                    BackHandler { if (!l.app.back()) finish() }
                    CoachApp(l.app, dark = dark, reducedMotion = reducedMotion.value)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        readReducedMotion()
        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, animationScaleObserver,
        )
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(animationScaleObserver)
        super.onStop()
    }

    private fun readReducedMotion() {
        reducedMotion.value = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
