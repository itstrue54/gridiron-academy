package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import org.jetbrains.compose.resources.stringResource

/** Sign in or create an account (SPEC 7.2 step 1): email and password for now. */
@Composable
fun SignInScreen(state: CoachAppState) {
    val c = Gridiron.colors
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // After sign-up the next step is signing in, once the emailed link is opened.
    LaunchedEffect(state.checkEmail) { if (state.checkEmail) creating = false }
    val ready = email.contains('@') && password.length >= MIN_PASSWORD && (!creating || name.isNotBlank())
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gridiron.skin.gutter).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(Res.string.app_title_coach), style = Gridiron.type.displayLarge, color = c.ink,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(Res.string.sign_in_pitch), style = Gridiron.type.body, color = c.inkDim)
        if (state.checkEmail) {
            Card(edge = c.correct) { Text(stringResource(Res.string.check_email), style = Gridiron.type.body, color = c.ink) }
        }
        state.notice?.let { Card(edge = c.retry) { Text(it, style = Gridiron.type.body, color = c.ink) } }
        if (creating) Field(name, { name = it.take(60) }, stringResource(Res.string.field_your_name), KeyboardType.Text)
        Field(email, { email = it.trim() }, stringResource(Res.string.field_email), KeyboardType.Email)
        Field(password, { password = it }, stringResource(Res.string.field_password), KeyboardType.Password, secret = true,
            hint = if (creating) stringResource(Res.string.password_rule, MIN_PASSWORD) else null)
        PrimaryButton(
            stringResource(if (creating) Res.string.create_account else Res.string.sign_in),
            onClick = { if (creating) state.signUp(email, password, name) else state.signIn(email, password) },
            enabled = ready && !state.working,
        )
        TextButton(onClick = { creating = !creating; state.clearNotice() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(if (creating) Res.string.have_account else Res.string.need_account), style = Gridiron.type.body, color = c.link)
        }
        Text(stringResource(Res.string.adults_only), style = Gridiron.type.label, color = c.inkDim)
    }
}

/** First steps for a new account: both paths of SPEC 7.2. */
@Composable
fun SetupCards(state: CoachAppState) {
    val c = Gridiron.colors
    Card(onClick = { state.open(CoachScreen.AddChild) }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.add_child), style = Gridiron.type.answer, color = c.ink)
            Text(stringResource(Res.string.add_child_body), style = Gridiron.type.label, color = c.inkDim)
        }
    }
    Card(onClick = { state.open(CoachScreen.CreateTeam) }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.create_team), style = Gridiron.type.answer, color = c.ink)
            Text(stringResource(Res.string.create_team_body), style = Gridiron.type.label, color = c.inkDim)
        }
    }
}

/** Add your child (SPEC 7.2 parent path): a nickname and an age band, nothing more. */
@Composable
fun AddChildScreen(state: CoachAppState) {
    val c = Gridiron.colors
    var nickname by remember { mutableStateOf("") }
    var band by remember { mutableStateOf<AgeBand?>(null) }
    FormScaffold(state, stringResource(Res.string.add_child)) {
        Text(stringResource(Res.string.add_child_privacy), style = Gridiron.type.body, color = c.inkDim)
        Field(nickname, { nickname = it.take(20) }, stringResource(Res.string.field_nickname), KeyboardType.Text)
        SectionTitle(stringResource(Res.string.field_age))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgeBand.entries.forEach { b ->
                FilterChip(
                    selected = band == b, onClick = { band = b },
                    label = { Text(b.label, style = Gridiron.type.body) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.gold, selectedLabelColor = c.onGold, labelColor = c.ink),
                )
            }
        }
        if (band?.child == true) Text(stringResource(Res.string.consent_next), style = Gridiron.type.label, color = c.inkDim)
        PrimaryButton(stringResource(Res.string.save), onClick = { band?.let { state.addChild(nickname, it) } },
            enabled = nickname.isNotBlank() && band != null && !state.working)
    }
}

/** Create a team (SPEC 7.2 coach path). */
@Composable
fun CreateTeamScreen(state: CoachAppState) {
    var name by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }
    var ageGroup by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("") }
    FormScaffold(state, stringResource(Res.string.create_team)) {
        Field(name, { name = it.take(60) }, stringResource(Res.string.field_team_name), KeyboardType.Text)
        Field(variant, { variant = it.take(40) }, stringResource(Res.string.field_variant), KeyboardType.Text)
        Field(ageGroup, { ageGroup = it.take(40) }, stringResource(Res.string.field_age_group), KeyboardType.Text)
        Field(season, { season = it.take(40) }, stringResource(Res.string.field_season), KeyboardType.Text)
        PrimaryButton(stringResource(Res.string.save), onClick = { state.createTeam(name, variant, ageGroup, season) },
            enabled = name.isNotBlank() && !state.working)
    }
}

@Composable
private fun FormScaffold(state: CoachAppState, title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Gridiron.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gridiron.skin.gutter).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(title, style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
        }
        state.notice?.let { Card(edge = c.retry) { Text(it, style = Gridiron.type.body, color = c.ink) } }
        content()
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, type: KeyboardType, secret: Boolean = false, hint: String? = null) {
    val c = Gridiron.colors
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Next),
        supportingText = hint?.let { { Text(it, color = c.inkDim) } },
        textStyle = Gridiron.type.body.copy(color = c.ink),
    )
}

/** Ours; stricter than Supabase's default of 6. */
private const val MIN_PASSWORD = 8
