package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.data.Session
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.GridironTheme
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import org.jetbrains.compose.resources.stringResource

/**
 * The Coach & Parent app (SPEC 7, DESIGN 14), shared by every platform. Adult skin in
 * Film Room colours that follow the phone, so a coach's diagrams look exactly as they
 * will on a player's phone. The shell supplies the repository, the clock and back.
 */
@Composable
fun CoachApp(state: CoachAppState, dark: Boolean, reducedMotion: Boolean = false) {
    GridironTheme(skin = Skin.Coach, dark = dark, reducedMotion = reducedMotion) {
        val auth = state.auth
        if (auth != null) {
            val session by auth.session.collectAsState()
            when (session) {
                Session.Loading -> { Box(Modifier.fillMaxSize().screenBackground()); return@GridironTheme }
                Session.SignedOut -> {
                    Box(Modifier.fillMaxSize().screenBackground().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = MAX_CONTENT_WIDTH)) { SignInScreen(state) }
                    }
                    return@GridironTheme
                }
                is Session.SignedIn -> LaunchedEffect(session) { state.refresh() }
            }
        }
        val snapshot by state.repository.snapshot.collectAsState()
        val dash = remember(snapshot) { CoachDashboard(snapshot, state.content, state.today) }
        val tabs = CoachTab.entries.filter { it != CoachTab.PLAYBOOK || snapshot.account.isCoach }
        Column(Modifier.fillMaxSize().screenBackground()) {
            Box(Modifier.weight(1f).fillMaxWidth().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxSize()) {
                    when (val s = state.screen) {
                        CoachScreen.Players -> PlayersScreen(state, dash)
                        is CoachScreen.Learner -> LearnerScreen(state, dash, s.learnerId)
                        is CoachScreen.TeamView -> TeamScreen(state, dash, s.teamId)
                        CoachScreen.Assign -> AssignScreen(state, dash)
                        CoachScreen.Playbook -> PlaybookScreen()
                        CoachScreen.More -> MoreScreen(state, dash)
                        CoachScreen.AddChild -> AddChildScreen(state)
                        CoachScreen.CreateTeam -> CreateTeamScreen(state)
                    }
                }
            }
            CoachNavBar(state, tabs, kidsOnly = !snapshot.account.isCoach)
        }
    }
}

@Composable
private fun CoachNavBar(state: CoachAppState, tabs: List<CoachTab>, kidsOnly: Boolean) {
    val c = Gridiron.colors
    NavigationBar(containerColor = c.surfaceRaised, contentColor = c.ink) {
        tabs.forEach { t ->
            val (glyph, label) = when (t) {
                CoachTab.PLAYERS -> Glyph.PEOPLE to if (kidsOnly) Res.string.tab_my_kids else Res.string.tab_players
                CoachTab.ASSIGN -> Glyph.ASSIGN to Res.string.tab_assign
                CoachTab.PLAYBOOK -> Glyph.PLAYBOOK to Res.string.tab_playbook
                CoachTab.MORE -> Glyph.MORE to Res.string.tab_more
            }
            val selected = state.tab == t
            NavigationBarItem(
                selected = selected,
                onClick = { state.select(t) },
                icon = { GlyphIcon(glyph, if (selected) c.onGold else c.inkDim) },
                label = { Text(stringResource(label), style = Gridiron.type.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = c.gold,
                    selectedTextColor = c.ink,
                    unselectedTextColor = c.inkDim,
                ),
            )
        }
    }
}

/** Phones use the full width; a tablet gets a readable column until the rail layout. */
private val MAX_CONTENT_WIDTH = 720.dp
