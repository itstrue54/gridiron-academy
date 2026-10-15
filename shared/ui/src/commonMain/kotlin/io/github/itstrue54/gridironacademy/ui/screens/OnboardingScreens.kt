package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.model.Diagram
import io.github.itstrue54.gridironacademy.model.DiagramPlayer
import io.github.itstrue54.gridironacademy.model.DiagramRoute
import io.github.itstrue54.gridironacademy.model.PathKind
import io.github.itstrue54.gridironacademy.model.Pt
import io.github.itstrue54.gridironacademy.model.Side
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.field.FieldDiagram
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.age_band
import io.github.itstrue54.gridironacademy.ui.resources.age_band_adult
import io.github.itstrue54.gridironacademy.ui.resources.age_title
import io.github.itstrue54.gridironacademy.ui.resources.app_name
import io.github.itstrue54.gridironacademy.ui.resources.nickname_hint
import io.github.itstrue54.gridironacademy.ui.resources.nickname_label
import io.github.itstrue54.gridironacademy.ui.resources.start
import io.github.itstrue54.gridironacademy.ui.resources.welcome_body
import io.github.itstrue54.gridironacademy.ui.resources.welcome_start
import io.github.itstrue54.gridironacademy.ui.resources.welcome_title
import io.github.itstrue54.gridironacademy.ui.resources.who_title
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.stringResource

/** A touchdown pass, drawn by the app's own renderer: the welcome picture. */
private val WelcomePlay = Diagram(
    players = listOf(
        DiagramPlayer("qb", "QB", Side.OFFENSE, 0f, -5f),
        DiagramPlayer("c", "C", Side.OFFENSE, 0f, 0f),
        DiagramPlayer("x", "X", Side.OFFENSE, -14f, 0f),
        DiagramPlayer("z", "Z", Side.OFFENSE, 14f, -1f),
        DiagramPlayer("cb", "CB", Side.DEFENSE, 13f, 6f),
        DiagramPlayer("fs", "FS", Side.DEFENSE, -2f, 13f),
    ),
    routes = listOf(
        DiagramRoute("z", listOf(Pt(14f, 6f), Pt(6f, 15f)), PathKind.PASS),
        DiagramRoute("x", listOf(Pt(-14f, 12f)), PathKind.PASS),
    ),
)

/** First launch (DESIGN 9.8): what this is, in one card, and one button. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    Column(
        Modifier.fillMaxSize().background(c.surface).safeDrawingPadding().padding(skin.gutter),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.app_name), style = Gridiron.type.label, color = c.inkDim)
            Text(stringResource(Res.string.welcome_title), style = Gridiron.type.displayLarge, color = c.ink, modifier = Modifier.semantics { heading() })
            FieldDiagram(WelcomePlay, description = "", modifier = Modifier.fillMaxWidth())
            Text(stringResource(Res.string.welcome_body), style = Gridiron.type.body, color = c.ink)
        }
        PrimaryButton(stringResource(Res.string.welcome_start), onStart)
    }
}

/**
 * Who's playing (DESIGN 9.8, SPEC 5.2): a nickname and the neutral age screen. Six equal
 * buttons, no default, nothing saying what a band unlocks.
 */
@Composable
fun OnboardingScreen(onDone: (nickname: String, band: AgeBand) -> Unit) {
    val c = Gridiron.colors
    val skin = Gridiron.skin
    var nickname by rememberSaveable { mutableStateOf("") }
    var band by rememberSaveable { mutableStateOf<AgeBand?>(null) }
    Column(
        Modifier.fillMaxSize().background(c.surface).safeDrawingPadding().padding(skin.gutter),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.who_title), style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(MAX_NICKNAME) },
                label = { Text(stringResource(Res.string.nickname_label)) },
                supportingText = { Text(stringResource(Res.string.nickname_hint)) },
                singleLine = true,
                textStyle = Gridiron.type.body,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.gold, unfocusedBorderColor = c.line,
                    focusedLabelColor = c.ink, unfocusedLabelColor = c.inkDim, cursorColor = c.ink,
                    focusedTextColor = c.ink, unfocusedTextColor = c.ink,
                    focusedSupportingTextColor = c.inkDim, unfocusedSupportingTextColor = c.inkDim,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(Res.string.age_title), style = Gridiron.type.title, color = c.ink, modifier = Modifier.semantics { heading() })
            AgeBand.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { b ->
                        val label = if (b == AgeBand.AGE_18_PLUS) stringResource(Res.string.age_band_adult) else stringResource(Res.string.age_band, b.label)
                        AgeButton(label, selected = band == b, onClick = { band = b }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        PrimaryButton(stringResource(Res.string.start), onClick = { band?.let { onDone(nickname, it) } }, enabled = band != null)
    }
}

@Composable
private fun AgeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = Gridiron.colors
    Surface(
        onClick = onClick,
        shape = Gridiron.skin.buttonShape,
        color = c.surfaceRaised,
        contentColor = c.ink,
        border = BorderStroke(if (selected) 3.dp else 2.dp, if (selected) c.gold else c.line),
        modifier = modifier.heightIn(min = 64.dp).semantics { role = Role.RadioButton; this.selected = selected },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
            Text(label, style = Gridiron.type.answer, textAlign = TextAlign.Center)
        }
    }
}

private const val MAX_NICKNAME = 20
