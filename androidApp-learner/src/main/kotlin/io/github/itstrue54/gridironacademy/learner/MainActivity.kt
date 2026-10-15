package io.github.itstrue54.gridironacademy.learner

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
import io.github.itstrue54.gridironacademy.ui.app.LearnerApp
import io.github.itstrue54.gridironacademy.ui.screens.LoadErrorScreen

class MainActivity : ComponentActivity() {
    private val model: LearnerViewModel by viewModels()

    /** System "Remove animations" (animator scale 0), kept live while the app is open (DESIGN 10). */
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
                Load.Loading -> Unit // the window background is the theme surface
                Load.Failed -> LoadErrorScreen(dark, onRetry = model::retry)
                is Load.Ready -> {
                    BackHandler(enabled = l.app.backStack.size > 1) { l.app.back() }
                    LearnerApp(l.app, dark = dark, reducedMotion = reducedMotion.value)
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
