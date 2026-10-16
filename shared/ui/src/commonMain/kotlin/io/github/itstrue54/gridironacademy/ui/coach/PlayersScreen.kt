package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.data.LinkedLearner
import io.github.itstrue54.gridironacademy.data.SendResult
import io.github.itstrue54.gridironacademy.data.Team
import io.github.itstrue54.gridironacademy.progress.AssignmentStatus
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.SecondaryButton
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Players (DESIGN 14.2): my kids, then teams. A team row opens the team view. */
@Composable
fun PlayersScreen(state: CoachAppState, dash: CoachDashboard) {
    val c = Gridiron.colors
    val snap = dash.snapshot
    val kids = snap.learners.filter { it.myChild }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Gridiron.skin.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                stringResource(Res.string.players_hello, snap.account.displayName),
                style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() },
            )
        }
        if (snap.demo) item { DemoNotice() }
        state.sent?.let { result ->
            item { SentNotice(result, onDismiss = state::clearSent) }
        }
        if (kids.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.section_my_kids)) }
            items(kids, key = { "kid-" + it.id }) { l -> LearnerRow(l, dash, state.today) { state.open(CoachScreen.Learner(l.id)) } }
        }
        if (snap.teams.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.section_teams)) }
            items(snap.teams, key = { "team-" + it.id }) { t -> TeamRow(t, dash) { state.open(CoachScreen.TeamView(t.id)) } }
        }
    }
}

@Composable
fun LearnerRow(l: LinkedLearner, dash: CoachDashboard, today: Long, onClick: () -> Unit) {
    val c = Gridiron.colors
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Monogram(l.nickname)
            Column(Modifier.weight(1f)) {
                Text(l.nickname, style = Gridiron.type.title, color = c.ink)
                Text(learnerSubline(l, today), style = Gridiron.type.label, color = c.inkDim)
            }
            dash.overallStatus(l.id)?.let { StatusPill(it) }
        }
    }
}

@Composable
private fun TeamRow(t: Team, dash: CoachDashboard, onClick: () -> Unit) {
    val c = Gridiron.colors
    val members = dash.snapshot.membersOf(t.id)
    val behind = members.count { dash.overallStatus(it.id) == AssignmentStatus.BEHIND }
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Monogram(teamMonogram(t.name), square = true, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(t.name, style = Gridiron.type.title, color = c.ink)
                Text(
                    stringResource(Res.string.team_subline, t.variant, t.ageGroup, pluralStringResource(Res.plurals.players_count, members.size, members.size)),
                    style = Gridiron.type.label, color = c.inkDim,
                )
                dash.teamProgress(t.id).forEach { (a, done) ->
                    Text(stringResource(Res.string.team_assignment_done, dash.packsOf(a).joinToString(" + ") { it.title }, done, a.targets.size),
                        style = Gridiron.type.label, color = c.ink)
                }
                if (behind > 0) Text(pluralStringResource(Res.plurals.team_behind, behind, behind), style = Gridiron.type.label, color = c.retry)
            }
        }
    }
}

/** The generated two-letter TeamMark (DESIGN 8): initials of the first two words. */
fun teamMonogram(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }

@Composable
private fun DemoNotice() {
    val c = Gridiron.colors
    Card(edge = c.link) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.demo_title), style = Gridiron.type.answer, color = c.ink)
            Text(stringResource(Res.string.demo_body), style = Gridiron.type.label, color = c.inkDim)
        }
    }
}

@Composable
private fun SentNotice(result: SendResult, onDismiss: () -> Unit) {
    val c = Gridiron.colors
    val ok = result is SendResult.Sent
    Card(edge = if (ok) c.correct else c.retry) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                when (result) {
                    is SendResult.Sent -> pluralStringResource(Res.plurals.sent_to, result.assignment.targets.size, result.assignment.targets.size)
                    is SendResult.Rejected -> stringResource(Res.string.send_failed, result.reason)
                },
                style = Gridiron.type.answer, color = c.ink,
            )
            SecondaryButton(stringResource(Res.string.ok), onDismiss)
        }
    }
}
