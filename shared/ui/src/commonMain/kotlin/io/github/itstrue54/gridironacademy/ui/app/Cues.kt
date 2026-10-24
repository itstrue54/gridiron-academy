package io.github.itstrue54.gridironacademy.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.github.itstrue54.gridironacademy.ui.theme.Skin

/**
 * The feedback cues of DESIGN 10. Each is a haptic, plus a short sound when sound is on
 * (Kid default on, Adult default off). The system's own haptics setting still applies.
 */
enum class Cue(val haptic: HapticFeedbackType) {
    SELECT(HapticFeedbackType.SegmentTick),
    CORRECT(HapticFeedbackType.Confirm),
    RETRY(HapticFeedbackType.Reject),
    PASS(HapticFeedbackType.LongPress),
}

/** Plays a cue. LearnerApp provides one built from the platform and the learner's setting. */
fun interface Cues {
    fun play(cue: Cue)
}

/** Silent by default, so screens rendered in tests and previews make no noise. */
val LocalCues = staticCompositionLocalOf { Cues { } }

/** The Sound setting: the learner's choice if they made one, else on for the Kid look only. */
class SoundSetting(private val platform: LearnerPlatform) {
    private var choice by mutableStateOf(platform.soundSetting)

    fun on(skin: Skin): Boolean = choice ?: (skin == Skin.Kid)

    fun set(on: Boolean) {
        choice = on
        platform.soundSetting = on
    }
}

val LocalSoundSetting = staticCompositionLocalOf { SoundSetting(NoPlatform) }
