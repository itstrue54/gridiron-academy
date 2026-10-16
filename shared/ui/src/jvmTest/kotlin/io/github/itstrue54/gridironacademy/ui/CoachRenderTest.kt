package io.github.itstrue54.gridironacademy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import io.github.itstrue54.gridironacademy.content.ClasspathContentSource
import io.github.itstrue54.gridironacademy.content.ContentLoader
import io.github.itstrue54.gridironacademy.data.DemoCoachRepository
import io.github.itstrue54.gridironacademy.ui.coach.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.*

/** Every Coach app screen rendered at phone size into build/coach-renders/ for review. */
class CoachRenderTest {
    private val content = ContentLoader.load(ClasspathContentSource())
    private val out = File("build/coach-renders").apply { mkdirs() }
    private val today = 20_000L

    private fun state() = CoachAppState(content, DemoCoachRepository(content, today), today, CoroutineScope(Dispatchers.Unconfined))

    @OptIn(ExperimentalComposeUiApi::class)
    private fun shot(name: String, height: Int = 1560, ui: @Composable () -> Unit) {
        val bytes = ImageComposeScene(width = 720, height = height, density = Density(2f)) { ui() }.use { scene ->
            scene.render(0)
            scene.render(1_000_000_000L)
            scene.render(2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
        }
        File(out, "$name.png").writeBytes(bytes)
        assertTrue(bytes.size > 10_000, "$name looks empty")
    }

    @Test fun playersLearnerTeam() {
        val s = state()
        shot("c01-players-light") { CoachApp(s, dark = false) }
        shot("c02-players-dark") { CoachApp(s, dark = true) }
        s.open(CoachScreen.Learner("jordan"))
        shot("c03-learner-jordan", height = 3200) { CoachApp(s, dark = false) }
        s.back()
        s.open(CoachScreen.TeamView("gulls"))
        shot("c04-team-gulls", height = 3600) { CoachApp(s, dark = true) }
    }

    @Test fun assignFlow() {
        val s = state()
        s.select(CoachTab.ASSIGN)
        shot("c05-assign-what") { CoachApp(s, dark = false) }
        s.edit { it.copy(packIds = listOf("def-coverage-0-4", "def-disguise-rotations")) }
        s.next()
        shot("c06-assign-who") { CoachApp(s, dark = false) }
        s.edit { it.copy(learnerIds = listOf("leo", "jordan", "ace", "tank")) }
        s.next()
        s.edit { it.copy(note = "Call me at 555 123 4567") }
        shot("c07-assign-options-bad-note") { CoachApp(s, dark = true) }
        assertFalse(s.draft.canContinue)
        s.edit { it.copy(note = "Know these before Thursday.") }
        s.next()
        shot("c08-assign-preview") { CoachApp(s, dark = false) }
        s.send(io.github.itstrue54.gridironacademy.data.CoachDashboard(s.repository.snapshot.value, content, today))
        assertEquals(CoachTab.PLAYERS, s.tab)
        shot("c09-players-after-send") { CoachApp(s, dark = false) }
    }

    @Test fun playbookAndMore() {
        val s = state()
        s.select(CoachTab.PLAYBOOK)
        shot("c10-playbook") { CoachApp(s, dark = true) }
        s.select(CoachTab.MORE)
        shot("c11-more") { CoachApp(s, dark = false) }
    }
}
