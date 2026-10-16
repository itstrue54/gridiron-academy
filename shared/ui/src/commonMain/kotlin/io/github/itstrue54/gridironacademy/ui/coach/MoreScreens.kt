package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.stringResource

/** The play editor is M4 (SPEC 14); until then the tab says so plainly. */
@Composable
fun PlaybookScreen() {
    val c = Gridiron.colors
    Column(
        Modifier.fillMaxSize().padding(Gridiron.skin.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        GlyphIcon(Glyph.PLAYBOOK, c.inkDim, size = 56.dp)
        Text(stringResource(Res.string.playbook_title), style = Gridiron.type.headline, color = c.ink,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.playbook_body), style = Gridiron.type.body, color = c.inkDim, textAlign = TextAlign.Center)
    }
}

/** Account, roles and what this build is (DESIGN 14.1 More). */
@Composable
fun MoreScreen(state: CoachAppState, dash: CoachDashboard) {
    val c = Gridiron.colors
    val a = dash.snapshot.account
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gridiron.skin.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.tab_more), style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
        SectionTitle(stringResource(Res.string.section_account))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Monogram(a.displayName.removePrefix("Coach ").ifBlank { a.displayName })
                Column {
                    Text(a.displayName, style = Gridiron.type.title, color = c.ink)
                    val roles = listOfNotNull(
                        if (a.isParent) stringResource(Res.string.role_parent) else null,
                        if (a.isCoach) stringResource(Res.string.role_coach) else null,
                    )
                    Text(roles.joinToString(" · "), style = Gridiron.type.label, color = c.inkDim)
                }
            }
        }
        if (dash.snapshot.demo) {
            SectionTitle(stringResource(Res.string.section_about))
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.demo_title), style = Gridiron.type.answer, color = c.ink)
                    Text(stringResource(Res.string.demo_more), style = Gridiron.type.body, color = c.inkDim)
                }
            }
        }
        if (state.auth != null) {
            io.github.itstrue54.gridironacademy.ui.components.SecondaryButton(stringResource(Res.string.sign_out), onClick = { state.signOut() })
        }
        SectionTitle(stringResource(Res.string.section_coming))
        Card {
            Text(stringResource(Res.string.coming_list), style = Gridiron.type.body, color = c.inkDim)
        }
    }
}
